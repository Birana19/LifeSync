package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.models.Habit

class HabitAdapter(
    private val onCheckChanged: (Habit, Boolean) -> Unit,
    private val onEdit: (Habit) -> Unit,
    private val onDelete: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {



    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val habitName: TextView = itemView.findViewById(R.id.habit_name)
        private val habitProgress: ProgressBar = itemView.findViewById(R.id.habit_progress)
        private val habitStreak: TextView = itemView.findViewById(R.id.streak_text)
        private val habitCheckBox: CheckBox = itemView.findViewById(R.id.habit_checkbox)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        fun bind(habit: Habit) {
            habitName.text = habit.name
            habitStreak.text = "🔥 Streak: ${habit.streak} days"

            habitProgress.max = habit.target
            val progress = habit.currentProgress.coerceIn(0, habit.target)
            habitProgress.progress = progress

            habitCheckBox.setOnCheckedChangeListener(null)
            habitCheckBox.isChecked = habit.isCompleted

            habitCheckBox.setOnCheckedChangeListener { _, checked ->
                onCheckChanged(habit, checked)
            }

            btnEdit.setOnClickListener { onEdit(habit) }
            btnDelete.setOnClickListener { onDelete(habit) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
    override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean =
        oldItem == newItem

}
