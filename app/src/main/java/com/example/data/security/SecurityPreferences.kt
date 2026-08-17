package com.example.data.security

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("docscan_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VAULT_PIN = "vault_pin"
        private const val KEY_VAULT_UNLOCKED = "vault_unlocked"
        private const val KEY_AUTO_ENCRYPT_ALL = "auto_encrypt_all"
    }

    var vaultPin: String?
        get() = prefs.getString(KEY_VAULT_PIN, null)
        set(value) = prefs.edit().putString(KEY_VAULT_PIN, value).apply()

    val isPinSet: Boolean
        get() = !vaultPin.isNullOrEmpty()

    fun verifyPin(pin: String): Boolean {
        return vaultPin != null && vaultPin == pin
    }

    fun clearPin() {
        prefs.edit().remove(KEY_VAULT_PIN).apply()
    }
}
