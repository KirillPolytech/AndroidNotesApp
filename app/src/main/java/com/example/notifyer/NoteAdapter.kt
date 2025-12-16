package com.example.notifyer

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val notes: MutableList<Note>,
    private var folders: List<Folder>,
    private val onEditClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit,
    private val onPinToggle: (Note, Boolean) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bodyText: TextView = itemView.findViewById(R.id.bodyText)
        val folderText: TextView = itemView.findViewById(R.id.folderText)
        val createdAtText: TextView = itemView.findViewById(R.id.createdAtText)
        val reminderTimeText: TextView = itemView.findViewById(R.id.reminderTimeText)
        val pinButton: Button = itemView.findViewById(R.id.pinButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        holder.bodyText.text = note.body.ifBlank { "Empty note" }

        val folder = folders.find { it.id == note.folderId }
        holder.folderText.text = folder?.name ?: "No folder"
        holder.createdAtText.text = note.getFormattedCreatedAt()

        // Локализованное отображение напоминания
        val reminderText = note.getFormattedReminderTime()
        holder.reminderTimeText.text = if (reminderText != null) {
            holder.itemView.context.getString(R.string.reminder_time_label, reminderText)
        } else {
            holder.itemView.context.getString(R.string.no_reminder)
        }

        // Кнопка Pin
        holder.pinButton.text = if (note.isPinned) holder.itemView.context.getString(R.string.unpin) else holder.itemView.context.getString(R.string.pin)
        holder.pinButton.setOnClickListener {
            val newPinned = !note.isPinned
            onPinToggle(note, newPinned)
            holder.pinButton.text = if (newPinned) holder.itemView.context.getString(R.string.unpin) else holder.itemView.context.getString(R.string.pin)
        }

        holder.itemView.setOnClickListener { onEditClick(note) }
        holder.deleteButton.setOnClickListener { onDeleteClick(note) }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    fun updateFolders(newFolders: List<Folder>) {
        folders = newFolders
        notifyDataSetChanged()
    }

    // ВЫЗЫВАЕТСЯ ИЗ MainActivity
    fun togglePin(noteId: Int, newPinned: Boolean) {
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx != -1) {
            notes[idx] = notes[idx].copy(isPinned = newPinned)
            // Перерисовываем весь список — чтобы закреплённые поднялись
            notifyDataSetChanged()
        }
    }
}