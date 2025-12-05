package com.example.myapplication.models

data class Habit(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var currentProgress: Int = 0,
    var target: Int = 1,
    var streak: Int = 0,
    var isCompleted: Boolean = false
)
