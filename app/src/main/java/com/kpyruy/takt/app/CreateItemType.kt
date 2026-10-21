package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

enum class CreateItemType(
    private val sourceLabel: String,
    val requiresCourse: Boolean,
) {
    COURSE(UiKey.TEXT_SUBJECT_68971283, false),
    CLASS(UiKey.TEXT_CLASS, false),
    TASK(UiKey.TEXT_ASSIGNMENT, true),
    TEST(UiKey.TEXT_TEST_532EAABD, true),
    EXAM(UiKey.TEXT_EXAM, true),
    NOTE(UiKey.TEXT_NOTE, true),
    EVENT(UiKey.TEXT_EVENT, false),
    REMINDER(UiKey.TEXT_REMINDER, false),

    ;
    val label: String get() = t(sourceLabel)
}
