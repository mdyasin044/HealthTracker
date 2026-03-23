package com.example.phonereceiver.nutritionlog

import org.json.JSONObject

object NutritionParser {

    fun parse(responseText: String): NutritionResult {
        val json = extractJson(responseText)
        val obj = JSONObject(json)
        return NutritionResult(
            carbs   = obj.getDouble("carbs"),
            protein = obj.getDouble("protein"),
            fat     = obj.getDouble("fat")
        )
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end   = text.lastIndexOf('}')
        return if (start != -1 && end != -1) text.substring(start, end + 1) else "{}"
    }
}