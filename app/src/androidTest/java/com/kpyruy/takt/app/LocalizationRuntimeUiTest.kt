package com.kpyruy.takt.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.feature.subjects.AddTaskForm
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocalizationRuntimeUiTest {
    @get:Rule val compose = createComposeRule()
    @After fun resetLanguage() { TaktI18n.use(AppLanguage.UKRAINIAN) }

    @Test fun savedValidationAndMaximumLabelFollowLanguageWhileUserTitleStaysIntact() {
        TaktI18n.use(AppLanguage.ENGLISH)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme {
            AddTaskForm("course", modifier = Modifier.verticalScroll(rememberScrollState()), onSave = {})
        } }
        compose.onNodeWithTag("task-title").performTextInput("My own homework")
        compose.onNodeWithTag("task-maximum").assertDoesNotExist()
        click("grade-details-trigger")
        click("grade-details-tab-RESULT")
        compose.onNodeWithTag("task-maximum").performTextInput("-1")
        click("grade-details-done")
        click("task-save")
        restoration.emulateSavedInstanceStateRestore()
        for (language in AppLanguage.entries) {
            compose.runOnIdle { TaktI18n.use(language) }
            compose.onNodeWithTag("task-maximum")
                .assertTextContains(TaktI18n.text(UiKey.WORK_MAXIMUM_POINTS, language))
                .assertTextContains("-1")
            compose.onNodeWithTag("grade-details-error")
                .assertTextContains(TaktI18n.text(UiKey.TEXT_MAXIMUM_MUST_BE_GREATER_THAN_0, language))
        }
        click("grade-details-done")
        compose.onNodeWithTag("task-title").assertTextContains("My own homework")
    }

    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        compose.waitForIdle()
    }
}
