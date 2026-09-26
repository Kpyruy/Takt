package com.kpyruy.takt.app

enum class CreateItemType(
    val label: String,
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
}
