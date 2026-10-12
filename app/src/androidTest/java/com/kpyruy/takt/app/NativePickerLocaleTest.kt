package com.kpyruy.takt.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import java.time.LocalDate
import java.time.format.TextStyle
import org.junit.After
import org.junit.Rule
import org.junit.Test

class NativePickerLocaleTest {
    @get:Rule val compose = createComposeRule()

    @After fun restoreLanguage() = TaktI18n.use(AppLanguage.UKRAINIAN)

    @Test fun dialogMonthFollowsAppLanguageInsteadOfDeviceLanguage() {
        var language by mutableStateOf(AppLanguage.UKRAINIAN)
        val date = LocalDate.now()
        compose.setContent {
            TaktI18n.use(language)
            TaktTheme(AppSettings(language = language)) {
                TaktDatePickerField("Дата", date, {})
            }
        }
        for (choice in listOf(AppLanguage.UKRAINIAN, AppLanguage.ENGLISH, AppLanguage.SLOVAK)) {
            compose.runOnIdle { language = choice }
            compose.onNode(hasText("Дата") and hasClickAction()).performClick()
            val month = date.month.getDisplayName(TextStyle.FULL_STANDALONE, TaktI18n.locale)
            compose.onAllNodesWithText(month, substring = true, ignoreCase = true)
                .onFirst().assertIsDisplayed()
            compose.onNodeWithText(t("Скасувати")).performClick()
        }
    }
}
