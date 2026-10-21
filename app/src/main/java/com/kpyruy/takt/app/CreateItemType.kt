package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

enum class CreateItemType(
    private val sourceLabel: String,
    val requiresCourse: Boolean,
) {
    COURSE("Subject", false),
    CLASS("Class", false),
    TASK("Assignment", true),
    TEST("Test", true),
    EXAM("Exam", true),
    NOTE("Note", true),
    EVENT("Event", false),
    REMINDER("Reminder", false),

    ;
    val label: String get() = t(sourceLabel)
}
