package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.StudyTask
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyContentMappingTest {
    @Test
    fun task_roundTripsThroughEntity() {
        val model = StudyTask(
            id = "task-1",
            courseId = "FYZI_6B",
            title = "Lab report",
            description = "Finish graphs",
            dueDate = LocalDate.of(2026, 9, 25),
            completed = false,
        )

        assertEquals(model, model.toEntity().toDomain())
    }

    @Test
    fun taskWithoutDeadline_roundTripsThroughEntity() {
        val model = StudyTask(
            id = "task-2",
            courseId = "ZAST_6B",
            title = "Read chapter",
            description = null,
            dueDate = null,
            completed = true,
        )

        assertEquals(model, model.toEntity().toDomain())
    }

    @Test
    fun note_roundTripsThroughEntity() {
        val model = CourseNote(
            id = "note-1",
            courseId = "TPAR_6B",
            title = "PLC notes",
            content = "Remember signal types.",
            updatedAtEpochMillis = 123456789L,
        )

        assertEquals(model, model.toEntity().toDomain())
    }
}
