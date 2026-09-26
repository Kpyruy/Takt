package com.kpyruy.takt.app

import com.kpyruy.takt.core.data.TaktDataContainer

/** Review-only fixture; the installed app never inserts sample courses or lessons. */
suspend fun TaktDataContainer.seedIfNeeded() {
    if (database.courseDao().count() != 0) return
    database.courseDao().insertAll(StudyPlanSeed.courses)
    DefaultTimetable.rules.forEach { scheduleRepository.upsertRule(it) }
    DefaultTimetable.oneOffEvents.forEach { scheduleRepository.upsertOneOffEvent(it) }
}
