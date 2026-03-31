package com.example.phonereceiver.nutritionlog

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.phonereceiver.BuildConfig

object GeminiService {

    private val API_KEY = BuildConfig.GEMINI_API_KEY
    private val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$API_KEY"

    suspend fun getNutrition(food: String, amount: String): NutritionResult =
        withContext(Dispatchers.IO) {
            val prompt = """
                Give the macronutrients for $amount of $food.
                Respond ONLY with a JSON object like: {"carbs": 0.0, "protein": 0.0, "fat": 0.0}.
                All values must be in grams. No explanation, no markdown.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }
            Log.d("TAG_HEALTH", "requestBody created")
            val url = URL(ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                outputStream.write(requestBody.toString().toByteArray())
            }
            Log.d("TAG_HEALTH", "connection created")
            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
                throw Exception("Response error - $responseCode: $errorBody")
            }
            val response = conn.inputStream.bufferedReader().readText()
            Log.d("TAG_HEALTH", "Response in JSON: $response")
            val content = JSONObject(response)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            Log.d("TAG_HEALTH", "Response in text: $content")

            NutritionParser.parse(content)
        }
}