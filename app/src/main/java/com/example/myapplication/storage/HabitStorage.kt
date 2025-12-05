package com.example.myapplication.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.models.Habit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HabitStorage {

    private const val PREFS_NAME = "habit_prefs"
    private const val KEY_HABITS = "habits"
    private val gson = Gson() // single Gson instance

    fun saveHabits(context: Context, habits: List<Habit>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HABITS, gson.toJson(habits)).apply()
    }

    fun loadHabits(context: Context): MutableList<Habit> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HABITS, null)
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }
}
