package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamMappingTest {
    @Test
    fun examInfo_roundTripsThroughEntity() {
        val model = ExamInfo(
            courseId = "FYZI_6B",
            gradeItemId = "exam-grade",
            date = LocalDate.of(2027, 1, 20),
            startTime = LocalTime.of(9, 30),
            endTime = LocalTime.of(11, 0),
            room = "T-068",
            attemptNumber = 2,
            maxAttempts = 3,
            notes = "Bring calculator",
        )

        assertEquals(model, model.toEntity().toDomain())
    }

    @Test
    fun examMaterial_roundTripsThroughEntity() {
        val model = ExamMaterial(
            id = "material-1",
            courseId = "FYZI_6B",
            title = "Vzorce",
            uri = "content://example/formulas",
        )

        assertEquals(model, model.toEntity().toDomain())
    }
}
