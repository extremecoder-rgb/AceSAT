package com.acesat.education.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("acesat_settings", Context.MODE_PRIVATE)

    // Backend IP for the local Node.js proxy server
    fun getBackendIp(): String? = prefs.getString("backend_ip", "192.168.0.104")
    fun setBackendIp(ip: String) = prefs.edit().putString("backend_ip", ip).apply()
}
