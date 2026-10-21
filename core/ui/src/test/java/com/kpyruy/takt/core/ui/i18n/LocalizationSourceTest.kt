package com.kpyruy.takt.core.ui.i18n

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationSourceTest {
    @Test fun inlineUiProseBelongsInTheTranslationCatalog() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        // Data interpolation, punctuation, grade codes and palette glyphs are not UI prose.
        val displayLiteral = Regex("(?:\\bText\\(\\s*(?:text\\s*=\\s*)?|contentDescription\\s*=\\s*)\"([^\"$\\n]*)\"")
        val violations = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".local") }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .flatMap { file -> displayLiteral.findAll(file.readText()).filter {
                val value = it.groupValues[1]
                value !in setOf("A", "FX") && value.replace(Regex("%[0-9]*[dfs]"), "").any(Char::isLetter)
            }.map { "${file.relativeTo(root)}: ${it.groupValues[1]}" } }.toList()
        assertTrue("Inline display text must use localization keys:\n${violations.joinToString("\n")}", violations.isEmpty())
    }
    @Test fun translationCallsInProductionUseStableResourceReferences() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val call = Regex("\\b(?:t|tf|uiMessage|uiArg)\\(\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val violations = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".local") }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .flatMap { file ->
                call.findAll(file.readText()).filter { match ->
                    match.groupValues[1].isNotEmpty()
                }.map { match -> "${file.relativeTo(root)}: ${match.groupValues[1]}" }
            }.toList()
        assertTrue("Translation calls must use registered resource references:\n${violations.joinToString("\n")}", violations.isEmpty())
    }
    @Test fun productionCodeKeepsCyrillicInLocalizationCatalogsOnly() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val violations = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".local") }
            .filter { it.isFile && it.extension in setOf("kt", "java") &&
                "/src/main/" in it.invariantSeparatorsPath && "/i18n/" !in it.invariantSeparatorsPath }
            .filter { file -> file.readText().any { it in '\u0400'..'\u04ff' } }
            .map { it.relativeTo(root).toString() }.toList()
        assertTrue("Production text belongs in the translation catalogs: $violations", violations.isEmpty())
    }

}
