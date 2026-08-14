package com.acesat.education.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("acesat_settings", Context.MODE_PRIVATE)

    fun getApiKey(): String? {
        return prefs.getString("nvidia_api_key", null)
    }

    fun setApiKey(key: String) {
        prefs.edit().putString("nvidia_api_key", key).apply()
    }
}
