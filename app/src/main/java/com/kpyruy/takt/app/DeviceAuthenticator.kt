package com.kpyruy.takt.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import com.kpyruy.takt.core.ui.i18n.t

/** Requests a fresh system challenge; Takt never handles the device PIN or biometric data. */
internal class DeviceAuthenticator(private val activity: FragmentActivity) {
    private var pending: ((DeviceAuthenticationResult) -> Unit)? = null
    val isShowing: Boolean get() = pending != null

    private val credentialLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        finish(if (result.resultCode == Activity.RESULT_OK) DeviceAuthenticationResult.SUCCESS
            else DeviceAuthenticationResult.CANCELLED)
    }

    fun authenticate(title: String, onResult: (DeviceAuthenticationResult) -> Unit) {
        if (pending != null) {
            onResult(DeviceAuthenticationResult.ERROR)
            return
        }
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            onResult(DeviceAuthenticationResult.DEVICE_LOCK_REQUIRED)
            return
        }
        pending = onResult
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            val intent = keyguard.createConfirmDeviceCredentialIntent(title, t("Підтвердь особу для Takt"))
            if (intent == null) finish(DeviceAuthenticationResult.ERROR)
            else credentialLauncher.launch(intent)
            return
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    finish(DeviceAuthenticationResult.SUCCESS)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish(if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED)
                        DeviceAuthenticationResult.CANCELLED else DeviceAuthenticationResult.ERROR)
                }
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(t("Відбиток, обличчя або PIN телефона"))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(promptInfo)
    }

    private fun finish(result: DeviceAuthenticationResult) {
        val callback = pending ?: return
        pending = null
        callback(result)
    }
}
