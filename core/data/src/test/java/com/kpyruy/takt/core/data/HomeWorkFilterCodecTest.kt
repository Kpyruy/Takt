package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.HomeWorkFilter
import com.kpyruy.takt.core.model.HomeWorkPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWorkFilterCodecTest {
    @Test fun fullFilterSurvivesLocalPersistence() {
        val filter = HomeWorkFilter(
            period = HomeWorkPeriod.CUSTOM,
            courseId = "TPAR_6B",
            types = setOf(null, GradeItemType.TEST, GradeItemType.LAB),
            includeCompleted = true,
            includeUndated = false,
            fromDate = LocalDate.of(2026, 10, 1),
            toDate = LocalDate.of(2026, 10, 20),
        )
        assertEquals(filter, HomeWorkFilterCodec.decode(HomeWorkFilterCodec.encode(filter)))
    }

    @Test fun corruptOrMissingSavedFilterUsesFourteenDays() {
        assertEquals(HomeWorkFilter(), HomeWorkFilterCodec.decode(null))
        assertEquals(HomeWorkFilter(), HomeWorkFilterCodec.decode("not-json"))
    }
}
