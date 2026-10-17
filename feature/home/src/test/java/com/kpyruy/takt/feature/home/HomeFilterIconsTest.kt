package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.GradeItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFilterIconsTest {
    @Test fun visibleWorkTypesHaveDistinctSymbolsAndLegacyTestKeepsItsAlias() {
        val types = listOf(null) + GradeItemType.entries.filterNot {
            it == GradeItemType.EXAM || it == GradeItemType.MIDTERM
        }
        val symbols = types.map { homeFilterTaskIcon(it).name }
        assertEquals(types.size, symbols.toSet().size)
        assertEquals(homeFilterTaskIcon(GradeItemType.TEST), homeFilterTaskIcon(GradeItemType.MIDTERM))
    }
}
