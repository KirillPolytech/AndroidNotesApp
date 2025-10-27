package com.example.notifyer

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var categorySpinner: Spinner
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var dbHelper: NoteDatabaseHelper
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        recyclerView = findViewById(R.id.recyclerView)
        categorySpinner = findViewById(R.id.categorySpinner)
        dbHelper = NoteDatabaseHelper(this)

        // Инициализация адаптера с пустым списком категорий
        noteAdapter = NoteAdapter(
            mutableListOf(),
            categories,
            onEditClick = { note -> showEditDialog(note) },
            onDeleteClick = { note -> showDeleteDialog(note) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        // Загрузка категорий и заметок
        scope.launch {
            try {
                categories = withContext(Dispatchers.IO) {
                    val cats = dbHelper.getAllCategories()
                    if (cats.isEmpty()) {
                        Log.d("MainActivity", "No categories found, creating defaults")
                        dbHelper.addCategory("Personal")
                        dbHelper.addCategory("Work")
                        dbHelper.getAllCategories()
                    } else {
                        Log.d("MainActivity", "Loaded ${cats.size} categories")
                        cats
                    }
                }

                // Настройка Spinner
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter

                // Обновление адаптера с новым списком категорий
                noteAdapter.updateCategories(categories)
                loadNotes()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading categories", e)
                Toast.makeText(this@MainActivity, "Error loading categories: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        addButton.setOnClickListener {
            val noteText = noteInput.text.toString()
            if (noteText.isNotEmpty()) {
                if (categories.isEmpty()) {
                    Toast.makeText(this, R.string.category_none, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val categoryId = categories[categorySpinner.selectedItemPosition].id
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            dbHelper.addNote(noteText, categoryId)
                        }
                        loadNotes()
                        noteInput.text.clear()
                        showNotification(noteText, getString(R.string.note_added_notification))
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error adding note", e)
                        Toast.makeText(this@MainActivity, "Error adding note: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Note text cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNotes() {
        scope.launch {
            try {
                val notes = withContext(Dispatchers.IO) {
                    dbHelper.getAllNotes()
                }
                Log.d("MainActivity", "Loaded ${notes.size} notes")
                noteAdapter.updateNotes(notes)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading notes", e)
                Toast.makeText(this@MainActivity, "Error loading notes: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditDialog(note: Note) {
        val editText = EditText(this).apply {
            setText(note.text)
        }
        val editSpinner = Spinner(this).apply {
            adapter = categorySpinner.adapter
            setSelection(categories.indexOfFirst { it.id == note.categoryId }.coerceAtLeast(0))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_note_title)
            .setView(android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(editText)
                addView(editSpinner)
            })
            .setPositiveButton(R.string.save_button) { _, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    val newCategoryId = categories[editSpinner.selectedItemPosition].id
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                dbHelper.updateNote(note.id, newText, newCategoryId)
                            }
                            loadNotes()
                            showNotification(newText, getString(R.string.note_updated_notification))
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error updating note", e)
                            Toast.makeText(this@MainActivity, "Error updating note: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Note text cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_note_title)
            .setMessage(R.string.delete_note_message)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            dbHelper.deleteNote(note.id)
                        }
                        loadNotes()
                        showNotification(note.text, getString(R.string.note_deleted_notification))
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error deleting note", e)
                        Toast.makeText(this@MainActivity, "Error deleting note: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showNotification(noteText: String, title: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "note_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(noteText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}