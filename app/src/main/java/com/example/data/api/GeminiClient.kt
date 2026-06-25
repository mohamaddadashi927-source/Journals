package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeChart(bitmap: Bitmap, userPrompt: String = "Please analyze this trading chart. Give key supports, resistances, candlestick patterns, and trade execution quality."): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "خطا: کلید API معتبر یافت نشد. لطفاً در تنظیمات یا پنل Secrets کلید خود را وارد کنید."
        }

        val url = "$BASE_URL/gemini-3.1-pro-preview:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", bitmap.toBase64())
                            })
                        })
                    })
                })
            })
        }

        val body = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Analyze Chart Failed: ${response.code} $errBody")
                return "خطا در ارتباط با جمینی: ${response.code}"
            }
            val respStr = response.body?.string() ?: ""
            parseTextResponse(respStr)
        } catch (e: Exception) {
            Log.e(TAG, "Analyze Chart Error", e)
            "خطا در تحلیل تصویر: ${e.localizedMessage}"
        }
    }

    suspend fun transcribeAudio(audioBytes: ByteArray, prompt: String = "Please transcribe this trading voice note into Persian text. Keep it literal and professional."): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "خطا: کلید API معتبر یافت نشد. لطفاً در تنظیمات یا پنل Secrets کلید خود را وارد کنید."
        }

        val url = "$BASE_URL/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/wav")
                                put("data", base64Audio)
                            })
                        })
                    })
                })
            })
        }

        val body = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Transcribe Audio Failed: ${response.code} $errBody")
                return "خطا در تحلیل صوت: ${response.code}"
            }
            val respStr = response.body?.string() ?: ""
            parseTextResponse(respStr)
        } catch (e: Exception) {
            Log.e(TAG, "Transcribe Audio Error", e)
            "خطا در تحلیل صوت: ${e.localizedMessage}"
        }
    }

    suspend fun generateChartMockup(prompt: String, aspectRatio: String): Bitmap? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val url = "$BASE_URL/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", aspectRatio)
                    put("imageSize", "1K")
                })
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                })
            })
        }

        val body = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Generate Image Failed: ${response.code} $errBody")
                return null
            }
            val respStr = response.body?.string() ?: ""
            parseImageResponse(respStr)
        } catch (e: Exception) {
            Log.e(TAG, "Generate Image Error", e)
            null
        }
    }

    private fun parseTextResponse(responseStr: String): String {
        return try {
            val root = JSONObject(responseStr)
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Parse text response error", e)
            "خطا در استخراج متن پاسخ"
        }
    }

    private fun parseImageResponse(responseStr: String): Bitmap? {
        return try {
            val root = JSONObject(responseStr)
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("inlineData")) {
                    val inlineData = part.getJSONObject("inlineData")
                    val base64Data = inlineData.getString("data")
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Parse image response error", e)
            null
        }
    }
}
