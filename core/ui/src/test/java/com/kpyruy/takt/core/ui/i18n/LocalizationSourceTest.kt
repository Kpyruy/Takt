package com.kpyruy.takt.core.ui.i18n

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationSourceTest {
    @Test fun translationCallsInProductionUseEnglishSourceKeys() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val call = Regex("\\btf?\\(\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val violations = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".local") }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .flatMap { file ->
                call.findAll(file.readText()).filter { match ->
                    match.groupValues[1].any { it in '\u0400'..'\u04ff' }
                }.map { match -> "${file.relativeTo(root)}: ${match.groupValues[1]}" }
            }.toList()
        assertTrue("Translation calls must use English source keys:\n${violations.joinToString("\n")}", violations.isEmpty())
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
