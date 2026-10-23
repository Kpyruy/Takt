package com.kpyruy.takt.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kpyruy.takt.core.data.BackupMerger
import com.kpyruy.takt.core.data.BackupPayload
import com.kpyruy.takt.core.data.BackupPayloadCodec
import com.kpyruy.takt.core.data.BackupScholarshipPayment
import com.kpyruy.takt.core.data.BackupSection
import com.kpyruy.takt.core.data.BackupStudentFee
import com.kpyruy.takt.core.data.BackupStudentHub
import com.kpyruy.takt.core.data.BackupStudentIdentity
import com.kpyruy.takt.core.data.BackupStudentStudy
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.BackupReviewPage
import com.kpyruy.takt.feature.settings.BackupReviewSource
import com.kpyruy.takt.feature.settings.BackupReviewState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class BackupStudiesReviewUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val study = BackupStudentStudy("study-current", "Automation", true, earnedCredits = 60,
        requiredCredits = 180, updatedAtEpochMillis = Instant.parse("2026-10-20T10:15:00Z").toEpochMilli())
    private val hub = BackupStudentHub(identity = BackupStudentIdentity(name = "Test Student"),
        studies = listOf(study), currentStudyId = study.id, viewedStudyId = study.id, displayName = "Test")

    @Test fun cacheOnlyChangeWithIdenticalNamesAndTitlesHasVisiblePhoneAndFileDetails() {
        val next = study.copy(feesLoaded = true, scholarshipsLoaded = true,
            updatedAtEpochMillis = Instant.parse("2026-10-22T11:30:00Z").toEpochMilli())
        var applied: Set<BackupSection>? = null
        show(hub, hub.copy(studies = listOf(next))) { applied = it }
        expand()
        compose.onNodeWithText("Saved fee records").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Phone: Not loaded")[0].performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("File: Loaded")[0].performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Records last updated").performScrollTo().assertIsDisplayed()
        captureStudiesScreenshot("backup-studies-comparison.png")
        compose.onNodeWithText("Phone: ${timestamp(study.updatedAtEpochMillis!!)}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("File: ${timestamp(next.updatedAtEpochMillis!!)}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("backup-choice-STUDIES").performScrollTo().performClick()
        compose.onNodeWithText("Apply selected data").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(setOf(BackupSection.STUDIES), applied)
    }

    @Test fun creditsProfileContextAndFinanceChangesShowActualValues() {
        val previous = study.copy(id = "study-previous", title = "Previous programme", active = false)
        val phone = hub.copy(studies = listOf(study, previous))
        val next = study.copy(earnedCredits = 66, feesLoaded = true, scholarshipsLoaded = true,
            fees = listOf(BackupStudentFee("fee", study.id, "Tuition", 20000, "EUR", 5000, "PARTIAL",
                dueEpochDay = LocalDate.of(2026, 11, 1).toEpochDay())),
            scholarships = listOf(BackupScholarshipPayment("scholarship", study.id, "Merit award", 15000, "EUR",
                paidEpochDay = LocalDate.of(2026, 10, 20).toEpochDay(), periodLabel = "2026/2027")))
        val file = phone.copy(identity = phone.identity!!.copy(faculty = "Faculty of Engineering"),
            viewedStudyId = previous.id, showPreviousStudies = true, studies = listOf(next, previous.copy(title = "Renamed programme")))
        show(phone, file)
        expand()
        compose.onNodeWithText("File: Faculty of Engineering").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Viewed study").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("File: Renamed programme")[0].performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Earned credits").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Phone: 60").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("File: 66").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Automation · Payments · Tuition").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("File: Partially paid").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Automation · Scholarships · Merit award").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Payment date").performScrollTo().assertIsDisplayed()
        assertEquals(file, BackupMerger.merge(BackupPayload(studentHub = phone), BackupPayload(studentHub = file),
            setOf(BackupSection.STUDIES)).studentHub)
    }

    @Test fun associationOnlyDifferenceHasAnExplanationWithoutExposingIdentifiers() {
        val phone = hub.copy(ownerKey = "private-phone-account-hash")
        val file = hub.copy(ownerKey = "private-file-account-hash")
        show(phone, file)
        expand()
        compose.onNodeWithText("University account association").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Phone: Existing saved record").assertIsDisplayed()
        compose.onNodeWithText("File: Different saved record").assertIsDisplayed()
        compose.onAllNodesWithText("private-phone-account-hash", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("private-file-account-hash", substring = true).assertCountEquals(0)
    }

    private fun show(phone: BackupStudentHub, file: BackupStudentHub, onApply: (Set<BackupSection>) -> Unit = {}) {
        val local = BackupPayload(studentHub = phone)
        val source = BackupPayload(studentHub = file)
        assertEquals(setOf(BackupSection.STUDIES), BackupMerger.changedSections(local, source))
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme(AppSettings(language = AppLanguage.ENGLISH)) {
                BackupReviewPage(BackupReviewState(BackupPayloadCodec.encode(local), BackupPayloadCodec.encode(source),
                    BackupReviewSource.JSON_FILE), onBack = {}, onApply = { onApply(it) })
            }
        } }
    }

    private fun expand() = compose.onNodeWithTag("backup-expand-STUDIES").performScrollTo().performClick()
    private fun timestamp(value: Long) = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.ENGLISH).format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
}
