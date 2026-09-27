package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.data.uis.UisFailure
import com.kpyruy.takt.core.data.uis.UisFailureReason
import com.kpyruy.takt.core.data.uis.UisStage
import com.kpyruy.takt.core.ui.i18n.t

internal fun uisFailureMessage(failure: UisFailure): String {
    val stage = t(when (failure.stage) {
        UisStage.LOGIN_FORM -> "Завантаження форми входу"
        UisStage.PREFLIGHT -> "Перевірка даних і другого фактора"
        UisStage.SIGN_IN -> "Вхід і підтвердження сесії"
        UisStage.SECOND_FACTOR -> "Перевірка коду підтвердження"
        UisStage.SESSION_CHECK -> "Перевірка сесії"
    })
    val reason = t(when (failure.reason) {
        UisFailureReason.DNS -> "Не вдалося знайти сервер UIS. Перевір інтернет або DNS."
        UisFailureReason.TIMEOUT -> "UIS не відповів вчасно. Спробуй ще раз."
        UisFailureReason.TLS -> "Не вдалося встановити захищене з’єднання. Перевір дату й час телефона."
        UisFailureReason.CONNECTION -> "З’єднання з UIS перервано або недоступне. Перевір мережу."
        UisFailureReason.HTTP -> "Сервер UIS повернув помилку"
        UisFailureReason.LOGIN_FORM_MISSING -> "У відповіді UIS немає очікуваної форми входу."
        UisFailureReason.RESPONSE_FORMAT -> "UIS повернув відповідь в іншому форматі. Потрібна перевірка інтеграції."
        UisFailureReason.CREDENTIALS_REJECTED -> "UIS відхилив введені дані. Перевір логін, пароль або код підтвердження."
        UisFailureReason.SECOND_FACTOR_CONFIGURATION -> "UIS запросив другий фактор, але не вказав спосіб підтвердження."
        UisFailureReason.SESSION_NOT_CONFIRMED -> "Відповідь UIS не підтвердила активну сесію. Це не обов’язково помилка пароля."
        UisFailureReason.UNSAFE_DESTINATION -> "UIS запропонував неочікувану адресу. Дані туди не надіслано."
        UisFailureReason.TOO_MANY_REDIRECTS -> "UIS повернув забагато перенаправлень."
        UisFailureReason.RESPONSE_TOO_LARGE -> "Відповідь UIS перевищила допустимий розмір."
        UisFailureReason.UNKNOWN -> "Невідома помилка обробки відповіді UIS."
    })
    val status = failure.httpStatus?.let { " (HTTP $it)" }.orEmpty()
    return "$stage: $reason$status"
}
