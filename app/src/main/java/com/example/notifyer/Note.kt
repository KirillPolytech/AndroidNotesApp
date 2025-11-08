package com.example.notifyer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Int,
    val title: String,
    val body: String,  // Поддержка форматирования (HTML-like)
    val folderId: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val reminderTime: Long? = null,
    val isPinned: Boolean = false
) {
    fun getFormattedCreatedAt(): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(createdAt))
    fun getFormattedUpdatedAt(): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt))
    fun getFormattedReminderTime(): String? = reminderTime?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
}