package com.example.notifyer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.os.Build
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        Log.d("ReminderReceiver", "Received reminder for noteId: $noteId")
        if (noteId != -1) {
            val dbHelper = NoteDatabaseHelper(context)
            val note = dbHelper.getNoteById(noteId)
            note?.let {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "reminder_channel"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Note Reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Reminder")
                    .setContentText(it.text)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                notificationManager.notify(noteId, notification)
                Log.d("ReminderReceiver", "Notification sent for note: ${it.text}")
            } ?: Log.e("ReminderReceiver", "Note with id $noteId not found")
        } else {
            Log.e("ReminderReceiver", "Invalid noteId: $noteId")
        }
    }
}