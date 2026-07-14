package com.example.data.config

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object RemoteConfigManager {
    
    data class RemoteConfig(
        val latestVersion: String,
        val forceUpdate: Boolean,
        val message: String,
        val appStatus: String
    )

    suspend fun fetchConfig(): RemoteConfig? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://journal.xo.je/wp-json/tradejrnl/v1/config")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                val latestVersion = json.optString("latest_version", "1.0.0")
                val forceUpdate = json.optBoolean("force_update", false)
                val message = json.optString("message", "")
                val appStatus = json.optString("app_status", "active")

                RemoteConfig(
                    latestVersion = latestVersion,
                    forceUpdate = forceUpdate,
                    message = message,
                    appStatus = appStatus
                )
            } else {
                Log.e("RemoteConfigManager", "Server returned HTTP ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("RemoteConfigManager", "Failed to fetch remote config", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Helper to compare version strings (e.g., "1.0.1" and "1.0.0").
     * Returns true if the version strings differ in major, minor, or patch numbers.
     */
    fun isVersionDifferent(v1: String, v2: String): Boolean {
        val clean1 = v1.replace(Regex("[^0-9.]"), "").split(".")
        val clean2 = v2.replace(Regex("[^0-9.]"), "").split(".")
        val maxLen = maxOf(clean1.size, clean2.size)
        for (i in 0 until maxLen) {
            val num1 = clean1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = clean2.getOrNull(i)?.toIntOrNull() ?: 0
            if (num1 != num2) {
                return true
            }
        }
        return false
    }
}
