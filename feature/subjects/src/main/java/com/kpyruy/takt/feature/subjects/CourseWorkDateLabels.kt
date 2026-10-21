package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun overdueLabel(date: LocalDate, today: LocalDate): String =
    t(UiKey.TEXT_OVERDUE) + " · " + date.format(DateTimeFormatter.ofPattern(
        if (date.year == today.year) "dd.MM" else "dd.MM.yyyy"))
