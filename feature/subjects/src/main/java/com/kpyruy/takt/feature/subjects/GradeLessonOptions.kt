package com.kpyruy.takt.feature.subjects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.model.ScheduleResolver
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun rememberGradeLessonOptions(
    scheduleRepository: ScheduleRepository?,
    settingsRepository: AppSettingsRepository?,
    courseId: String,
    date: LocalDate?,
): List<ResolvedScheduleEvent> {
    if (scheduleRepository == null || date == null) return emptyList()

    val rules by remember(scheduleRepository) { scheduleRepository.observeRules() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val exceptions by remember(scheduleRepository) { scheduleRepository.observeExceptions() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val oneOffEvents by remember(scheduleRepository) { scheduleRepository.observeOneOffEvents() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by remember(settingsRepository) { settingsRepository?.settings ?: flowOf(AppSettings()) }
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    return remember(rules, exceptions, oneOffEvents, settings, courseId, date) {
        settings.filterScheduleEvents(ScheduleResolver.eventsForDate(
            rules, exceptions, oneOffEvents, date, settings.effectiveParity(date),
        )).filter { it.courseId == courseId && it.status != ScheduleEventStatus.CANCELLED }
            .sortedWith(compareBy<ResolvedScheduleEvent> { it.startTime }.thenBy { it.id })
    }
}
