package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.data.uis.UisFailure
import com.kpyruy.takt.core.data.uis.UisFailureReason
import com.kpyruy.takt.core.data.uis.UisStage
import com.kpyruy.takt.core.ui.i18n.t

internal fun uisFailureMessage(failure: UisFailure): String {
    val stage = t(when (failure.stage) {
        UisStage.LOGIN_FORM -> "Loading sign-in form"
        UisStage.PREFLIGHT -> "Checking credentials and second factor"
        UisStage.SIGN_IN -> "Sign-in and session confirmation"
        UisStage.SECOND_FACTOR -> "Verification code check"
        UisStage.SESSION_CHECK -> "Session check"
    })
    val reason = t(when (failure.reason) {
        UisFailureReason.DNS -> "UIS server could not be found. Check your internet connection or DNS."
        UisFailureReason.TIMEOUT -> "UIS did not respond in time. Try again."
        UisFailureReason.TLS -> "A secure connection could not be established. Check your phone date and time."
        UisFailureReason.CONNECTION -> "The UIS connection was interrupted or is unavailable. Check your network."
        UisFailureReason.HTTP -> "UIS server returned an error"
        UisFailureReason.LOGIN_FORM_MISSING -> "The UIS response does not contain the expected sign-in form."
        UisFailureReason.RESPONSE_FORMAT -> "UIS returned an unexpected response format. The integration needs checking."
        UisFailureReason.CREDENTIALS_REJECTED -> "UIS rejected the entered details. Check your username, password or verification code."
        UisFailureReason.SECOND_FACTOR_CONFIGURATION -> "UIS requested a second factor without specifying the verification method."
        UisFailureReason.SESSION_NOT_CONFIRMED -> "The UIS response did not confirm an active session. This does not necessarily mean the password is wrong."
        UisFailureReason.UNSAFE_DESTINATION -> "UIS provided an unexpected destination. No credentials were sent there."
        UisFailureReason.TOO_MANY_REDIRECTS -> "UIS returned too many redirects."
        UisFailureReason.RESPONSE_TOO_LARGE -> "The UIS response exceeded the size limit."
        UisFailureReason.UNKNOWN -> "An unknown error occurred while processing the UIS response."
    })
    val status = failure.httpStatus?.let { " (HTTP $it)" }.orEmpty()
    return "$stage: $reason$status"
}
