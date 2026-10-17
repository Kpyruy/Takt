package com.kpyruy.takt.core.data

import android.content.Context
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ReminderCoordinatorStatus(val plannedCount: Int = 0, val exact: Boolean = false, val lastError: String? = null)
enum class ReminderDeliveryResult { DELIVERED, DISABLED, NO_PERMISSION, STALE, TOO_EARLY, ALREADY_DELIVERED, FAILED }

/** All Room reads, planning and durable alarm JSON run off the main thread. No minute polling. */
class ReminderCoordinator(
    context: Context,
    private val snapshots: StateFlow<PlanningSnapshot?>,
    private val readSnapshot: suspend () -> PlanningSnapshot,
    private val backend: ReminderAlarmBackend = AndroidReminderAlarmBackend(context),
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val store: ReminderAlarmStore = ReminderAlarmStore(context.applicationContext),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var delivery: ReminderNotificationDelivery? = null
    private var observer: Job? = null
    private var disabledCleaned = false
    private var maintenanceAt: Instant? = null
    private var channelLanguage: com.kpyruy.takt.core.model.AppLanguage? = null
    private var lastExactPermission: Boolean? = null
    private val mutableStatus = MutableStateFlow(ReminderCoordinatorStatus())
    val status: StateFlow<ReminderCoordinatorStatus> = mutableStatus.asStateFlow()

    @OptIn(FlowPreview::class)
    fun start(delivery: ReminderNotificationDelivery) {
        if (observer != null) return
        this.delivery = delivery
        observer = scope.launch {
            var firstEmission = true
            snapshots.filterNotNull().debounce { if (it.settings.reminders.enabled) 350L else 0L }.collect {
                try {
                    mutex.withLock {
                        // Once OFF has cancelled the OS state, unrelated study/theme updates
                        // need no fresh Room reads. Explicit lifecycle/permission refreshes
                        // still use refreshNow, including the first process reconciliation.
                        if (it.settings.reminders.enabled || !disabledCleaned) {
                            reconcile(readSnapshot(), forceRearm = firstEmission)
                        }
                    }
                    firstEmission = false
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    mutableStatus.value = mutableStatus.value.copy(lastError = error.javaClass.simpleName)
                }
            }
        }
    }

    fun requestRefresh() { scope.launch { runCatching { refreshNow(forceRearm = true) }
        .onFailure { mutableStatus.value = mutableStatus.value.copy(lastError = it.javaClass.simpleName) } } }

    suspend fun readPlannedAlarms(): List<PlannedReminder> = withContext(Dispatchers.IO) { mutex.withLock { store.read() } }

    suspend fun refreshNow(forceRearm: Boolean = true): List<PlannedReminder> = withContext(Dispatchers.IO) {
        mutex.withLock { reconcile(readSnapshot(), forceRearm) }
    }

    private fun reconcile(snapshot: PlanningSnapshot, forceRearm: Boolean): List<PlannedReminder> {
        val now = clock.instant()
        val previous = store.read().associateBy { it.id }
        val permissions = backend.permissions()
        val rearm = forceRearm || lastExactPermission != permissions.exactAlarmsAllowed
        lastExactPermission = permissions.exactAlarmsAllowed
        val sink = delivery
        if (snapshot.settings.reminders.enabled && channelLanguage != snapshot.settings.language && sink != null) {
            sink.bootstrapChannels(snapshot.settings.language)
            channelLanguage = snapshot.settings.language
        }
        if (!snapshot.settings.reminders.enabled || !permissions.notificationsAllowed ||
            com.kpyruy.takt.core.model.ReminderCategory.entries.none(snapshot.settings.reminders.normalized()::allows)) {
            previous.keys.forEach(backend::cancel)
            // First reconciliation cleans up OS state even if the local ledger was already empty.
            if (!disabledCleaned || previous.isNotEmpty()) {
                backend.cancelMaintenance()
                sink?.cancelAllReminders()
            }
            maintenanceAt = null
            disabledCleaned = true
            if (previous.isNotEmpty()) store.write(emptyList())
            store.prune(now)
            mutableStatus.value = ReminderCoordinatorStatus(exact = permissions.exactAlarmsAllowed)
            return emptyList()
        }
        disabledCleaned = false
        val delivered = store.delivered(now)
        val legacyDeliveredIds = delivered.mapNotNull { key ->
            key.substringBefore('@').takeIf { key.substringAfter('@').toLongOrNull() != null }
        }.toSet()
        val planned = ReminderPlanner.plan(snapshot, now.minus(Duration.ofDays(1)), zone(), ReminderPlanner.HORIZON_DAYS + 1)
            .filter { it.occursAt.isAfter(now) && !wasDelivered(it, delivered, legacyDeliveredIds) &&
                (it.fireAt.isAfter(now) || previous[it.id]?.let { old -> sameAlarm(old, it) } == true) }
            .take(MAX_PENDING_ALARMS)
        val next = planned.associateBy { it.id }
        previous.values.filter { old -> next[old.id]?.let { sameAlarm(old, it) } != true }.forEach {
            backend.cancel(it.id)
            // Consuming an alarm must not immediately dismiss the notification just posted for it.
            if (!wasDelivered(it, delivered, legacyDeliveredIds)) sink?.cancel(it.id)
        }
        // Commit source targets and alarm timestamps before arming; unchanged JSON avoids a write.
        store.write(planned)
        var exact = permissions.exactAlarmsAllowed
        planned.forEach { reminder ->
            if (rearm || previous[reminder.id]?.let { sameAlarm(it, reminder) } != true) {
                exact = backend.schedule(reminder,
                    if (reminder.fireAt.isAfter(now)) reminder.fireAt else now.plusSeconds(1), exact) && exact
            }
        }
        val nextMaintenance = now.atZone(zone()).toLocalDate().plusDays(1).atTime(3, 15).atZone(zone()).toInstant()
        if (rearm || maintenanceAt != nextMaintenance) {
            backend.scheduleMaintenance(nextMaintenance)
            maintenanceAt = nextMaintenance
        }
        store.prune(now)
        mutableStatus.value = ReminderCoordinatorStatus(planned.size, exact)
        return planned
    }

    private fun sameAlarm(a: PlannedReminder, b: PlannedReminder): Boolean =
        a.deliveryKey == b.deliveryKey && a.fireAt == b.fireAt && a.occursAt == b.occursAt

    private fun wasDelivered(reminder: PlannedReminder, delivered: Set<String>, legacyIds: Set<String>? = null): Boolean =
        reminder.deliveryKey in delivered || if (legacyIds != null) reminder.id in legacyIds else delivered.any { key ->
            // Old ledgers contain only id@epoch. Preserve their at-most-once claims until expiry;
            // the old format cannot distinguish a timezone change from an edited local source.
            key.startsWith("${reminder.id}@") && key.substringAfter('@').toLongOrNull() != null
        }

    /** Same validation path as AlarmManager. Tests may call it without exporting any debug receiver. */
    suspend fun deliver(id: String, expectedFireAtEpochMillis: Long): ReminderDeliveryResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = clock.instant()
            val snapshot = readSnapshot()
            val settings = snapshot.settings
            if (!settings.reminders.enabled) { reconcile(snapshot, false); return@withLock ReminderDeliveryResult.DISABLED }
            if (!backend.permissions().notificationsAllowed) { reconcile(snapshot, false); return@withLock ReminderDeliveryResult.NO_PERMISSION }
            val pending = store.read().firstOrNull { it.id == id && it.fireAt.toEpochMilli() == expectedFireAtEpochMillis }
                ?: return@withLock ReminderDeliveryResult.STALE
            if (wasDelivered(pending, store.delivered(now))) return@withLock ReminderDeliveryResult.ALREADY_DELIVERED
            if (now.isBefore(pending.fireAt)) return@withLock ReminderDeliveryResult.TOO_EARLY
            // Re-read sources rather than trusting serialized notification text or extras.
            val current = ReminderPlanner.plan(snapshot, pending.fireAt.minusMillis(1), zone()).firstOrNull {
                it.id == pending.id && it.fireAt == pending.fireAt && it.occursAt == pending.occursAt &&
                    (pending.localTarget == null || it.deliveryKey == pending.deliveryKey)
            }
            if (current == null || !current.occursAt.isAfter(now) || Duration.between(current.fireAt, now) > Duration.ofDays(1)) {
                backend.cancel(id); delivery?.cancel(id); reconcile(snapshot, false)
                return@withLock ReminderDeliveryResult.STALE
            }
            val sink = delivery ?: return@withLock ReminderDeliveryResult.FAILED
            // At-most-once: durable claim precedes OS delivery. No duplicate after reboot/repeated broadcast.
            store.markDelivered(current, now)
            val posted = sink.post(current, settings.language, zone())
            backend.cancel(id)
            reconcile(snapshot, false)
            if (posted) ReminderDeliveryResult.DELIVERED else ReminderDeliveryResult.NO_PERMISSION
        }
    }

    fun stop() { observer?.cancel(); observer = null; scope.coroutineContext.cancelChildren() }
    suspend fun stopAndAwait() {
        val jobs = scope.coroutineContext[Job]?.children?.toList().orEmpty()
        stop()
        jobs.joinAll()
    }
    companion object { const val MAX_PENDING_ALARMS = 450 }
}

/** Application supplies the singleton to a cold receiver without constructing a second Room database. */
interface ReminderCoordinatorOwner { val reminderCoordinator: ReminderCoordinator }
