package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.models.Habit
import com.example.myapplication.models.MoodData
import com.example.myapplication.storage.HabitStorage
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private val emojiMap = mapOf(
        R.drawable.happy_emoji to "😊",
        R.drawable.cry_emoji to "😢",
        R.drawable.party_emoji to "🥳",
        R.drawable.love_emoji to "😍",
        R.drawable.laugh_emoji to "😂",
        R.drawable.cool_emoji to "😎"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analytics_page)

        updateStatsCards()
        setupMoodBarChart()
        setupHabitPieChart()
        setupBottomNav()
    }


    private fun setupMoodBarChart() {
        val weeklyMood = getWeeklyMoodData()

        val entries = weeklyMood.entries.mapIndexed { index, entry ->
            val value = if (entry.value.isEmpty()) 0.3f else 1f
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "Mood Trend")
        dataSet.colors = weeklyMood.entries.map {
            if (it.value.isEmpty()) resources.getColor(R.color.light_pink)
            else resources.getColor(R.color.blueish_green)
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        val chart = findViewById<BarChart>(R.id.moodTrendChart)
        chart.data = barData

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt().coerceIn(0, weeklyMood.size - 1)
                val day = weeklyMood.keys.toList()[index]
                val emoji = weeklyMood[day] ?: ""
                return "$day\n$emoji"
            }
        }

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.invalidate()
    }


    private fun setupHabitPieChart() {
        val pieChart = findViewById<PieChart>(R.id.habitPieChart)
        val weeklyPrefs = getSharedPreferences("WeeklyData", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        var totalProgress = 0
        var totalGoal = 0

        for (i in 0..6) {
            val dateStr = sdf.format(calendar.time)
            val progress = weeklyPrefs.getInt("progress_$dateStr", -1)
            if (progress != -1) {
                totalProgress += progress
                totalGoal += 100 // assuming max 100% per day
            } else {
                totalGoal += 100
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val remaining = (totalGoal - totalProgress).coerceAtLeast(0)

        val entries = listOf(
            PieEntry(totalProgress.toFloat(), "Completed"),
            PieEntry(remaining.toFloat(), "Remaining")
        )

        val dataSet = PieDataSet(entries, "Weekly Performance")
        dataSet.colors = listOf(resources.getColor(R.color.blueish_green), resources.getColor(R.color.light_pink))
        dataSet.valueTextColor = resources.getColor(R.color.black)
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = false
        pieChart.setEntryLabelColor(resources.getColor(R.color.black))
        pieChart.setEntryLabelTextSize(12f)
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }


    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_habits -> { startActivity(Intent(this, HabitsActivity::class.java)); true }
                R.id.nav_mood -> { startActivity(Intent(this, MoodActivity::class.java)); true }
                R.id.nav_hydration -> { startActivity(Intent(this, HydrationActivity::class.java)); true }
                R.id.nav_analytics -> true
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }
    }


    private fun getWeeklyMoodData(): Map<String, String> {
        val sharedPref = getSharedPreferences("mood_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val calendar = Calendar.getInstance()
        val weeklyData = mutableMapOf<String, String>()

        for (i in 0..6) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val json = sharedPref.getString(dateStr, null)
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)

            val emoji = if (json != null) {
                val moodData = gson.fromJson(json, MoodData::class.java)
                moodData.emojiResId?.let { emojiMap[it] } ?: ""
            } else ""

            weeklyData[dayLabel] = emoji
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return weeklyData.entries.reversed().associate { it.toPair() }
    }


    private fun updateStatsCards() {
        val habits = HabitStorage.loadHabits(this)
        val weeklyPrefs = getSharedPreferences("WeeklyData", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        var totalProgress = 0
        var count = 0
        for (i in 0..6) {
            val dateStr = sdf.format(calendar.time)
            val progress = weeklyPrefs.getInt("progress_$dateStr", -1)
            if (progress != -1) {
                totalProgress += progress
                count++
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        val averageWeeklyProgress = if (count > 0) totalProgress / count else 0
        findViewById<TextView>(R.id.complete).text = "Weekly Progress : $averageWeeklyProgress%"

        val highestStreak = habits.maxOfOrNull { it.streak } ?: 0
        findViewById<TextView>(R.id.current_streak).text = "Streaks : $highestStreak Streaks"

        val completedCount = habits.count { it.isCompleted }
        findViewById<TextView>(R.id.habit_complete).text = "Habits Completed : $completedCount"
    }

    override fun onResume() {
        super.onResume()
        updateStatsCards()
        setupHabitPieChart()
        setupMoodBarChart()
    }
}
