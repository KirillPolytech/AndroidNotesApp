package com.example.notifyer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Int,
    val text: String,
    val categoryId: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val reminderTime: Long? = null  // Новое поле для времени напоминания, nullable
) {
    fun getFormattedCreatedAt(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(createdAt))
    }

    fun getFormattedUpdatedAt(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt))
    }

    fun getFormattedReminderTime(): String? {
        return reminderTime?.let {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it))
        }
    }
}