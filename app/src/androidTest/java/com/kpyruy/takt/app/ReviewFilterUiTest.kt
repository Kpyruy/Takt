package com.kpyruy.takt.app

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ReviewFilterUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val data get()=(compose.activity.application as TaktApplication).dataContainer
 private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
 private fun seed()=runBlocking {
  data.seedLiquidGlassReview()
  data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(themeMode=AppThemeMode.DARK,customWallpaper=null,customThemes=emptyList(),selectedCustomThemeId=null))
  data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
  data.settingsRepository.setHomeWorkFilter(HomeWorkFilter())
  data.awaitRestoredPlanning()
 }
 private fun photo():BackupWallpaper {
  val bitmap=Bitmap.createBitmap(240,360,Bitmap.Config.ARGB_8888)
  val canvas=android.graphics.Canvas(bitmap)
  val paint=android.graphics.Paint().apply { shader=android.graphics.LinearGradient(0f,0f,240f,360f,0xFFEA9258.toInt(),0xFF0B344A.toInt(),android.graphics.Shader.TileMode.CLAMP) }
  canvas.drawRect(0f,0f,240f,360f,paint)
  val out=java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);bitmap.recycle()
  return BackupWallpaper.fromBytes(out.toByteArray(),"Campus sunset")
 }
 @Test fun namedThemeAndWallpaperSurvivePortableBackupAndRejectInvalidPixelsBeforeImport() {
  seed()
  val store=WallpaperStore(compose.activity)
  val photo=photo()
  var imported:String?=null
  try { runBlocking {
   val ref=store.restoreWallpaper(photo,null)
   data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(
    customThemes=listOf(CustomColorTheme("campus","Campus Night","#9B75E1")),selectedCustomThemeId="campus",customWallpaper=ref))
   val raw=data.backupRepository.exportJson()
   val payload=BackupPayloadCodec.decode(raw)
   assertTrue(payload.settings.wallpaperRecorded)
   assertArrayEquals(photo.decodedBytes(),payload.settings.wallpaper!!.decodedBytes())
   assertEquals("Campus Night",payload.settings.customThemes.single().name)
   data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(customWallpaper=null,customThemes=emptyList(),selectedCustomThemeId=null))
   File(File(compose.activity.filesDir,"wallpapers"),ref).delete()
   data.backupRepository.importJson(raw)
   imported=data.settingsRepository.settings.value.customWallpaper
   assertNotNull(imported);assertNotEquals(ref,imported)
   assertEquals("Campus Night",data.settingsRepository.settings.value.selectedCustomTheme!!.name)
   assertArrayEquals(photo.decodedBytes(),store.exportWallpaper(imported)!!.decodedBytes())
   val before=data.backupRepository.exportJson()
   val bad=payload.copy(settings=payload.settings.copy(wallpaper=photo.copy(sha256="0".repeat(64))))
   assertTrue(runCatching { data.backupRepository.importJson(BackupPayloadCodec.encode(bad)) }.isFailure)
   assertEquals(before,data.backupRepository.exportJson())
  } } finally { runBlocking { data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(customWallpaper=null)) };imported?.let { File(File(compose.activity.filesDir,"wallpapers"),it).delete() } }
 }
 @Test fun comparisonHasNamedThemePhotoAndReadableScheduleAndNotes() {
  seed()
  android.os.SystemClock.sleep(8_000) // Let queued test download toasts leave the screenshot.
  val exported=runBlocking { BackupPayloadCodec.decode(data.backupRepository.exportJson()) }
  val titles=mapOf("glass-physics" to "Fyzika", "glass-programming" to "Technické prostriedky automatického riadenia", "glass-math" to "Matematika")
  val local=exported.copy(courses=exported.courses.map { it.copy(title=titles[it.id] ?: it.title) },
    scheduleRules=exported.scheduleRules.map { it.copy(title=titles[it.courseId] ?: it.title) })
  val source=local.copy(
   courses=local.courses.map { if (it.status=="enrolled") it.copy(iconKey=if(it.iconKey=="Computer") "DeveloperBoard" else "Computer") else it },
   scheduleRules=listOf(local.scheduleRules.first().copy(courseId="glass-programming",dayOfWeek=2,startMinute=480,endMinute=590,title="Technické prostriedky automatického riadenia",lessonType="PRACTICE",room="T02 4.108")),
   courseNotes=listOf(local.courseNotes.first().copy(title="Kinematika – stručný prehľad",content="Výpočty a príklady pohybu.")),
   settings=local.settings.copy(customThemes=listOf(BackupCustomColorTheme("campus","Campus Night","#9B75E1","#4477AA","#CC8844")),selectedCustomThemeId="campus",wallpaperRecorded=true,wallpaper=photo()))
  compose.activityRule.scenario.onActivity { activity -> activity.setContent {
   TaktI18n.use(AppLanguage.SLOVAK)
   TaktTheme(AppSettings(language=AppLanguage.SLOVAK,themeMode=AppThemeMode.DARK,cardAppearance=CardAppearance.GLASS)) {
    TaktGlassBackground { BackupReviewPage(BackupReviewState(BackupPayloadCodec.encode(local),BackupPayloadCodec.encode(source),BackupReviewSource.DOCUMENTS),{}, {}) }
   }
  } }
  compose.onNodeWithTag("backup-expand-ICONS").performScrollTo().performClick()
  capture("comparison-icons.png")
  compose.onNodeWithTag("backup-expand-SCHEDULE").performScrollTo().performClick()
  compose.onNodeWithText("08:00–09:50",substring=true).performScrollTo().assertIsDisplayed()
  compose.onNodeWithTag("backup-section-SCHEDULE").performScrollTo()
  capture("comparison-schedule.png")
  compose.onNodeWithTag("backup-expand-MATERIALS").performScrollTo().performClick()
  compose.onNodeWithText("Kinematika – stručný prehľad").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("Výpočty a príklady pohybu.").assertExists()
  compose.onNodeWithTag("backup-section-MATERIALS").performScrollTo()
  capture("comparison-notes.png")
  compose.onNodeWithTag("backup-expand-APPEARANCE").performScrollTo().performClick()
  compose.onAllNodesWithText("Campus Night")[0].performScrollTo().assertIsDisplayed()
  compose.waitUntil(5_000) { compose.onAllNodesWithTag("backup-appearance-file-background").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithTag("backup-section-APPEARANCE").performScrollTo()
  capture("comparison-appearance.png")
  listOf("palette", "material", "background").forEach { kind ->
   compose.onNodeWithTag("backup-appearance-phone-$kind").performScrollTo().assertIsDisplayed()
   compose.onNodeWithTag("backup-appearance-file-$kind").assertIsDisplayed()
   val left=compose.onNodeWithTag("backup-appearance-phone-$kind").fetchSemanticsNode().boundsInRoot
   val right=compose.onNodeWithTag("backup-appearance-file-$kind").fetchSemanticsNode().boundsInRoot
   assertEquals(left.top, right.top, 1f)
   assertEquals(left.height, right.height, 1f)
   if (kind != "palette") capture("comparison-$kind.png")
  }
 }
 @Test fun compactPopupsPersistMultipleCoursesAndAllExclusivelyClearsSpecificChoices() {
  seed()
  compose.activityRule.scenario.onActivity { activity -> activity.setContent {
   val settings by data.settingsRepository.settings.collectAsStateWithLifecycle()
   TaktI18n.use(settings.language)
   TaktTheme(settings) { TaktGlassBackground {
    HomeScreen(data.planningSnapshot,data.studyContentRepository,data.settingsRepository,{}, {},onOpenCourse={},onOpenAssessment={},onEventLongClick={})
   } }
  } }
  compose.onNodeWithTag("home-tasks-filter").performScrollTo().performClick()
  compose.onNodeWithTag("home-filter-courses").performClick()
  compose.onNodeWithTag("home-filter-course-all").assertIsOn()
  compose.onNodeWithTag("home-filter-course-glass-physics").performScrollTo().performClick()
  compose.onNodeWithTag("home-filter-course-glass-programming").performScrollTo().performClick()
  compose.onNodeWithTag("home-filter-course-all").performScrollTo().assertIsOff()
  compose.waitUntil(5_000) { data.settingsRepository.settings.value.homeWorkFilter.courseIds.size==2 }
  assertEquals(2,SharedPreferencesAppSettingsRepository(compose.activity).settings.value.homeWorkFilter.courseIds.size)
  capture("filter-courses-popup.png")
  compose.onNodeWithTag("home-filter-course-all").performClick().assertIsOn()
  compose.onNodeWithTag("home-filter-course-glass-physics").assertIsOff()
  UiDevice.getInstance(instrumentation).pressBack()
  compose.onNodeWithTag("home-filter-types").performClick()
  compose.onNodeWithTag("home-filter-type-TEST").performClick()
  compose.onNodeWithTag("home-filter-type-LAB").performClick()
  compose.onNodeWithTag("home-filter-type-all").performScrollTo().assertIsOff()
  capture("filter-types-popup.png")
  compose.onNodeWithTag("home-filter-type-all").performClick().assertIsOn()
  compose.onNodeWithTag("home-filter-type-TEST").assertIsOff()
  UiDevice.getInstance(instrumentation).pressBack()
  compose.waitUntil(5_000) { data.settingsRepository.settings.value.homeWorkFilter.types.isEmpty() && data.settingsRepository.settings.value.homeWorkFilter.selectedCourseIds.isEmpty() }
  capture("filters-compact.png")
 }
 private fun capture(name:String) {
  compose.waitForIdle();android.os.SystemClock.sleep(120)
  val image=instrumentation.uiAutomation.takeScreenshot()
  compose.activity.openFileOutput(name,0).use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
 }
}
