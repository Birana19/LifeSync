package com.example.myapplication

import android.os.Bundle
import android.text.InputType
import android.content.Intent
import android.content.Context
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapters.HabitAdapter
import com.example.myapplication.models.Habit
import com.example.myapplication.storage.HabitStorage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator

class HabitsActivity : AppCompatActivity() {

    private lateinit var adapter: HabitAdapter
    private lateinit var habitList: MutableList<Habit>
    private lateinit var circularProgressBar: CircularProgressIndicator
    private lateinit var progressPercentage: TextView
    private lateinit var streakCountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.habits_page)

        circularProgressBar = findViewById(R.id.circularProgressBar)
        progressPercentage = findViewById(R.id.progressPercentage)
        streakCountText = findViewById(R.id.streak_count)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_habits -> true
                R.id.nav_mood -> {
                    startActivity(Intent(this, MoodActivity::class.java))
                    true
                }
                R.id.nav_hydration -> {
                    startActivity(Intent(this, HydrationActivity::class.java))
                    true
                }
                R.id.nav_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        habitList = HabitStorage.loadHabits(this)

        if (habitList.isEmpty()) {
            habitList.add(Habit(name = "Drink 8 glasses of water", currentProgress = 0, target = 8, streak = 7))
            habitList.add(Habit(name = "Walk 10,000 steps", currentProgress = 0, target = 10000, streak = 12))
            habitList.add(Habit(name = "Read for 30 minutes", currentProgress = 0, target = 30, streak = 5))
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HabitAdapter(
            onCheckChanged = { habit, checked ->
                val idx = habitList.indexOfFirst { it.id == habit.id }
                if (idx >= 0) {
                    val h = habitList[idx]
                    if (checked && !h.isCompleted) {
                        h.isCompleted = true
                        h.currentProgress = h.target
                        h.streak += 1
                    } else if (!checked && h.isCompleted) {
                        h.isCompleted = false
                        h.currentProgress = 0
                    }
                    saveAndRefresh()
                }
            },
            onEdit = { habit -> showEditHabitDialog(habit) },
            onDelete = { habit -> showDeleteConfirmation(habit) }
        )

        recyclerView.adapter = adapter
        adapter.submitList(habitList.toList())

        val tvDate = findViewById<TextView>(R.id.date)
        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        tvDate.text = today

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener { showAddHabitDialog() }

        updateProgress()
    }

    private fun saveAndRefresh() {
        HabitStorage.saveHabits(this, habitList)
        adapter.submitList(habitList.toList())
        updateProgress()
    }


    private fun updateProgress() {
        val totalHabits = habitList.size
        val completedHabits = habitList.count { it.isCompleted }

        val progressPercent = if (totalHabits == 0) 0 else (completedHabits * 100) / totalHabits
        circularProgressBar.progress = progressPercent
        progressPercentage.text = "$progressPercent%"

        val highestStreak = habitList.maxOfOrNull { it.streak } ?: 0
        streakCountText.text = "$highestStreak Streaks"



        val sharedPref = getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("habitCount", totalHabits)
            putInt("streak", highestStreak)
            putFloat("successPercentage", progressPercent.toFloat())
            apply()
        }



        val weeklyPrefs = getSharedPreferences("WeeklyData", Context.MODE_PRIVATE)
        val editor = weeklyPrefs.edit()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        editor.putInt("progress_$todayDate", progressPercent)
        editor.apply()
    }


    private fun showAddHabitDialog() {
        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val nameInput = EditText(ctx).apply {
            hint = "Habit name"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val targetInput = EditText(ctx).apply {
            hint = "Target (number)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(nameInput)
        layout.addView(targetInput)

        AlertDialog.Builder(ctx)
            .setTitle("Add habit")
            .setView(layout)
            .setPositiveButton("Add") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                if (name.isNotEmpty()) {
                    val habit = Habit(name = name, currentProgress = 0, target = target, streak = 0, isCompleted = false)
                    habitList.add(habit)
                    saveAndRefresh()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditHabitDialog(habit: Habit) {
        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }
        val nameInput = EditText(ctx).apply {
            hint = "Habit name"
            setText(habit.name)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val targetInput = EditText(ctx).apply {
            hint = "Target (number)"
            setText(habit.target.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(nameInput)
        layout.addView(targetInput)

        AlertDialog.Builder(ctx)
            .setTitle("Edit habit")
            .setView(layout)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = nameInput.text.toString().trim()
                val newTarget = targetInput.text.toString().toIntOrNull() ?: habit.target
                if (newName.isNotEmpty()) {
                    val idx = habitList.indexOfFirst { it.id == habit.id }
                    if (idx >= 0) {
                        habitList[idx].name = newName
                        habitList[idx].target = newTarget
                        if (habitList[idx].currentProgress > newTarget) {
                            habitList[idx].currentProgress = newTarget
                        }
                        saveAndRefresh()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(habit: Habit) {
        AlertDialog.Builder(this)
            .setTitle("Delete habit")
            .setMessage("Delete \"${habit.name}\"?")
            .setPositiveButton("Delete") { dialog, _ ->
                val removed = habitList.removeAll { it.id == habit.id }
                if (removed) saveAndRefresh()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        HabitStorage.saveHabits(this, adapter.currentList.toMutableList())
    }
}
