package com.kpyruy.takt.core.model

import java.time.LocalTime

enum class ReminderCategory(val label: String, val academic: Boolean = false) {
    TASK("Звичайні задачі"),
    TEST("Тест", true), LAB("Лабораторна", true), SEMINAR("Семінар", true),
    HOMEWORK("Домашня робота", true), PROJECT("Проєкт", true), ORAL("Усна відповідь", true),
    EXAM("Екзамен", true), OTHER("Інша навчальна робота", true),
    LESSON("Заняття"), EVENT("Події"), REMINDER("Нагадування");

    companion object {
        fun forAssessment(type: GradeItemType): ReminderCategory = when (type) {
            GradeItemType.MIDTERM -> TEST
            else -> valueOf(type.name)
        }
    }
}

/** Minutes are canonical: 1 day and 24 hours cannot produce duplicate offsets. */
data class ReminderRule(val enabled: Boolean = true, val offsetsMinutes: List<Long> = listOf(1440)) {
    fun normalized(): ReminderRule = copy(offsetsMinutes = offsetsMinutes
        .filter { it in 1..MAX_REMINDER_LEAD_MINUTES }.distinct().sortedDescending())
}

const val MAX_REMINDER_LEAD_MINUTES: Long = 365L * 24 * 60

enum class ReminderLeadUnit(val minutes: Long, val label: String) {
    MINUTES(1, "хв"), HOURS(60, "год"), DAYS(1440, "дн"), WEEKS(10080, "тиж")
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val academicEnabled: Boolean = true,
    val dateOnlyTime: LocalTime = LocalTime.of(9, 0),
    val rules: Map<ReminderCategory, ReminderRule> = defaultReminderRules(),
) {
    fun rule(category: ReminderCategory): ReminderRule = rules[category] ?: defaultReminderRule(category)
    fun allows(category: ReminderCategory): Boolean = enabled &&
        (!category.academic || academicEnabled) && rule(category).enabled && rule(category).offsetsMinutes.isNotEmpty()
    fun withRule(category: ReminderCategory, rule: ReminderRule): ReminderSettings =
        copy(rules = rules + (category to rule.normalized()))
    fun withAllAcademic(enabled: Boolean): ReminderSettings = copy(rules = ReminderCategory.entries
        .associateWith { category -> rule(category).let { if (category.academic) it.copy(enabled = enabled) else it } })
    fun normalized(): ReminderSettings = copy(dateOnlyTime = dateOnlyTime.withSecond(0).withNano(0),
        rules = ReminderCategory.entries.associateWith { rule(it).normalized() })
}

private fun defaultReminderRule(category: ReminderCategory) = ReminderRule(offsetsMinutes =
    if (category in setOf(ReminderCategory.LESSON, ReminderCategory.EVENT, ReminderCategory.REMINDER)) listOf(30) else listOf(1440))

fun defaultReminderRules(): Map<ReminderCategory, ReminderRule> = ReminderCategory.entries.associateWith(::defaultReminderRule)
