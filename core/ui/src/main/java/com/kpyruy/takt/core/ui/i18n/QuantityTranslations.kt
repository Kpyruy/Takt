package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage

fun formatLessonCount(count: Int, language: AppLanguage = TaktI18n.language): String =
    formatCount(count, language, listOf("class", "classes"), listOf("пара", "пари", "пар"), listOf("hodina", "hodiny", "hodín"))

fun formatWorkCount(count: Int, language: AppLanguage = TaktI18n.language): String =
    formatCount(count, language, listOf("task", "tasks"), listOf("робота", "роботи", "робіт"), listOf("úloha", "úlohy", "úloh"))

private fun formatCount(count: Int, language: AppLanguage, english: List<String>, ukrainian: List<String>, slovak: List<String>): String {
    val noun = when (language) {
        AppLanguage.ENGLISH -> english[if (count == 1) 0 else 1]
        AppLanguage.SLOVAK -> slovak[when (count) { 1 -> 0; in 2..4 -> 1; else -> 2 }]
        AppLanguage.UKRAINIAN -> ukrainian[when {
            count % 100 in 11..14 -> 2
            count % 10 == 1 -> 0
            count % 10 in 2..4 -> 1
            else -> 2
        }]
    }
    return "$count $noun"
}
