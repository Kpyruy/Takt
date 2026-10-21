package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.data.uis.UisFailure
import com.kpyruy.takt.core.data.uis.UisFailureReason
import com.kpyruy.takt.core.data.uis.UisStage
import com.kpyruy.takt.core.ui.i18n.t

internal fun uisFailureMessage(failure: UisFailure): String {
    val stage = t(when (failure.stage) {
        UisStage.LOGIN_FORM -> UiKey.TEXT_LOADING_SIGN_IN_FORM
        UisStage.PREFLIGHT -> UiKey.TEXT_CHECKING_CREDENTIALS_AND_SECOND_FACTOR
        UisStage.SIGN_IN -> UiKey.TEXT_SIGN_IN_AND_SESSION_CONFIRMATION
        UisStage.SECOND_FACTOR -> UiKey.TEXT_VERIFICATION_CODE_CHECK
        UisStage.SESSION_CHECK -> UiKey.TEXT_SESSION_CHECK
    })
    val reason = t(when (failure.reason) {
        UisFailureReason.DNS -> UiKey.TEXT_UIS_SERVER_COULD_NOT_BE_FOUND_CHECK_YOUR_INTERNET_CONNECTION_OR_DNS
        UisFailureReason.TIMEOUT -> UiKey.TEXT_UIS_DID_NOT_RESPOND_IN_TIME_TRY_AGAIN
        UisFailureReason.TLS -> UiKey.TEXT_A_SECURE_CONNECTION_COULD_NOT_BE_ESTABLISHED_CHECK_YOUR_PHONE_F1216254
        UisFailureReason.CONNECTION -> UiKey.TEXT_THE_UIS_CONNECTION_WAS_INTERRUPTED_OR_IS_UNAVAILABLE_CHECK_YOUR_NETWORK
        UisFailureReason.HTTP -> UiKey.TEXT_UIS_SERVER_RETURNED_AN_ERROR
        UisFailureReason.LOGIN_FORM_MISSING -> UiKey.TEXT_THE_UIS_RESPONSE_DOES_NOT_CONTAIN_THE_EXPECTED_SIGN_IN_FORM
        UisFailureReason.RESPONSE_FORMAT -> UiKey.TEXT_UIS_RETURNED_AN_UNEXPECTED_RESPONSE_FORMAT_THE_INTEGRATION_NEE_DA0D4F40
        UisFailureReason.CREDENTIALS_REJECTED -> UiKey.TEXT_UIS_REJECTED_THE_ENTERED_DETAILS_CHECK_YOUR_USERNAME_PASSWORD_943AF4CB
        UisFailureReason.SECOND_FACTOR_CONFIGURATION -> UiKey.TEXT_UIS_REQUESTED_A_SECOND_FACTOR_WITHOUT_SPECIFYING_THE_VERIFICATION_METHOD
        UisFailureReason.SESSION_NOT_CONFIRMED -> UiKey.TEXT_THE_UIS_RESPONSE_DID_NOT_CONFIRM_AN_ACTIVE_SESSION_THIS_DOES_N_A5C79319
        UisFailureReason.UNSAFE_DESTINATION -> UiKey.TEXT_UIS_PROVIDED_AN_UNEXPECTED_DESTINATION_NO_CREDENTIALS_WERE_SENT_THERE
        UisFailureReason.TOO_MANY_REDIRECTS -> UiKey.TEXT_UIS_RETURNED_TOO_MANY_REDIRECTS
        UisFailureReason.RESPONSE_TOO_LARGE -> UiKey.TEXT_THE_UIS_RESPONSE_EXCEEDED_THE_SIZE_LIMIT
        UisFailureReason.UNKNOWN -> UiKey.TEXT_AN_UNKNOWN_ERROR_OCCURRED_WHILE_PROCESSING_THE_UIS_RESPONSE
    })
    val status = failure.httpStatus?.let { " (HTTP $it)" }.orEmpty()
    return "$stage: $reason$status"
}
