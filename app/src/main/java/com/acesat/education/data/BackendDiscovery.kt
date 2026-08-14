package com.acesat.education.data

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

object BackendDiscovery {
    private const val TAG = "BackendDiscovery"
    private const val PORT = 3000
    private const val TIMEOUT_MS = 300

    fun getLocalSubnet(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress == 0) null
            else String.format(
                "%d.%d.%d.",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not get local subnet: ${e.message}")
            null
        }
    }

    suspend fun discoverServer(context: Context): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val subnet = getLocalSubnet(context)
            if (subnet == null) {
                Log.w(TAG, "Could not determine subnet, skipping auto-discovery")
                return@withContext null
            }

            Log.d(TAG, "Scanning subnet: $subnet")

            // Collect all candidate IPs
            val candidates = mutableListOf<String>()
            for (i in 1..254) { candidates.add("$subnet$i") }

            // Parallel scan all IPs with a short timeout
            val found = coroutineScope {
                candidates.map { ip ->
                    async {
                        if (pingServer(ip)) ip else null
                    }
                }.awaitAll().firstOrNull { it != null }
            }

            if (found != null) Log.d(TAG, "Discovered server at: $found")
            else Log.w(TAG, "No server found on subnet $subnet")

            found
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed: ${e.message}")
            null
        }
    }

    private fun pingServer(ip: String): Boolean {
        return try {
            val url = URL("http://$ip:$PORT/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }
    }
}
