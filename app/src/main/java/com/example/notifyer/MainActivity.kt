package com.example.notifyer

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.*

class MainActivity : AppCompatActivity() {
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var dbHelper: NoteDatabaseHelper
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        recyclerView = findViewById(R.id.recyclerView)
        dbHelper = NoteDatabaseHelper(this)
        noteAdapter = NoteAdapter(mutableListOf()) { note, position ->
            showEditDialog(note, position)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        loadNotes()

        addButton.setOnClickListener {
            val noteText = noteInput.text.toString()
            if (noteText.isNotEmpty()) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dbHelper.addNote(noteText)
                    }
                    loadNotes()
                    noteInput.text.clear()
                    showNotification(noteText, "New Note Added")
                }
            }
        }
    }

    private fun loadNotes() {
        scope.launch {
            val notes = withContext(Dispatchers.IO) {
                dbHelper.getAllNotes()
            }
            noteAdapter.updateNotes(notes)
        }
    }

    private fun showEditDialog(note: Note, position: Int) {
        val editText = EditText(this).apply {
            setText(note.text)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.updateNote(note.id, newText)
                        }
                        loadNotes()
                        showNotification(newText, "Note Updated")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotification(noteText: String, title: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "note_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Note Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(noteText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}