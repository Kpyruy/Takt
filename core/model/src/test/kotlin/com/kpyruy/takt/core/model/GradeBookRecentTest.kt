package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeBookRecentTest {
    @Test
    fun recentItems_areNewestFirstAndRespectLimit() {
        val items = listOf(
            GradeItem("old", "A", "Old", GradeItemType.TEST, 10.0, 20.0, recordedAtEpochMillis = 100L),
            GradeItem("new", "B", "New", GradeItemType.EXAM, 30.0, 40.0, recordedAtEpochMillis = 300L),
            GradeItem("middle", "C", "Middle", GradeItemType.LAB, 15.0, 20.0, recordedAtEpochMillis = 200L),
        )

        assertEquals(
            listOf("new", "middle"),
            GradeBook.recent(items, limit = 2).map { it.id },
        )
    }
}
