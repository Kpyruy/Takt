package com.kpyruy.takt.app

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.NotificationTranslations
import java.time.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Isolated settings/alarm stores and injected clock; never replaces the app's real study data. */
class ReminderCoordinatorNativeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("Europe/Bratislava")
    private class MutableClock(var at: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = at
    }
    private class Alarms(var allowed: Boolean = true, var exact: Boolean = true) : ReminderAlarmBackend {
        val armed = mutableMapOf<String, PlannedReminder>()
        val cancelled = mutableSetOf<String>()
        var maintenance: Instant? = null
        var schedules = 0
        var permissionReads = 0
        var maintenanceSchedules = 0
        var maintenanceCancels = 0
        override fun permissions(): ReminderPermissionStatus { permissionReads++; return ReminderPermissionStatus(allowed, exact) }
        override fun schedule(reminder: PlannedReminder, triggerAt: Instant): Boolean { schedules++; armed[reminder.id] = reminder; return exact }
        override fun cancel(id: String) { armed.remove(id); cancelled += id }
        override fun scheduleMaintenance(at: Instant) { maintenanceSchedules++; maintenance = at }
        override fun cancelMaintenance() { maintenanceCancels++; maintenance = null }
    }
    private class Notifications : ReminderNotificationDelivery {
        val posted = mutableListOf<PlannedReminder>()
        val visible = mutableSetOf<String>()
        val channelLanguages = mutableListOf<AppLanguage>()
        var allCancels = 0
        override fun bootstrapChannels(language: AppLanguage) { channelLanguages += language }
        override fun post(reminder: PlannedReminder, language: AppLanguage, zone: ZoneId): Boolean { posted += reminder; visible += reminder.id; return true }
        override fun cancel(id: String) { visible -= id }
        override fun cancelAllReminders() { allCancels++; visible.clear() }
    }
    private val text = object : ReminderTextProvider {
        override fun channelName(id: String, language: AppLanguage) = NotificationTranslations.text(when (id) {
            ReminderChannels.WORK -> "Навчальна робота й задачі"; ReminderChannels.LESSON -> "Заняття"; else -> "Події й нагадування"
        }, language)
        override fun notification(reminder: PlannedReminder, language: AppLanguage, zone: ZoneId) = ReminderNotificationText(
            NotificationTranslations.title(reminder.category, reminder.title, language),
            NotificationTranslations.body(reminder.occursAt, reminder.dateOnly, reminder.courseTitle, language, zone))
    }
    private fun snapshot(clock: MutableClock): PlanningSnapshot {
        val target = clock.at.atZone(zone).toLocalDateTime().plusMinutes(5).withSecond(0).withNano(0)
        return PlanningSnapshot(emptyList(), emptyList(), emptyList(), listOf(OneOffScheduleEvent("native-${System.nanoTime()}",
            null, "Reminder native test", target.toLocalDate(), target.toLocalTime(), target.plusHours(1).toLocalTime(),
            type = OneOffScheduleEventType.REMINDER)), emptyList(), emptyList(), emptyList(),
            AppSettings(reminders = ReminderSettings(enabled = true).withRule(ReminderCategory.REMINDER, ReminderRule(true, listOf(1, 2)))))
    }
    private suspend fun withCoordinator(delivery: ReminderNotificationDelivery? = null,
        body: suspend (ReminderCoordinator, MutableStateFlow<PlanningSnapshot?>, MutableClock, Alarms, Notifications) -> Unit) {
        val name = "reminder-native-${System.nanoTime()}"
        val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
        val snapshots = MutableStateFlow<PlanningSnapshot?>(snapshot(clock))
        val backend = Alarms()
        val sink = Notifications()
        val coordinator = ReminderCoordinator(context, snapshots, { requireNotNull(snapshots.value) }, backend,
            clock, { zone }, ReminderAlarmStore(context, name))
        coordinator.start(delivery ?: sink)
        try { body(coordinator, snapshots, clock, backend, sink) }
        finally { coordinator.stopAndAwait(); context.deleteSharedPreferences(name) }
    }
    @Test fun offAndDeniedNotificationsCancelEveryAlarmAndMaintenance() = runBlocking {
        withCoordinator { coordinator, snapshots, _, backend, _ ->
            val first = coordinator.refreshNow()
            assertEquals(2, first.size)
            assertNotNull(backend.maintenance)
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(reminders = it.settings.reminders.copy(enabled = false))) }
            assertTrue(coordinator.refreshNow().isEmpty())
            assertTrue(backend.armed.isEmpty()); assertNull(backend.maintenance)
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(reminders =
                ReminderSettings(enabled = true, rules = ReminderCategory.entries.associateWith { ReminderRule(false, listOf(1)) }))) }
            assertTrue(coordinator.refreshNow().isEmpty()); assertNull(backend.maintenance)
            snapshots.value = snapshot(MutableClock(Instant.parse("2026-10-14T08:00:00Z")))
            backend.allowed = false
            assertTrue(coordinator.refreshNow().isEmpty())
            assertTrue(coordinator.readPlannedAlarms().isEmpty())
        }
    }
    @Test fun inexactFallbackEditsDeleteAndExactGrantReconcileStableAlarms() = runBlocking {
        withCoordinator { coordinator, snapshots, _, backend, _ ->
            backend.exact = false
            val first = coordinator.refreshNow()
            assertFalse(coordinator.status.value.exact)
            assertEquals(first, coordinator.readPlannedAlarms())
            snapshots.value = snapshots.value!!.let { it.copy(oneOffEvents = it.oneOffEvents.map { event -> event.copy(startTime = event.startTime.plusMinutes(10)) }) }
            val changed = coordinator.refreshNow()
            assertEquals(first.map { it.id }, changed.map { it.id })
            assertNotEquals(first.first().fireAt, changed.first().fireAt)
            assertTrue(backend.cancelled.containsAll(first.map { it.id }))
            backend.exact = true
            coordinator.refreshNow()
            assertTrue(coordinator.status.value.exact)
            snapshots.value = snapshots.value!!.copy(oneOffEvents = emptyList())
            assertTrue(coordinator.refreshNow().isEmpty()); assertTrue(backend.armed.isEmpty())
        }
    }
    @Test fun deliveryRechecksSourcesAndDoesNotDismissOrRepeatNotifications() = runBlocking {
        withCoordinator { coordinator, snapshots, clock, backend, sink ->
            val first = coordinator.refreshNow().first()
            assertEquals(ReminderDeliveryResult.TOO_EARLY, coordinator.deliver(first.id, first.fireAt.toEpochMilli()))
            clock.at = first.fireAt.plusSeconds(1)
            assertEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(first.id, first.fireAt.toEpochMilli()))
            assertNotEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(first.id, first.fireAt.toEpochMilli()))
            assertEquals(1, sink.posted.size)
            assertTrue(first.id in sink.visible)
            assertTrue(coordinator.refreshNow().none { it.id == first.id })
            val remaining = coordinator.readPlannedAlarms().single()
            snapshots.value = snapshots.value!!.copy(oneOffEvents = emptyList())
            clock.at = remaining.fireAt.plusSeconds(1)
            assertEquals(ReminderDeliveryResult.STALE, coordinator.deliver(remaining.id, remaining.fireAt.toEpochMilli()))
            assertEquals(1, sink.posted.size); assertTrue(backend.armed.isEmpty())
        }
    }
    @Test fun durableDeliveredClaimSurvivesCoordinatorRecreation() = runBlocking {
        val name = "reminder-restart-native-${System.nanoTime()}"
        val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
        val snapshots = MutableStateFlow<PlanningSnapshot?>(snapshot(clock))
        val backend = Alarms()
        val sink = Notifications()
        fun create() = ReminderCoordinator(context, snapshots, { requireNotNull(snapshots.value) }, backend,
            clock, { zone }, ReminderAlarmStore(context, name)).also { it.start(sink) }
        var coordinator = create()
        try {
            val reminder = coordinator.refreshNow().first()
            clock.at = reminder.fireAt.plusSeconds(1)
            assertEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(reminder.id, reminder.fireAt.toEpochMilli()))
            coordinator.stopAndAwait()
            coordinator = create()
            assertTrue(coordinator.refreshNow().none { it.id == reminder.id })
            assertNotEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(reminder.id, reminder.fireAt.toEpochMilli()))
            assertEquals(1, sink.posted.size)
        } finally { coordinator.stopAndAwait(); context.deleteSharedPreferences(name) }
    }
    @Test fun timezoneRefreshKeepsIdentityAndReplacesEpochTimes() = runBlocking {
        val name = "reminder-zone-native-${System.nanoTime()}"
        val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
        val snapshots = MutableStateFlow<PlanningSnapshot?>(snapshot(clock))
        val backend = Alarms()
        var selectedZone = zone
        val coordinator = ReminderCoordinator(context, snapshots, { requireNotNull(snapshots.value) }, backend,
            clock, { selectedZone }, ReminderAlarmStore(context, name))
        coordinator.start(Notifications())
        try {
            val before = coordinator.refreshNow()
            selectedZone = ZoneOffset.UTC
            val after = coordinator.refreshNow()
            assertEquals(before.map { it.id }, after.map { it.id })
            assertNotEquals(before.first().fireAt, after.first().fireAt)
            assertTrue(backend.cancelled.containsAll(before.map { it.id }))
        } finally { coordinator.stopAndAwait(); context.deleteSharedPreferences(name) }
    }
    @Test fun newRepositoryReadsDurableSettingsAndAppearanceCannotOverwriteThem() = runBlocking {
        val name = "reminder-settings-native-${System.nanoTime()}"
        try {
            val repository = SharedPreferencesAppSettingsRepository(context, name)
            val config = ReminderSettings(enabled = true).withRule(ReminderCategory.TEST, ReminderRule(true, listOf(10080, 1440)))
            repository.setReminders(config)
            repository.setAppearance(AppSettings(themeFamily = ThemeFamily.PURPLE))
            assertEquals(config, SharedPreferencesAppSettingsRepository(context, name).settings.value.reminders)
            repository.setReminders(config.copy(enabled = false))
            assertFalse(SharedPreferencesAppSettingsRepository(context, name).settings.value.reminders.enabled)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun realAndroidNotificationDeliveryUsesChannelsAndOrdinaryLockGatedLauncher() = runBlocking {
        // Root opts into OS permission before this test; it never prompts or changes permission itself.
        assumeTrue(AndroidReminderPermissions.read(context).notificationsAllowed)
        val delivery = AndroidReminderNotificationDelivery(context, text)
        withCoordinator(delivery) { coordinator, snapshots, clock, _, _ ->
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(language = AppLanguage.SLOVAK)) }
            val reminder = coordinator.refreshNow().first()
            val manager = context.getSystemService(NotificationManager::class.java)
            try {
                delivery.bootstrapChannels(AppLanguage.SLOVAK)
                assumeTrue(manager.getNotificationChannel(ReminderChannels.EVENT).importance != NotificationManager.IMPORTANCE_NONE)
                clock.at = reminder.fireAt.plusSeconds(1)
                assertEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(reminder.id, reminder.fireAt.toEpochMilli()))
                val posted = manager.activeNotifications.first { it.tag == AndroidReminderNotificationDelivery.TAG_PREFIX + reminder.id }
                assertEquals(ReminderChannels.EVENT, posted.notification.channelId)
                assertNotNull(posted.notification.contentIntent)
                assertEquals("Pripomienka: Reminder native test", posted.notification.extras.getString(android.app.Notification.EXTRA_TITLE))
                assertNotEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(reminder.id, reminder.fireAt.toEpochMilli()))
            } finally { delivery.cancel(reminder.id) }
        }
    }
    @Test fun deliveredLeadSurvivesTimezoneChangeAndRestartButLocalTimeEditRearms() = runBlocking {
        val name = "reminder-zone-consumed-${System.nanoTime()}"
        val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
        val snapshots = MutableStateFlow<PlanningSnapshot?>(snapshot(clock))
        val backend = Alarms()
        val sink = Notifications()
        var selectedZone = zone
        fun create() = ReminderCoordinator(context, snapshots, { snapshots.value!! }, backend,
            clock, { selectedZone }, ReminderAlarmStore(context, name)).also { it.start(sink) }
        var coordinator = create()
        try {
            val before = coordinator.refreshNow().first()
            clock.at = before.fireAt.plusSeconds(1)
            assertEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(before.id, before.fireAt.toEpochMilli()))
            coordinator.stopAndAwait()
            selectedZone = ZoneOffset.UTC
            coordinator = create()
            assertTrue(coordinator.refreshNow().none { it.id == before.id })
            assertNotEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(before.id, before.fireAt.toEpochMilli()))
            assertEquals(1, sink.posted.size)
            snapshots.value = snapshots.value!!.let { it.copy(oneOffEvents = it.oneOffEvents.map { event ->
                event.copy(startTime = event.startTime.plusMinutes(10)) }) }
            val edited = coordinator.refreshNow().single { it.id == before.id }
            assertNotEquals(before.deliveryKey, edited.deliveryKey)
            assertEquals(ReminderDeliveryResult.STALE, coordinator.deliver(before.id, before.fireAt.toEpochMilli()))
            clock.at = edited.fireAt.plusSeconds(1)
            assertEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(edited.id, edited.fireAt.toEpochMilli()))
            assertNotEquals(ReminderDeliveryResult.DELIVERED, coordinator.deliver(edited.id, edited.fireAt.toEpochMilli()))
            assertEquals(2, sink.posted.size)
        } finally { coordinator.stopAndAwait(); context.deleteSharedPreferences(name) }
    }

    @Test fun ordinaryObservedThemeAndLanguageChangesDoNotRearm450UnchangedAlarms() = runBlocking {
        withCoordinator { coordinator, snapshots, _, backend, sink ->
            val base = snapshots.value!!
            val event = base.oneOffEvents.single()
            snapshots.value = base.copy(oneOffEvents = (1..225).map { event.copy(id = "bulk-$it") })
            coordinator.refreshNow()
            delay(700) // Let the initial debounced emission settle before counting ordinary updates.
            val scheduled = backend.schedules
            val maintenance = backend.maintenanceSchedules
            assertEquals(450, backend.armed.size)
            val reads = backend.permissionReads
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(themeFamily = ThemeFamily.PURPLE)) }
            delay(700)
            assertEquals(scheduled, backend.schedules)
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(language = AppLanguage.SLOVAK)) }
            delay(700)
            assertEquals(scheduled, backend.schedules)
            assertEquals(maintenance, backend.maintenanceSchedules)
            assertEquals(reads + 2, backend.permissionReads)
            assertEquals(AppLanguage.SLOVAK, sink.channelLanguages.last())
            snapshots.value = snapshots.value!!.let { it.copy(oneOffEvents = it.oneOffEvents.mapIndexed { index, item ->
                if (index == 0) item.copy(startTime = item.startTime.plusMinutes(1)) else item }) }
            // Wait for the observed source change, rather than racing its 350ms debounce
            // plus planning/persistence on a busy emulator. Reading the ledger takes the
            // coordinator mutex, so the assertion follows the completed reconciliation.
            withTimeout(5_000) {
                while (coordinator.readPlannedAlarms().none {
                        it.sourceKey.contains("bulk-1:") && it.localTarget?.toLocalTime() == event.startTime.plusMinutes(1)
                    }) delay(25)
            }
            assertEquals(scheduled + 2, backend.schedules)
            // Repeated OFF is cheap, while the first OFF still clears pending and visible work.
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(reminders = it.settings.reminders.copy(enabled = false))) }
            coordinator.refreshNow(false)
            delay(100)
            val cancels = backend.maintenanceCancels
            val notificationCancels = sink.allCancels
            coordinator.refreshNow(false)
            assertEquals(cancels, backend.maintenanceCancels)
            assertEquals(notificationCancels, sink.allCancels)
            assertTrue(backend.armed.isEmpty())
        }
    }

    @Test fun disabledObserverDoesNotReadStudyDataAgainUntilEnabled() = runBlocking {
        val name = "reminder-off-observer-${System.nanoTime()}"
        val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
        val base = snapshot(clock)
        val snapshots = MutableStateFlow<PlanningSnapshot?>(base.copy(settings = base.settings.copy(
            reminders = base.settings.reminders.copy(enabled = false))))
        val reads = java.util.concurrent.atomic.AtomicInteger()
        val coordinator = ReminderCoordinator(context, snapshots, {
            reads.incrementAndGet(); requireNotNull(snapshots.value)
        }, Alarms(), clock, { zone }, ReminderAlarmStore(context, name))
        coordinator.start(Notifications())
        try {
            withTimeout(5_000) { while (reads.get() == 0) delay(25) }
            coordinator.readPlannedAlarms() // Complete the first OFF cleanup under its mutex.
            for (family in listOf(ThemeFamily.PURPLE, ThemeFamily.GREEN, ThemeFamily.BLUE)) {
                snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(themeFamily = family)) }
                delay(100)
            }
            assertEquals("Disabled reminders must not requery Room for appearance updates", 1, reads.get())
            snapshots.value = snapshots.value!!.let { it.copy(settings = it.settings.copy(reminders =
                it.settings.reminders.copy(enabled = true))) }
            withTimeout(5_000) { while (reads.get() == 1) delay(25) }
            assertEquals(2, coordinator.readPlannedAlarms().size)
        } finally { coordinator.stopAndAwait(); context.deleteSharedPreferences(name) }
    }

    @Test fun atomicTransformsUseLatestDurableStateWhileFirstCommitIsDelayed() = runBlocking {
        val name = "reminder-delayed-settings-${System.nanoTime()}"
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val delayFirst = AtomicBoolean(true)
        val wrapped = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(key: String, mode: Int): android.content.SharedPreferences {
                val real = context.getSharedPreferences(key, mode)
                return object : android.content.SharedPreferences by real {
                    override fun edit(): android.content.SharedPreferences.Editor {
                        val editor = real.edit()
                        return object : android.content.SharedPreferences.Editor by editor {
                            override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor {
                                editor.putString(key, value); return this
                            }
                            override fun commit(): Boolean {
                                if (delayFirst.compareAndSet(true, false)) {
                                    entered.countDown()
                                    check(release.await(5, TimeUnit.SECONDS))
                                }
                                return editor.commit()
                            }
                        }
                    }
                }
            }
        }
        try {
            val repository = SharedPreferencesAppSettingsRepository(wrapped, name)
            val first = async(Dispatchers.IO) { repository.updateReminders { current -> current.withRule(ReminderCategory.TEST,
                current.rule(ReminderCategory.TEST).copy(offsetsMinutes = listOf(10080, 1440))) } }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val second = async(start = CoroutineStart.UNDISPATCHED) { repository.updateReminders { current -> current
                .withRule(ReminderCategory.TEST, current.rule(ReminderCategory.TEST).copy(offsetsMinutes = current.rule(ReminderCategory.TEST).offsetsMinutes + 60))
                .withRule(ReminderCategory.EVENT, current.rule(ReminderCategory.EVENT).copy(enabled = false)) } }
            assertFalse(second.isCompleted)
            release.countDown()
            first.await(); second.await()
            val durable = SharedPreferencesAppSettingsRepository(context, name).settings.value.reminders
            assertEquals(listOf(10080L, 1440L, 60L), durable.rule(ReminderCategory.TEST).offsetsMinutes)
            assertFalse(durable.rule(ReminderCategory.EVENT).enabled)
            assertEquals(durable, repository.settings.value.reminders)
        } finally { release.countDown(); context.deleteSharedPreferences(name) }
    }

    @Test fun roomExamSchedulingEditsReachLivePlanningAndReplaceLinkedUndatedAlarms() = runBlocking {
        val name = "reminder-exam-input-${System.nanoTime()}"
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context,
            com.kpyruy.takt.core.database.TaktDatabase::class.java).build()
        var collector: Job? = null
        var coordinator: ReminderCoordinator? = null
        try {
            db.courseDao().insertAll(listOf(com.kpyruy.takt.core.database.CourseEntity(
                "exam-course", "EX", "Exam course", 5, 3, "enrolled", "COMPULSORY", null)))
            val settings = SharedPreferencesAppSettingsRepository(context, "$name-settings")
            settings.setReminders(ReminderSettings(enabled = true, rules = ReminderCategory.entries.associateWith {
                ReminderRule(it == ReminderCategory.EXAM, listOf(30, 1440)) }))
            val exams = RoomExamRepository(db)
            val grades = RoomGradeRepository(db)
            val linked = GradeItem("exam-result", "exam-course", "Exam", GradeItemType.EXAM,
                0.0, 37.0, completed = false, contributesToGrade = false)
            grades.upsertItem(linked)
            val state = MutableStateFlow<PlanningSnapshot?>(null)
            val updates = Channel<PlanningSnapshot>(Channel.UNLIMITED)
            collector = launch(Dispatchers.IO) {
                observePlanningSnapshot(RoomStudyPlanRepository(db), RoomScheduleRepository(db),
                    RoomStudyContentRepository(db), grades, settings, exams).collect {
                    state.value = it; updates.send(it)
                }
            }
            suspend fun awaitInfo(expected: ExamInfo?): PlanningSnapshot = withTimeout(5_000) {
                var frame = updates.receive()
                while (frame.examInfos.singleOrNull() != expected) frame = updates.receive()
                frame
            }
            awaitInfo(null)
            val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
            val backend = Alarms()
            val active = ReminderCoordinator(context, state, { state.value!! }, backend,
                clock, { zone }, ReminderAlarmStore(context, name))
            coordinator = active
            active.start(Notifications())
            val info = ExamInfo("exam-course", linked.id, LocalDate.of(2026, 10, 22), LocalTime.of(14, 0), null, null)
            exams.upsertExamInfo(info)
            awaitInfo(info)
            val before = active.refreshNow(false)
            assertEquals(2, before.size)
            assertTrue(before.all { it.occursAt == Instant.parse("2026-10-22T12:00:00Z") })
            val edited = info.copy(date = LocalDate.of(2026, 10, 23), startTime = LocalTime.of(16, 0))
            exams.upsertExamInfo(edited)
            awaitInfo(edited)
            val after = active.refreshNow(false)
            assertEquals(before.map { it.id }.toSet(), after.map { it.id }.toSet())
            assertEquals(2, after.size)
            assertTrue(after.all { it.occursAt == Instant.parse("2026-10-23T14:00:00Z") })
            assertTrue(backend.cancelled.containsAll(before.map { it.id }))
            assertEquals(ReminderDeliveryResult.STALE, active.deliver(before.first().id, before.first().fireAt.toEpochMilli()))
            assertEquals(listOf(linked), grades.observeAllItems().first())
            grades.upsertItem(linked.copy(awaitingResult = true))
            withTimeout(5_000) { while (updates.receive().assessments.none { it.awaitingResult }) Unit }
            assertTrue(active.refreshNow(false).isEmpty())
        } finally {
            coordinator?.stopAndAwait()
            collector?.cancelAndJoin()
            db.close()
            context.deleteSharedPreferences(name)
            context.deleteSharedPreferences("$name-settings")
        }
    }

    @Test fun legacyClaimsRemainConsumedAndEmptyOffLedgerDoesNotWriteAgain() = runBlocking {
        val name = "reminder-store-compat-${System.nanoTime()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val clock = MutableClock(Instant.parse("2026-10-14T08:00:00Z"))
        val state = MutableStateFlow<PlanningSnapshot?>(snapshot(clock))
        val store = ReminderAlarmStore(context, name)
        val legacy = ReminderPlanner.plan(state.value!!, clock.at, zone).first().copy(localTarget = null)
        store.write(listOf(legacy))
        assertEquals(listOf(legacy), store.read())
        store.markDelivered(legacy, clock.at)
        val sink = Notifications()
        val backend = Alarms()
        val coordinator = ReminderCoordinator(context, state, { state.value!! }, backend,
            clock, { ZoneOffset.UTC }, store)
        coordinator.start(sink)
        try {
            assertTrue(coordinator.refreshNow().none { it.id == legacy.id })
            state.value = state.value!!.let { it.copy(settings = it.settings.copy(reminders = ReminderSettings())) }
            coordinator.refreshNow(false)
            delay(100)
            val persisted = preferences.all.toMap()
            val maintenanceCancels = backend.maintenanceCancels
            val notificationCancels = sink.allCancels
            repeat(2) { coordinator.refreshNow(false) }
            assertEquals(persisted, preferences.all)
            assertEquals(maintenanceCancels, backend.maintenanceCancels)
            assertEquals(notificationCancels, sink.allCancels)
        } finally { coordinator.stopAndAwait(); context.deleteSharedPreferences(name) }
    }

}
