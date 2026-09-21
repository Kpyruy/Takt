package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class GradeItemMappingTest {
    @Test
    fun gradeItem_roundTripsWithRecordedTimestamp() {
        val model = GradeItem(
            id = "grade-1",
            courseId = "FYZI_6B",
            title = "Test 1",
            type = GradeItemType.TEST,
            earnedPoints = 18.0,
            maxPoints = 20.0,
            recordedAtEpochMillis = 123456789L,
        )

        assertEquals(model, model.toEntity().toDomain())
    }
}
