package com.kpyruy.takt.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import java.time.LocalDate
import java.time.LocalTime

class DateTimePickerFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingDateFieldOpensDialogAndConfirmReturnsSelectedDate() {
        val initialDate = LocalDate.of(2026, 9, 24)
        var selectedDate: LocalDate? = null
        compose.setContent {
            MaterialTheme {
                TaktDatePickerField(
                    label = "Дата",
                    value = initialDate,
                    onValueChange = { selectedDate = it },
                    modifier = Modifier.testTag("date-field"),
                )
            }
        }

        // Inject a real touch: performClick() bypasses the pointer handling that broke.
        compose.onNodeWithTag("date-field").performTouchInput { click() }
        compose.onNode(isDialog()).assertIsDisplayed()
        compose.runOnIdle { assertNull(selectedDate) }
        compose.onNodeWithText("Готово").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(initialDate, selectedDate) }
    }

    @Test fun tappingEmptyDateFieldOpensDialogAndCancelAllowsReopening() {
        var selectedDate: LocalDate? = null
        compose.setContent {
            MaterialTheme {
                TaktDatePickerField(
                    label = "Дата",
                    value = null,
                    onValueChange = { selectedDate = it },
                    modifier = Modifier.testTag("date-field"),
                )
            }
        }

        compose.onNodeWithTag("date-field").performTouchInput { click() }
        compose.onNode(isDialog()).assertIsDisplayed()
        compose.onNodeWithText("Скасувати").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { assertNull(selectedDate) }
        compose.onNodeWithTag("date-field").performTouchInput { click() }
        compose.onNode(isDialog()).assertIsDisplayed()
    }

    @Test fun tappingTimeFieldOpensDialogAndConfirmReturnsSelectedTime() {
        val initialTime = LocalTime.of(14, 30)
        var selectedTime: LocalTime? = null
        compose.setContent {
            MaterialTheme {
                TaktTimePickerField(
                    label = "Час",
                    value = initialTime,
                    onValueChange = { selectedTime = it },
                    modifier = Modifier.testTag("time-field"),
                )
            }
        }

        compose.onNodeWithTag("time-field").performTouchInput { click() }
        compose.onNode(isDialog()).assertIsDisplayed()
        compose.runOnIdle { assertNull(selectedTime) }
        compose.onNodeWithText("Готово").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(initialTime, selectedTime) }
    }

    @Test fun cancellingTimeDialogKeepsValueAndAllowsReopening() {
        var selectedTime: LocalTime? = null
        compose.setContent {
            MaterialTheme {
                TaktTimePickerField(
                    label = "Час",
                    value = LocalTime.of(8, 45),
                    onValueChange = { selectedTime = it },
                    modifier = Modifier.testTag("time-field"),
                )
            }
        }

        compose.onNodeWithTag("time-field").performTouchInput { click() }
        compose.onNode(isDialog()).assertIsDisplayed()
        compose.onNodeWithText("Скасувати").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { assertNull(selectedTime) }
        compose.onNodeWithTag("time-field").performTouchInput { click() }
        compose.onNode(isDialog()).assertIsDisplayed()
    }
}
