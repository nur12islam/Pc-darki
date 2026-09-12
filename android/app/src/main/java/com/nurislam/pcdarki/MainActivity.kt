package com.nurislam.pcdarki

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private val security by lazy { SecurityStore(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var unlocked = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            if (unlocked.value) PCDarkiDesktopV2(security)
            else PCDarkiAccountLogin(security, { unlocked.value = true }) { authenticateBiometric { unlocked.value = true } }
        }
    }

    private fun authenticateBiometric(onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return
        BiometricPrompt(this, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock PC-DARKI").setSubtitle("Use your device biometric").setNegativeButtonText("Use PIN").build())
    }
}
