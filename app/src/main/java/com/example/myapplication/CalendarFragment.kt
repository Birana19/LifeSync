package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.models.MoodData
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var sharedPref: android.content.SharedPreferences
    private val gson = Gson()

    private lateinit var emoji1: ImageView
    private lateinit var emoji2: ImageView
    private lateinit var emoji3: ImageView
    private lateinit var backBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        emoji1 = view.findViewById(R.id.imageView84)
        emoji2 = view.findViewById(R.id.imageView85)
        emoji3 = view.findViewById(R.id.imageView86)
        backBtn = view.findViewById(R.id.back_btn)

        sharedPref = requireContext().getSharedPreferences("mood_data", Context.MODE_PRIVATE)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        showEmojiForDate(today)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val moodJson = sharedPref.getString(dateStr, null)
            if (moodJson != null) {
                val moodData = gson.fromJson(moodJson, MoodData::class.java)
                showMoodDialog(dateStr, moodData)
            } else {
                Toast.makeText(requireContext(), "No mood saved for this date", Toast.LENGTH_SHORT).show()
            }
        }


        backBtn.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MoodFragment())
                .commit()
        }

        return view
    }

    private fun showEmojiForDate(date: String) {
        val json = sharedPref.getString(date, null)
        if (json != null) {
            val moodData = gson.fromJson(json, MoodData::class.java)
            moodData.emojiResId?.let { resId ->
                emoji1.setImageResource(resId)
            }
        }
    }

    private fun showMoodDialog(date: String, moodData: MoodData) {
        val message = if (moodData.note.isNotEmpty()) moodData.note else "No note for this day."
        AlertDialog.Builder(requireContext())
            .setTitle("Mood on $date")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
