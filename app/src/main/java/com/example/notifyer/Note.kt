package com.example.notifyer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

data class Note(
    val id: Int,
    val title: String?,
    val body: String,
    val folderId: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val reminderTime: Long? = null,
    val isPinned: Boolean = false
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