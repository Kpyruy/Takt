package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.data.UniversityCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Memory-only: neither UIS cookies nor pending two-factor credentials enter backups. */
class UisSession {
    private var client: UisClient? = null
    private val mutableState = MutableStateFlow(UisResult.DISCONNECTED)
    val state = mutableState.asStateFlow()

    suspend fun login(credentials: UniversityCredentials) {
        disconnect()
        val current = UisClient()
        client = current
        execute(current) { current.login(credentials.login, credentials.password) }
    }

    suspend fun submitCode(code: String) {
        val current = client ?: return
        if (mutableState.value != UisResult.SECOND_FACTOR) return
        execute(current) { current.submitCode(code) }
    }

    suspend fun check() {
        val current = client ?: return
        if (mutableState.value != UisResult.CONNECTED) return
        execute(current) { current.checkSession() }
    }

    fun disconnect() {
        client?.close()
        client = null
        mutableState.value = UisResult.DISCONNECTED
    }

    private suspend fun execute(current: UisClient, action: () -> UisResult) {
        mutableState.value = UisResult.CONNECTING
        try {
            val result = withContext(Dispatchers.IO) { action() }
            if (client === current) {
                mutableState.value = result
                if (result !in listOf(UisResult.CONNECTED, UisResult.SECOND_FACTOR)) {
                    current.close()
                    client = null
                }
            }
        } catch (cancelled: CancellationException) {
            if (client === current) disconnect()
            throw cancelled
        }
    }
}
