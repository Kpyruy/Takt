package com.kpyruy.takt.core.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CourseIconsTest {
    @Test fun metadataLookupAndPickerFilteringDoNotBuildVectors() {
        val builds = mutableListOf<String>()
        val school = option("School", "Освіта школа", "Навчання", builds)
        val atom = option("Atom", "Фізика атом", "Наука", builds)
        val science = option("Science", "Хімія колба", "Наука", builds)
        val catalog = CourseIconCatalog(listOf(school, atom, science))

        assertNull(catalog.find(null))
        assertNull(catalog.find("missing"))
        assertSame(atom, catalog.find("Atom"))
        assertEquals(listOf("Навчання", "Наука"), catalog.all.map { it.category }.distinct())
        val filtered = catalog.all.filter { it.category == "Наука" && it.label.contains("атом") }
        assertEquals(listOf("Atom"), filtered.map { it.key })
        assertEquals(emptyList<String>(), builds)

        // The lazy grid reads vectors only for composed items, then reuses them.
        val firstVector = filtered.single().vector
        assertSame(firstVector, catalog.find("Atom")!!.vector)
        assertEquals(listOf("Atom"), builds)
        school.vector
        assertEquals(listOf("Atom", "School"), builds)
    }

    @Test fun metadataCopyKeepsTheVectorLazyAndSharesTheCachedValue() {
        val builds = mutableListOf<String>()
        val original = option("Atom", "Фізика атом", "Наука", builds)
        val renamed = original.copy(label = "Атом")

        assertEquals("Атом", renamed.label)
        assertEquals(emptyList<String>(), builds)
        assertSame(original.vector, renamed.vector)
        assertEquals(listOf("Atom"), builds)
    }

    @Test fun existingVectorConstructorCopyAndDestructuringRemainCompatible() {
        val vector = vector("Atom")
        val original = CourseIconOption("Atom", "Фізика атом", "Наука", vector)
        val (key, label, category, destructuredVector) = original
        assertEquals("Atom", key)
        assertEquals("Фізика атом", label)
        assertEquals("Наука", category)
        assertSame(vector, destructuredVector)
        assertEquals(original, original.copy())
        assertEquals(original.hashCode(), original.copy().hashCode())
        val replacement = vector("Replacement")
        assertSame(replacement, original.copy(vector = replacement).vector)
    }

    @Test fun storedKeysStillResolveAndPickerKeepsItsCatalogueOrder() {
        val options = CourseIcons.all
        assertEquals(110, options.size)
        assertEquals(options.size, options.map { it.key }.distinct().size)
        assertEquals(listOf("School", "MenuBook", "AutoStories"), options.take(3).map { it.key })
        assertEquals(listOf("StarOutline", "FavoriteBorder", "Diamond"), options.takeLast(3).map { it.key })
        assertEquals(
            listOf("Навчання", "Наука", "Технології", "Інженерія", "Економіка", "Мови й творчість", "Спорт та інше"),
            options.map { it.category }.distinct(),
        )
        options.forEach { assertSame(it, CourseIcons.find(it.key)) }
        assertNull(CourseIcons.find(null))
        assertNull(CourseIcons.find("missing"))
    }

    private fun option(key: String, label: String, category: String, builds: MutableList<String>) =
        CourseIconOption(key, label, category) {
            builds += key
            vector(key)
        }

    private fun vector(name: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).build()
}
