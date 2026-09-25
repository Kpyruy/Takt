package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AssessmentPhaseMode
import com.kpyruy.takt.core.model.SemesterPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SemesterPeriodsCodecTest {
    @Test fun roundTripKeepsSeparateSemestersAndManualPhase() {
        val periods = mapOf(
            3 to SemesterPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 18),
                LocalDate.of(2027, 1, 11), LocalDate.of(2027, 2, 5)),
            5 to SemesterPeriod(assessmentMode = AssessmentPhaseMode.EXAM),
        )
        assertEquals(periods, SemesterPeriodsCodec.decode(SemesterPeriodsCodec.encode(periods)))
    }

    @Test fun invalidOrOldStoredValuesRemainSafe() {
        assertEquals(emptyMap<Int, SemesterPeriod>(), SemesterPeriodsCodec.decode(null))
        assertEquals(emptyMap<Int, SemesterPeriod>(), SemesterPeriodsCodec.decode("not JSON"))
        assertEquals(emptyMap<Int, SemesterPeriod>(), SemesterPeriodsCodec.decode(
            """[{"semester":3,"studyStart":"2026-12-18","studyEnd":"2026-09-01"}]"""))
    }
}
