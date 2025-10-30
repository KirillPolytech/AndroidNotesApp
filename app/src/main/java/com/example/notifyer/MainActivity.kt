package com.example.notifyer

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var categorySpinner: Spinner
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var dbHelper: NoteDatabaseHelper
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories: List<Category> = emptyList()

    companion object {
        private const val REQUEST_CODE_NOTIFICATIONS = 100
        private const val REQUEST_CODE_ALARM = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Запрос разрешений
        requestPermissions()

        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        recyclerView = findViewById(R.id.recyclerView)
        categorySpinner = findViewById(R.id.categorySpinner)
        dbHelper = NoteDatabaseHelper(this)

        // Инициализация адаптера
        noteAdapter = NoteAdapter(
            mutableListOf(),
            categories,
            onEditClick = { note -> showEditDialog(note) },
            onDeleteClick = { note -> showDeleteDialog(note) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        // Загрузка категорий и заметок
        loadData()

        addButton.setOnClickListener {
            val noteText = noteInput.text.toString()
            if (noteText.isNotEmpty()) {
                if (categories.isEmpty()) {
                    Toast.makeText(this, R.string.category_none, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val categoryId = categories[categorySpinner.selectedItemPosition].id
                showReminderDialog { reminderTime ->
                    scope.launch {
                        try {
                            val noteId = withContext(Dispatchers.IO) {
                                dbHelper.addNote(noteText, categoryId, reminderTime)
                                dbHelper.getAllNotes().lastOrNull()?.id ?: -1
                            }
                            if (reminderTime != null && noteId != -1) {
                                setReminder(noteId, noteText, reminderTime)
                            }
                            loadNotes()
                            noteInput.text.clear()
                            showNotification(noteText, getString(R.string.note_added_notification))
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error adding note", e)
                            Toast.makeText(this@MainActivity, "Error adding note: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Note text cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadData() {
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
                Log.d("MainActivity", "Spinner adapter set with ${categories.size} items")

                // Обновление адаптера
                noteAdapter.updateCategories(categories)
                loadNotes()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading data", e)
                Toast.makeText(this@MainActivity, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
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
                    showReminderDialog { reminderTime ->
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    dbHelper.updateNote(note.id, newText, newCategoryId, reminderTime)
                                }
                                if (reminderTime != null) {
                                    setReminder(note.id, newText, reminderTime)
                                }
                                loadNotes()
                                showNotification(newText, getString(R.string.note_updated_notification))
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error updating note", e)
                                Toast.makeText(this@MainActivity, "Error updating note: ${e.message}", Toast.LENGTH_LONG).show()
                            }
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

    private fun showReminderDialog(onSet: (Long?) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Set Reminder")
            .setMessage("Would you like to set a reminder for this note?")
            .setPositiveButton("Yes") { _, _ ->
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this,
                    { _: DatePicker, year: Int, month: Int, day: Int ->
                        TimePickerDialog(
                            this,
                            { _: TimePicker, hour: Int, minute: Int ->
                                calendar.set(year, month, day, hour, minute, 0)
                                val reminderTime = calendar.timeInMillis
                                if (reminderTime > System.currentTimeMillis()) {
                                    onSet(reminderTime)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Reminder time must be in the future",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSet(null)
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .setNegativeButton("No") { _, _ ->
                onSet(null)
            }
            .show()
    }

    private suspend fun setReminder(noteId: Int, noteText: String, reminderTime: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.USE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Permission denied for setting reminder", Toast.LENGTH_LONG).show()
            }
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("noteId", noteId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            noteId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            Log.d("MainActivity", "Reminder set for noteId: $noteId, text: $noteText at ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(reminderTime)}")
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Failed to set reminder", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Failed to set reminder: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showNotification(noteText: String, title: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.USE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.USE_EXACT_ALARM)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE_NOTIFICATIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS || requestCode == REQUEST_CODE_ALARM) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("MainActivity", "Permissions granted")
            } else {
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_LONG).show()
            }
        }
    }
}