package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.AndroidReminderPermissions
import com.kpyruy.takt.core.data.ReminderCoordinatorOwner
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.NotificationTranslations
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t

@Composable
fun NotificationsSettingsPage(settings: ReminderSettings, onChange: ((ReminderSettings) -> ReminderSettings) -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<ReminderCategory?>(null) }
    var academic by remember { mutableStateOf(false) }
    selected?.let { category ->
        ReminderCategoryPage(category, settings.rule(category),
            onChange = { transform -> onChange { current -> current.withRule(category, transform(current.rule(category))) } }, onBack = { selected = null })
        return
    }
    if (academic) {
        NotificationsPageFrame(t(UiKey.TEXT_ACADEMIC_WORK), { academic = false }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onChange { it.withAllAcademic(true) } }, Modifier.weight(1f).testTag("reminder_academic_all")) {
                    Text(t(UiKey.TEXT_SELECT_ALL_TYPES))
                }
                OutlinedButton(onClick = { onChange { it.withAllAcademic(false) } }, Modifier.weight(1f).testTag("reminder_academic_none")) {
                    Text(t(UiKey.TEXT_DISABLE_ALL_TYPES))
                }
            }
            ReminderCategory.entries.filter { it.academic }.forEach { category ->
                ReminderCategoryCard(category, settings.rule(category),
                    { onChange { current -> current.withRule(category, current.rule(category).copy(enabled = !current.rule(category).enabled)) } }, { selected = category })
            }
        }
        return
    }
    NotificationsPageFrame(t(UiKey.TEXT_NOTIFICATIONS), onBack) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Text(t(UiKey.TEXT_LOCAL_REMINDERS), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(settings.enabled, { onChange { current -> current.copy(enabled = !current.enabled) } }, Modifier.testTag("reminder_global_switch"))
            }
            Text(t(if (settings.enabled) UiKey.TEXT_REMINDERS_BEFORE_LESSONS_EVENTS_AND_WORK_DUE_DATES
                else UiKey.TEXT_ALL_REMINDERS_ARE_OFF), style = MaterialTheme.typography.bodySmall)
        }
        if (settings.enabled) NotificationPermissionCard() else {
            Text(t(UiKey.TEXT_NO_PERMISSIONS_ARE_NEEDED_WHILE_REMINDERS_ARE_OFF), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t(UiKey.TEXT_ACADEMIC_WORK), style = MaterialTheme.typography.titleMedium)
                    Text(t(if (ReminderCategory.entries.filter { it.academic }.all { settings.rule(it).enabled })
                        UiKey.TEXT_ALL_WORK_TYPES else UiKey.TEXT_SELECTED_WORK_TYPES), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(settings.academicEnabled, { onChange { current -> current.copy(academicEnabled = !current.academicEnabled) } }, Modifier.testTag("reminder_academic_switch"))
            }
            OutlinedButton(onClick = { academic = true }, Modifier.fillMaxWidth().testTag("reminder_academic_types")) {
                Text(t(UiKey.TEXT_TYPES_AND_REMINDER_TIMING))
            }
        }
        listOf(ReminderCategory.TASK, ReminderCategory.LESSON, ReminderCategory.EVENT, ReminderCategory.REMINDER).forEach { category ->
            ReminderCategoryCard(category, settings.rule(category),
                { onChange { current -> current.withRule(category, current.rule(category).copy(enabled = !current.rule(category).enabled)) } }, { selected = category })
        }
        SectionCard {
            TaktTimePickerField(t(UiKey.TEXT_TIME_FOR_WORK_WITHOUT_A_START_TIME), settings.dateOnlyTime,
                { onChange { current -> current.copy(dateOnlyTime = it) } }, Modifier.fillMaxWidth().testTag("reminder_date_only_time"))
            Text(t(UiKey.TEXT_FOR_TASKS_AND_WORK_WITH_A_DATE_A_TEST_LINKED_TO_A_LESSON_USES_47047D89),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotificationPermissionCard() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val coordinator = (context.applicationContext as? ReminderCoordinatorOwner)?.reminderCoordinator
    val coordinatorStatus = coordinator?.status?.collectAsStateWithLifecycle()?.value
    var permissions by remember { mutableStateOf(AndroidReminderPermissions.read(context)) }
    fun refresh() {
        permissions = AndroidReminderPermissions.read(context)
        (context.applicationContext as? ReminderCoordinatorOwner)?.reminderCoordinator?.requestRefresh()
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val systemSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    SectionCard(modifier = Modifier.testTag("reminder_permissions")) {
        Text(t(UiKey.TEXT_ANDROID_PERMISSIONS), style = MaterialTheme.typography.titleMedium)
        if (coordinatorStatus?.lastError != null) {
            Text(t(UiKey.TEXT_COULD_NOT_REFRESH_REMINDERS), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { coordinator?.requestRefresh() }, Modifier.fillMaxWidth()) { Text(t(UiKey.TEXT_TRY_AGAIN)) }
        }
        Text(t(if (permissions.notificationsAllowed) UiKey.TEXT_NOTIFICATIONS_ALLOWED else UiKey.TEXT_NOTIFICATIONS_BLOCKED),
            Modifier.testTag("reminder_notification_permission_status"))
        if (!permissions.notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
            TaktPrimaryButton(onClick = { request.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().testTag("reminder_request_notifications")) { Text(t(UiKey.TEXT_ALLOW_NOTIFICATIONS)) }
        }
        if (permissions.blockedChannels.isNotEmpty()) Text(t(UiKey.TEXT_SOME_CHANNELS_ARE_DISABLED_IN_ANDROID))
        OutlinedButton(onClick = { systemSettings.launch(AndroidReminderPermissions.notificationSettingsIntent(context)) },
            Modifier.fillMaxWidth().testTag("reminder_android_notification_settings")) { Text(t(UiKey.TEXT_ANDROID_NOTIFICATION_SETTINGS)) }
        Text(t(if (permissions.exactAlarmsAllowed) UiKey.TEXT_EXACT_REMINDERS_ALLOWED
            else UiKey.TEXT_APPROXIMATE_TIMING_ANDROID_MAY_DELAY_REMINDERS), Modifier.testTag("reminder_exact_permission_status"))
        if (!permissions.exactAlarmsAllowed && Build.VERSION.SDK_INT >= 31) {
            OutlinedButton(onClick = { systemSettings.launch(AndroidReminderPermissions.exactAlarmSettingsIntent(context)) },
                Modifier.fillMaxWidth().testTag("reminder_request_exact")) { Text(t(UiKey.TEXT_ALLOW_EXACT_TIMING)) }
        }
        Text(t(UiKey.TEXT_BATTERY_RESTRICTIONS_OR_FORCE_STOPPING_MAY_PREVENT_ANDROID_DELIVERY),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReminderCategoryCard(category: ReminderCategory, rule: ReminderRule,
    onEnabled: (Boolean) -> Unit, onEdit: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t(category.label), style = MaterialTheme.typography.titleMedium)
                Text(if (rule.enabled && rule.offsetsMinutes.isNotEmpty()) rule.offsetsMinutes.joinToString(" · ") {
                    NotificationTranslations.offset(it, TaktI18n.language)
                } else t(UiKey.TEXT_OFF), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(rule.enabled, onEnabled, Modifier.testTag("reminder_rule_${category.name}"))
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("reminder_edit_${category.name}")) {
            Text(t(UiKey.TEXT_REMINDER_TIMING))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCategoryPage(category: ReminderCategory, rule: ReminderRule,
    onChange: ((ReminderRule) -> ReminderRule) -> Unit, onBack: () -> Unit) {
    var custom by remember { mutableStateOf(false) }
    NotificationsPageFrame(t(category.label), onBack) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t(UiKey.TEXT_ON_13001175), Modifier.weight(1f))
                Switch(rule.enabled, { onChange { current -> current.copy(enabled = !current.enabled) } }, Modifier.testTag("reminder_rule_${category.name}"))
            }
            Text(t(UiKey.TEXT_BEFORE_START_OR_DUE_DATE), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (rule.offsetsMinutes.isEmpty()) Text(t(UiKey.TEXT_NO_REMINDER_TIMES_SELECTED))
            rule.offsetsMinutes.forEach { minutes ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(NotificationTranslations.offset(minutes, TaktI18n.language), Modifier.weight(1f))
                    TaktIconButton(Icons.Outlined.DeleteOutline, t(UiKey.TEXT_REMOVE_TIMING),
                        { onChange { current -> current.copy(offsetsMinutes = current.offsetsMinutes - minutes) } },
                        modifier = Modifier.testTag("reminder_remove_${category.name}_$minutes"))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10_080L, 1_440L, 60L, 30L, 10L).forEach { minutes ->
                    FilterChip(selected = minutes in rule.offsetsMinutes, onClick = {
                        onChange { current -> current.copy(offsetsMinutes = if (minutes in current.offsetsMinutes)
                            current.offsetsMinutes - minutes else current.offsetsMinutes + minutes).normalized() }
                    }, label = { Text(NotificationTranslations.offset(minutes, TaktI18n.language)) },
                        modifier = Modifier.testTag("reminder_preset_${category.name}_$minutes"))
                }
            }
            OutlinedButton(onClick = { custom = true }, modifier = Modifier.fillMaxWidth().testTag("reminder_custom_${category.name}")) {
                Text(t(UiKey.TEXT_ADD_CUSTOM_TIMING))
            }
        }
    }
    if (custom) CustomReminderOffset(onDismiss = { custom = false }, onAdd = {
        onChange { current -> current.copy(offsetsMinutes = current.offsetsMinutes + it).normalized() }; custom = false
    })
}

@Composable
private fun CustomReminderOffset(onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(ReminderLeadUnit.MINUTES) }
    var menu by remember { mutableStateOf(false) }
    val count = amount.toLongOrNull()
    val minutes = count?.takeIf { it > 0 && it <= MAX_REMINDER_LEAD_MINUTES / unit.minutes }?.times(unit.minutes)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(t(UiKey.TEXT_ADD_CUSTOM_TIMING)) },
        modifier = Modifier.glassSurface(MaterialTheme.shapes.extraLarge), containerColor = androidx.compose.ui.graphics.Color.Transparent,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TaktTextField(amount, { amount = it.filter(Char::isDigit).take(7) }, label = { Text(t(UiKey.TEXT_AMOUNT)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("reminder_custom_amount"))
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth().testTag("reminder_custom_unit")) {
                        Text(t(UiKey.TEXT_TIME_UNIT) + ": " + t(unit.label))
                    }
                    TaktChoiceMenu(menu, { menu = false }) {
                        ReminderLeadUnit.entries.forEach { choice ->
                            TaktChoiceMenuItem(text = { Text(t(choice.label)) },
                                onClick = { unit = choice; menu = false }, modifier = Modifier.testTag("reminder_unit_${choice.name}"))
                        }
                    }
                }
                Text(t(UiKey.TEXT_ENTER_A_POSITIVE_TIME_UP_TO_365_DAYS), style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TaktPrimaryButton(onClick = { minutes?.let(onAdd) }, enabled = minutes != null,
            modifier = Modifier.testTag("reminder_custom_add")) { Text(t(UiKey.TEXT_ADD)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t(UiKey.ACTION_CANCEL)) } })
}

@Composable
private fun NotificationsPageFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(title = title, navigation = { TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t(UiKey.ACTION_BACK), onBack) })
        content()
    }
}
