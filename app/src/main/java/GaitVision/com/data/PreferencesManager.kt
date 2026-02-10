package GaitVision.com.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("GaitVisionPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_USER_NAME = "user_name"
    }

    var language: String
        get() = sharedPreferences.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) = sharedPreferences.edit().putString(KEY_LANGUAGE, value).apply()

    var theme: String
        get() = sharedPreferences.getString(KEY_THEME, "Dark") ?: "Dark"
        set(value) = sharedPreferences.edit().putString(KEY_THEME, value).apply()

    var userName: String
        get() = sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_USER_NAME, value).apply()
}
