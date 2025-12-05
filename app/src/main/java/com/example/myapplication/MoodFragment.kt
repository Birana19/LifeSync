package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.models.MoodData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class MoodFragment : Fragment() {

    private var selectedEmojiRes: Int? = null
    private lateinit var noteInput: EditText
    private lateinit var saveBtn: Button
    private lateinit var calendarBtn: FloatingActionButton
    private lateinit var sharedPref: android.content.SharedPreferences
    private val gson = Gson()


    private val selectedDate: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val emojiMap by lazy {
        mapOf(
            R.id.imageView78 to R.drawable.happy_emoji,
            R.id.imageView79 to R.drawable.cry_emoji,
            R.id.imageView80 to R.drawable.party_emoji,
            R.id.imageView81 to R.drawable.love_emoji,
            R.id.imageView82 to R.drawable.laugh_emoji,
            R.id.imageView83 to R.drawable.cool_emoji
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mood, container, false)

        noteInput = view.findViewById(R.id.editTextTextMultiLine)
        saveBtn = view.findViewById(R.id.save_btn)
        calendarBtn = view.findViewById(R.id.floatingActionButton)
        sharedPref = requireContext().getSharedPreferences("mood_data", Context.MODE_PRIVATE)


        emojiMap.forEach { (viewId, emojiRes) ->
            val emojiView = view.findViewById<ImageView>(viewId)
            emojiView.setOnClickListener {
                selectedEmojiRes = emojiRes
                highlightSelectedEmoji(viewId)
            }
        }


        saveBtn.setOnClickListener {
            saveMood()
        }


        calendarBtn.setOnClickListener {
            saveMood()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CalendarFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun highlightSelectedEmoji(selectedId: Int) {
        emojiMap.keys.forEach { id ->
            val emojiView = view?.findViewById<ImageView>(id)
            emojiView?.alpha = if (id == selectedId) 1.0f else 0.3f
        }
    }

    private fun saveMood() {
        val note = noteInput.text.toString().trim()

        if (selectedEmojiRes == null && note.isEmpty()) {
            Toast.makeText(requireContext(), "Select an emoji or write a note", Toast.LENGTH_SHORT).show()
            return
        }

        val moodData = MoodData(selectedEmojiRes, note)
        val json = gson.toJson(moodData)
        sharedPref.edit().putString(selectedDate, json).apply()

        Toast.makeText(requireContext(), "Saved for $selectedDate", Toast.LENGTH_SHORT).show()
    }
}
