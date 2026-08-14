package com.acesat.education.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("acesat_settings", Context.MODE_PRIVATE)

    // Backend IP for the production Node.js proxy server
    fun getBackendIp(): String? = prefs.getString("backend_ip", "https://ace-sat.vercel.app/")
    fun setBackendIp(ip: String) = prefs.edit().putString("backend_ip", ip).apply()
}
