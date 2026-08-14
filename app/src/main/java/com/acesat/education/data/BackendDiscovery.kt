package com.acesat.education.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object BackendDiscovery {
    private const val TAG = "BackendDiscovery"

    fun getLocalSubnet(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo
        val ipAddress = connectionInfo.ipAddress
        if (ipAddress == 0) return null
        
        val ipString = String.format(
            "%d.%d.%d.",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff
        )
        return ipString
    }

    suspend fun discoverServer(context: Context): String? = withContext(Dispatchers.IO) {
        val subnet = getLocalSubnet(context) ?: "192.168.1."
        Log.d(TAG, "Scanning subnet: $subnet")
        
        // Also check standard emulators localhost redirect (10.0.2.2)
        val candidateIPs = mutableListOf("10.0.2.2")
        for (i in 1..254) {
            candidateIPs.add("$subnet$i")
        }

        val deferredResults = candidateIPs.map { ip ->
            async {
                val urlString = "http://$ip:3000/health"
                try {
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 250 // Fast connection timeout
                    conn.readTimeout = 250
                    conn.requestMethod = "GET"
                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        if (responseText.contains("status") && responseText.contains("OK")) {
                            Log.d(TAG, "Found active server at: $ip")
                            return@async ip
                        }
                    }
                } catch (e: Exception) {
                    // Ignore failures
                }
                null
            }
        }

        // Wait for first non-null result, or complete with null
        for (deferred in deferredResults) {
            val res = deferred.await()
            if (res != null) {
                // Cancel all other active scan jobs
                deferredResults.forEach { it.cancel() }
                return@withContext res
            }
        }
        null
    }
}
