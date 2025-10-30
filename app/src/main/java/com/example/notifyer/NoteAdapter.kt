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
    private var categories: List<Category>,
    private val onEditClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        val categoryText: TextView = itemView.findViewById(R.id.categoryText)
        val createdAtText: TextView = itemView.findViewById(R.id.createdAtText)
        val updatedAtText: TextView = itemView.findViewById(R.id.updatedAtText)
        val reminderTimeText: TextView = itemView.findViewById(R.id.reminderTimeText) // Новое поле
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.noteText.text = note.text
        val category = categories.find { it.id == note.categoryId }
        holder.categoryText.text = category?.name
            ?: holder.itemView.context.getString(R.string.category_none)
        holder.createdAtText.text = holder.itemView.context.getString(R.string.created_at_label, note.getFormattedCreatedAt())
        holder.updatedAtText.text = holder.itemView.context.getString(R.string.updated_at_label, note.getFormattedUpdatedAt())
        holder.reminderTimeText.text = note.getFormattedReminderTime()?.let {
            holder.itemView.context.getString(R.string.reminder_time_label, it)
        } ?: "No reminder"
        Log.d("NoteAdapter", "Note id: ${note.id}, categoryId: ${note.categoryId}, category: ${category?.name ?: "None"}, reminder: ${note.getFormattedReminderTime() ?: "None"}")
        holder.itemView.setOnClickListener {
            onEditClick(note)
        }
        holder.deleteButton.setOnClickListener {
            onDeleteClick(note)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
        Log.d("NoteAdapter", "Updated notes: ${notes.size}")
    }

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
        Log.d("NoteAdapter", "Updated categories: ${categories.size}")
    }
}