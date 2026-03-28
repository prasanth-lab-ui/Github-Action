package com.saithanyam.wallpaper.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.LocalDate

class BirthdayRepository(context: Context) {

    companion object {
        private const val PREFS_FILE = "saithanyam_prefs"
        private const val KEY_BIRTHDAY = "birthday"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveBirthday(date: LocalDate) {
        prefs.edit().putString(KEY_BIRTHDAY, date.toString()).apply()
    }

    fun getBirthday(): LocalDate? {
        val stored = prefs.getString(KEY_BIRTHDAY, null) ?: return null
        return runCatching { LocalDate.parse(stored) }.getOrNull()
    }
}
