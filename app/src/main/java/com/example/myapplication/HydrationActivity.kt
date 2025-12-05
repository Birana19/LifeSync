package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class HydrationActivity : AppCompatActivity() {

    enum class CupSize(val ml: Int) {
        HALF(125), FULL(250), LARGE(500)
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var percentageText: TextView
    private lateinit var cups: MutableList<ImageView>
    private val totalCups = 8
    private lateinit var sharedPref: android.content.SharedPreferences
    private var cupSizes: MutableList<CupSize?> = MutableList(totalCups) { null }
    private var customCups: MutableList<Pair<String, Int>> = mutableListOf()

    private lateinit var plusBtn: Button
    private lateinit var minusBtn: Button
    private lateinit var halfBtn: ImageView
    private lateinit var fullBtn: ImageView
    private lateinit var largeBtn: ImageView

    private lateinit var barChart: BarChart
    private lateinit var reminderSwitch: Switch
    private lateinit var capsule1: ImageView
    private lateinit var capsule2: ImageView
    private lateinit var capsule3: ImageView
    private lateinit var capsule4: ImageView
    private lateinit var drinkReminder: TextView

    private var selectedInterval: Int? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hydration_page)

        sharedPref = getSharedPreferences("hydration_pref", Context.MODE_PRIVATE)
        progressBar = findViewById(R.id.circularProgressBar)
        percentageText = findViewById(R.id.tvPercentage)
        plusBtn = findViewById(R.id.plus_btn)
        minusBtn = findViewById(R.id.minus_btn)
        halfBtn = findViewById(R.id.halfcup_btn)
        fullBtn = findViewById(R.id.fullcup_btn)
        largeBtn = findViewById(R.id.largecup_btn)
        barChart = findViewById(R.id.barChart)
        drinkReminder = findViewById(R.id.drink_reminder)


        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = sharedPref.getString("date", "")
        if (savedDate != today) {
            sharedPref.edit().putString("date", today).apply()
            for (i in 0 until totalCups) sharedPref.edit().putBoolean("cup_$i", false).apply()
            saveDailyIntake(0)
        }

        cups = mutableListOf(
            findViewById(R.id.cup1), findViewById(R.id.cup2), findViewById(R.id.cup3), findViewById(R.id.cup4),
            findViewById(R.id.cup5), findViewById(R.id.cup6), findViewById(R.id.cup7), findViewById(R.id.cup8)
        )

        for (i in 0 until totalCups) {
            val isCompleted = sharedPref.getBoolean("cup_$i", false)
            updateCupImage(cups[i], isCompleted)
        }

        cups.forEachIndexed { index, cup ->
            cup.setOnClickListener {
                val isFilled = sharedPref.getBoolean("cup_$index", false)
                if (!isFilled) {
                    selectCupSizeDialog { selectedSize ->
                        cupSizes[index] = selectedSize
                        sharedPref.edit().putBoolean("cup_$index", true).apply()
                        updateCupImage(cup, true)
                        updateProgressBar()
                    }
                } else {
                    sharedPref.edit().putBoolean("cup_$index", false).apply()
                    cupSizes[index] = null
                    updateCupImage(cup, false)
                    updateProgressBar()
                }
            }
        }

        halfBtn.setOnClickListener { addQuickCup(CupSize.HALF) }
        fullBtn.setOnClickListener { addQuickCup(CupSize.FULL) }
        largeBtn.setOnClickListener { addQuickCup(CupSize.LARGE) }

        plusBtn.setOnClickListener { showCustomCupDialog() }
        minusBtn.setOnClickListener {
            if (customCups.isNotEmpty()) {
                val removed = customCups.removeLast()
                updateProgressBar()
                Toast.makeText(this, "Removed ${removed.first} (${removed.second}ml)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No custom cups to remove", Toast.LENGTH_SHORT).show()
            }
        }

        capsule1 = findViewById(R.id.imageView63)
        capsule2 = findViewById(R.id.imageView64)
        capsule3 = findViewById(R.id.imageView65)
        capsule4 = findViewById(R.id.imageView66)
        reminderSwitch = findViewById(R.id.reminder_switch)

        setupCapsuleClickListeners()

        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (selectedInterval == null) {
                    Toast.makeText(this, "Select a reminder interval!", Toast.LENGTH_SHORT).show()
                    reminderSwitch.isChecked = false
                } else {
                    setHydrationReminder(selectedInterval!!)
                    Toast.makeText(this, "Reminders ON every $selectedInterval hr", Toast.LENGTH_SHORT).show()
                }
            } else {
                cancelHydrationReminder()
                Toast.makeText(this, "Reminders OFF", Toast.LENGTH_SHORT).show()
            }
        }

        updateProgressBar()
        setupBarChart()
        setupBottomNav()
    }

    private fun updateProgressBar() {
        val intake = cupSizes.filterNotNull().sumOf { it.ml } + customCups.sumOf { it.second }
        val progressPercent = ((intake.toFloat() / 2000) * 100).toInt().coerceAtMost(100)
        progressBar.progress = progressPercent
        percentageText.text = "$progressPercent%"
        saveDailyIntake(intake)
        setupBarChart()
    }

    private fun saveDailyIntake(ml: Int) {
        val day = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
        sharedPref.edit().putInt("intake_$day", ml).apply()
    }

    private fun loadWeeklyData(): List<Int> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return days.map { sharedPref.getInt("intake_$it", 0) }
    }

    private fun setupBarChart() {
        val weeklyData = loadWeeklyData()
        val entries = weeklyData.mapIndexed { index, ml -> BarEntry(index.toFloat(), ml.toFloat()) }
        val dataSet = BarDataSet(entries, "Weekly Intake (ml)").apply {
            color = Color.parseColor("#4DB6E3")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }
        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.animateY(800)
        barChart.invalidate()
    }

    private fun addQuickCup(size: CupSize) {
        customCups.add(size.name to size.ml)
        updateProgressBar()
        Toast.makeText(this, "Added ${size.name} (${size.ml}ml)", Toast.LENGTH_SHORT).show()
    }

    private fun showCustomCupDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val nameInput = EditText(this).apply { hint = "Cup Name" }
        val sizeInput = EditText(this).apply {
            hint = "Size (ml)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(nameInput)
        layout.addView(sizeInput)

        AlertDialog.Builder(this)
            .setTitle("Add Custom Cup")
            .setView(layout)
            .setPositiveButton("Add") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val size = sizeInput.text.toString().toIntOrNull()
                if (name.isNotEmpty() && size != null) {
                    customCups.add(name to size)
                    updateProgressBar()
                    Toast.makeText(this, "Added $name ($size ml)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun selectCupSizeDialog(onSelected: (CupSize) -> Unit) {
        val cupSizes = arrayOf("Half Cup (125ml)", "Full Cup (250ml)", "Large Cup (500ml)")
        val cupValues = arrayOf(CupSize.HALF, CupSize.FULL, CupSize.LARGE)
        AlertDialog.Builder(this)
            .setTitle("Select Cup Size")
            .setItems(cupSizes) { _, which -> onSelected(cupValues[which]) }
            .show()
    }

    private fun updateCupImage(cup: ImageView, isCompleted: Boolean) {
        cup.setImageResource(if (isCompleted) R.drawable.water_color else R.drawable.water_bw)
    }

    private fun updateReminder(selectedTime: String) {
        drinkReminder.text = "Drink water every $selectedTime"
    }

    private fun setupCapsuleClickListeners() {
        capsule1.setOnClickListener { selectCapsule(1, capsule1) }
        capsule2.setOnClickListener { selectCapsule(2, capsule2) }
        capsule3.setOnClickListener { selectCapsule(3, capsule3) }
        capsule4.setOnClickListener { selectCapsule(4, capsule4) }
    }

    private fun selectCapsule(hours: Int, selectedCapsule: ImageView) {
        resetAllCapsules()
        selectedCapsule.setColorFilter(ContextCompat.getColor(this, R.color.blueish_green))
        selectedInterval = hours
        updateReminder("$hours hour(s)")
        Toast.makeText(this, "Reminder every $hours hour(s)", Toast.LENGTH_SHORT).show()
    }

    private fun resetAllCapsules() {
        val white = ContextCompat.getColor(this, R.color.white)
        capsule1.setColorFilter(white)
        capsule2.setColorFilter(white)
        capsule3.setColorFilter(white)
        capsule4.setColorFilter(white)
    }

    private fun setHydrationReminder(hours: Int) {
        val intent = Intent(this, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + hours * 60 * 60 * 1000
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            hours * 60 * 60 * 1000L,
            pendingIntent
        )
    }

    private fun cancelHydrationReminder() {
        val intent = Intent(this, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_habits -> true
                R.id.nav_mood -> true
                R.id.nav_hydration -> true
                R.id.nav_analytics -> true
                R.id.nav_settings -> true
                else -> false
            }
        }
    }
}
