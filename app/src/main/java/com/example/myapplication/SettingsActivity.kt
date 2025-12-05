package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_page)


        val sharedPref = getSharedPreferences("HabitPrefs", MODE_PRIVATE)
        val habitCount = sharedPref.getInt("habitCount", 0)
        val streak = sharedPref.getInt("streak", 0)
        val successPercentage = sharedPref.getFloat("successPercentage", 0f)


        val habitCountText = findViewById<TextView>(R.id.habit_count)
        val streakText = findViewById<TextView>(R.id.streak)
        val successText = findViewById<TextView>(R.id.success)


        habitCountText.text = "$habitCount"
        streakText.text = "$streak"
        successText.text = "${successPercentage.toInt()}%"

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener{ item ->
            when (item.itemId){
                R.id.nav_habits->{
                    startActivity(Intent(this, HabitsActivity::class.java))
                    true
                }
                R.id.nav_mood->{
                    startActivity(Intent(this,MoodActivity::class.java))
                    true
                }
                R.id.nav_hydration->{
                    startActivity(Intent(this,HydrationActivity::class.java))
                    true
                }
                R.id.nav_analytics->{
                    startActivity(Intent(this,AnalyticsActivity::class.java))
                    true
                }
                R.id.nav_settings-> true

                else -> false
            }
    }

}}