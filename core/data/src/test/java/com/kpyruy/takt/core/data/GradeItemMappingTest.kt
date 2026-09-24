package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GradeItemMappingTest {
    @Test
    fun gradeItem_roundTripsAcademicProgressFields() {
        val model = GradeItem(
            id = "grade-1",
            courseId = "FYZI_6B",
            title = "Test 1",
            type = GradeItemType.TEST,
            earnedPoints = 18.0,
            maxPoints = 20.0,
            recordedAtEpochMillis = 123456789L,
            dueDate = LocalDate.of(2026, 10, 1),
            completed = false,
            requiredForExam = true,
            minimumPointsForExam = 12.0,
            lessonId = "physics-friday-rule",
        )

        assertEquals(model, model.toEntity().toDomain())
    }
}
