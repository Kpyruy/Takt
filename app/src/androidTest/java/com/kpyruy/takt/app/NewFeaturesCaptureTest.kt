package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.settings.SettingsScreen
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class NewFeaturesCaptureTest {
    @get:Rule val compose=createComposeRule()
    @Test fun captureWrittenAdmissionOnlyTestAndCurrentGitHubRelease() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("newFeatureShots")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val app=context.applicationContext as TaktApplication
        val data=app.dataContainer
        data.database.clearAllTables()
        runBlocking {
            data.database.courseDao().insertAll(listOf(CourseEntity("new-feature","FYZI_6B","Фізика",5,3,"enrolled","COMPULSORY",null,gradingType="EXAM_LETTER")))
            data.gradeRepository.upsertItem(GradeItem("gate","new-feature","Допусковий тест",GradeItemType.TEST,0.0,20.0,
                completed=false,dueDate=LocalDate.now().minusDays(2),requiredForExam=true,minimumPointsForExam=10.0,
                durationMinutes=45,contributesToGrade=false,awaitingResult=true))
            data.gradeRepository.upsertItem(GradeItem("lab","new-feature","Лабораторна робота",GradeItemType.LAB,20.0,20.0))
            data.gradeRepository.upsertItem(GradeItem("exam","new-feature","Підсумковий екзамен",GradeItemType.EXAM,0.0,100.0,completed=false,dueDate=LocalDate.now().plusMonths(2)))
        }
        // This current public release was verified via GitHub API; captures use deterministic metadata.
        val store=object:AppReleasePreferenceStore {
            var value=AppReleasePreferences(latest=AppRelease("v0.2.0","https://github.com/Kpyruy/Takt/releases/tag/v0.2.0",
                "https://github.com/Kpyruy/Takt/releases/download/v0.2.0/Takt-v0.2.0.apk"),lastSuccessfulCheck=System.currentTimeMillis())
            override fun read()=value
            override fun write(value:AppReleasePreferences){this.value=value}
        }
        val checker=AppReleaseRepository("0.2.0",store,{store.value.latest!!})
        var screen by mutableStateOf("tasks")
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme(AppSettings(themeMode=AppThemeMode.LIGHT)) {
                val activity=checkNotNull(LocalActivity.current);val view=LocalView.current
                SideEffect { androidx.core.view.WindowCompat.getInsetsController(activity.window,view).apply {
                    isAppearanceLightStatusBars=true;isAppearanceLightNavigationBars=true
                } }
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
                        key(screen) {
                            if(screen=="updates") SettingsScreen(data.settingsRepository,data.backupRepository,data.documentStore,
                                data.studyPlanRepository,data.universityAccountRepository,authenticateDevice={_,cb->cb(DeviceAuthenticationResult.CANCELLED)},
                                appReleaseRepository=checker,onBack={})
                            else SubjectDetailScreen(data.studyPlanRepository,data.gradeRepository,data.scheduleRepository,
                                data.settingsRepository,data.studyContentRepository,data.examRepository,data.documentStore,
                                courseId="new-feature",onBack={},initialTab=screen)
                        }
                    }
                }
            }
        }
        for(name in listOf("tasks","exam","updates")) {
            compose.runOnIdle { screen=name };compose.waitForIdle()
            if(name=="updates") compose.onNodeWithText("Оновлення додатка").performScrollTo().performClick()
            else compose.waitUntil(10_000) { compose.onAllNodesWithText(if(name=="tasks") "Очікую оцінку" else "Цільова оцінка").fetchSemanticsNodes().isNotEmpty() }
            compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(350)
            val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            context.openFileOutput("new-feature-$name.png",0).use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        }
    }
}
