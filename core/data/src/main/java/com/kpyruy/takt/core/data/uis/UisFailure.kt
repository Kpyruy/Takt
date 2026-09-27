package com.kpyruy.takt.core.data.uis

/** Allowlisted diagnostic metadata only. Never includes URLs, response bodies or secrets. */
data class UisFailure(val stage: UisStage, val reason: UisFailureReason, val httpStatus: Int? = null)
enum class UisStage { LOGIN_FORM, PREFLIGHT, SIGN_IN, SECOND_FACTOR, SESSION_CHECK }
enum class UisFailureReason {
    DNS, TIMEOUT, TLS, CONNECTION, HTTP, LOGIN_FORM_MISSING, RESPONSE_FORMAT,
    CREDENTIALS_REJECTED, SECOND_FACTOR_CONFIGURATION, SESSION_NOT_CONFIRMED,
    UNSAFE_DESTINATION, TOO_MANY_REDIRECTS, RESPONSE_TOO_LARGE, UNKNOWN,
}
