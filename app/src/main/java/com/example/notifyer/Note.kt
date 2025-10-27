package com.example.notifyer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
        val id: Int,
        val text: String,
        val categoryId: Int,
        val createdAt: Long,
        val updatedAt: Long
) {
    fun getFormattedCreatedAt(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(createdAt))
    }

    fun getFormattedUpdatedAt(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(updatedAt))
    }
}