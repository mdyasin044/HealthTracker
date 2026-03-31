package com.example.phonereceiver.nutritionlog

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.phonereceiver.R
import kotlinx.coroutines.launch

class LogMealViewController(private val activity: AppCompatActivity) {

    private val etFood: EditText       = activity.findViewById(R.id.etFood)
    private val etAmount: EditText     = activity.findViewById(R.id.etAmount)
    private val btnLogMeal: Button     = activity.findViewById(R.id.btnLogMeal)
    private val progressBar: ProgressBar = activity.findViewById(R.id.progressBar)
    private val tvResult: TextView     = activity.findViewById(R.id.tvNutritionAmount)

    init {
        btnLogMeal.setOnClickListener { analyze() }
    }

    private fun analyze() {
        val food   = etFood.text.toString().trim()
        val amount = etAmount.text.toString().trim()

        if (food.isEmpty() || amount.isEmpty()) {
            Toast.makeText(activity, "Please enter food and amount", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        activity.lifecycleScope.launch {
            try {
                val result = GeminiService.getNutrition(food, amount)
                tvResult.text = "Carbs: ${result.carbs}g, Protein: ${result.protein}g, Fat: ${result.fat}g"
                tvResult.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(activity, "Something went wrong", Toast.LENGTH_LONG).show()
                Log.e("TAG_HEALTH", "Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogMeal.isEnabled   = !loading
        tvResult.visibility    = if (!loading) View.VISIBLE else View.GONE
    }
}