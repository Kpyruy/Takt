package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.*

fun formatLessonCount(count: Int, language: AppLanguage = TaktI18n.language) =
    formatCount(count, language, listOf(UiKey.QUANTITY_CLASS_ONE, UiKey.QUANTITY_CLASS_FEW, UiKey.QUANTITY_CLASS_MANY))
fun formatWorkCount(count: Int, language: AppLanguage = TaktI18n.language) =
    formatCount(count, language, listOf(UiKey.QUANTITY_TASK_ONE, UiKey.QUANTITY_TASK_FEW, UiKey.QUANTITY_TASK_MANY))
internal fun formatLeadCount(count: Long, unit: ReminderLeadUnit, language: AppLanguage): String = when (unit) {
    ReminderLeadUnit.DAYS -> formatCount(count.toInt(), language, listOf(UiKey.QUANTITY_DAY_ONE, UiKey.QUANTITY_DAY_FEW, UiKey.QUANTITY_DAY_MANY))
    ReminderLeadUnit.WEEKS -> formatCount(count.toInt(), language, listOf(UiKey.QUANTITY_WEEK_ONE, UiKey.QUANTITY_WEEK_FEW, UiKey.QUANTITY_WEEK_MANY))
    else -> TaktI18n.text(uiMessage(UiKey.FORMAT_QUANTITY, count, uiArg(unit.label)), language)
}
private fun formatCount(count: Int, language: AppLanguage, forms: List<String>): String {
    val index = when (language) {
        AppLanguage.ENGLISH -> if (count == 1) 0 else 2
        AppLanguage.SLOVAK -> when (count) { 1 -> 0; in 2..4 -> 1; else -> 2 }
        AppLanguage.UKRAINIAN -> when {
            count % 100 in 11..14 -> 2
            count % 10 == 1 -> 0
            count % 10 in 2..4 -> 1
            else -> 2
        }
    }
    return TaktI18n.text(uiMessage(UiKey.FORMAT_QUANTITY, count, uiArg(forms[index])), language)
}
