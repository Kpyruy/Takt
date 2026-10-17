package com.kpyruy.takt.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidReminderAlarmBackend.ACTION_DELIVER) return
        val owner = context.applicationContext as? ReminderCoordinatorOwner ?: return
        val id = intent.getStringExtra(AndroidReminderAlarmBackend.EXTRA_ID) ?: return
        val fireAt = intent.getLongExtra(AndroidReminderAlarmBackend.EXTRA_FIRE_AT, -1L)
        if (fireAt < 0) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8_000) { owner.reminderCoordinator.deliver(id, fireAt) } }
            catch (error: Exception) { android.util.Log.w("TaktReminders", "Delivery deferred: ${error.javaClass.simpleName}") }
            finally { result.finish() }
        }
    }
}

class ReminderRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val owner = context.applicationContext as? ReminderCoordinatorOwner ?: return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8_000) { owner.reminderCoordinator.refreshNow(forceRearm = true) } }
            catch (error: Exception) { android.util.Log.w("TaktReminders", "Refresh deferred: ${error.javaClass.simpleName}") }
            finally { result.finish() }
        }
    }
    companion object {
        private val ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            AndroidReminderAlarmBackend.ACTION_MAINTENANCE)
    }
}
