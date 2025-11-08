package com.example.notifyer

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
        val titleText: TextView = itemView.findViewById(R.id.titleText)
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

        // Исправление: Показываем title, если он есть, иначе — первые слова из body
        val title = if (!note.title.isNullOrBlank()) {
            note.title
        } else if (!note.body.isBlank()) {
            // Берём первую строку из body
            note.body.split("\n").first().take(50)
        } else {
            "Untitled"
        }

        holder.titleText.text = title
        holder.bodyText.text = note.body

        val folder = folders.find { it.id == note.folderId }
        holder.folderText.text = folder?.name ?: "No folder"
        holder.createdAtText.text = note.getFormattedCreatedAt()
        holder.reminderTimeText.text = note.getFormattedReminderTime() ?: "No reminder"

        // Пин-кнопка
        holder.pinButton.text = if (note.isPinned) "Unpin" else "Pin"
        holder.pinButton.setOnClickListener {
            onPinToggle(note, !note.isPinned)
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

    fun togglePin(noteId: Int, newPinned: Boolean) {
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx != -1) {
            notes[idx] = notes[idx].copy(isPinned = newPinned)
            notifyItemChanged(idx)
        }
    }
}