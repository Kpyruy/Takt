package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

enum class CreateItemType(
    private val ukrainianLabel: String,
    val requiresCourse: Boolean,
) {
    COURSE("Предмет", false),
    CLASS("Пара", false),
    TASK("Завдання", true),
    TEST("Тест", true),
    EXAM("Екзамен", true),
    NOTE("Нотатка", true),
    EVENT("Подія", false),
    REMINDER("Нагадування", false),

    ;
    val label: String get() = t(ukrainianLabel)
}
