package com.example.notifyer

data class Folder(
    val id: Int,
    val name: String,
    val parentId: Int? = null,  // Для вложенности
    val isPinned: Boolean = false
)