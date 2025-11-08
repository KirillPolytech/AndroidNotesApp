package com.example.notifyer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class NoteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 7
        private const val TABLE_NOTES = "notes"
        private const val TABLE_FOLDERS = "folders"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_BODY = "body"
        private const val COLUMN_FOLDER_ID = "folder_id"
        private const val COLUMN_PARENT_ID = "parent_id"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"
        private const val COLUMN_REMINDER_TIME = "reminder_time"
        private const val COLUMN_IS_PINNED = "is_pinned"
        private const val COLUMN_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("NoteDatabaseHelper", "Creating tables")
        val createFoldersTable = """
            CREATE TABLE $TABLE_FOLDERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_PARENT_ID INTEGER,
                FOREIGN KEY ($COLUMN_PARENT_ID) REFERENCES $TABLE_FOLDERS($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createFoldersTable)

        val createNotesTable = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_BODY TEXT NOT NULL,
                $COLUMN_FOLDER_ID INTEGER,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL,
                $COLUMN_REMINDER_TIME INTEGER,
                $COLUMN_IS_PINNED INTEGER DEFAULT 0,
                FOREIGN KEY ($COLUMN_FOLDER_ID) REFERENCES $TABLE_FOLDERS($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createNotesTable)

        // Начальные папки
        db.execSQL("INSERT INTO $TABLE_FOLDERS ($COLUMN_NAME) VALUES ('All Notes')")
        db.execSQL("INSERT INTO $TABLE_FOLDERS ($COLUMN_NAME) VALUES ('Personal')")
        db.execSQL("INSERT INTO $TABLE_FOLDERS ($COLUMN_NAME) VALUES ('Work')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("NoteDatabaseHelper", "Upgrading from $oldVersion to $newVersion")
        if (oldVersion < 7) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLDERS")
            onCreate(db)
        }
    }

    fun addFolder(name: String, parentId: Int? = null): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            parentId?.let { put(COLUMN_PARENT_ID, it) }
        }
        val id = db.insert(TABLE_FOLDERS, null, values)
        db.close()
        return id
    }

    fun addNote(title: String?, body: String, folderId: Int, reminderTime: Long? = null, isPinned: Boolean = false): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_BODY, body)
            put(COLUMN_FOLDER_ID, folderId)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            if (reminderTime != null) {
                put(COLUMN_REMINDER_TIME, reminderTime)
                Log.d("NoteDatabaseHelper", "Added reminderTime: $reminderTime for note")
            } else {
                putNull(COLUMN_REMINDER_TIME)
                Log.d("NoteDatabaseHelper", "No reminderTime set for note")
            }
            put(COLUMN_IS_PINNED, if (isPinned) 1 else 0)
        }
        val id = db.insert(TABLE_NOTES, null, values)
        Log.d("NoteDatabaseHelper", "Added note id: $id, title: $title, body: $body, folderId: $folderId")
        db.close()
        return id
    }

    fun updateNote(id: Int, title: String?, body: String, folderId: Int, reminderTime: Long? = null, isPinned: Boolean = false) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_BODY, body)
            put(COLUMN_FOLDER_ID, folderId)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            if (reminderTime != null) {
                put(COLUMN_REMINDER_TIME, reminderTime)
                Log.d("NoteDatabaseHelper", "Updated reminderTime: $reminderTime for note $id")
            } else {
                putNull(COLUMN_REMINDER_TIME)
                Log.d("NoteDatabaseHelper", "Removed reminderTime for note $id")
            }
            put(COLUMN_IS_PINNED, if (isPinned) 1 else 0)
        }
        val rows = db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        Log.d("NoteDatabaseHelper", "Updated note id: $id, rows: $rows")
        db.close()
    }

    fun deleteNote(id: Int) {
        val db = writableDatabase
        db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun getAllNotes(folderId: Int? = null): List<Note> {

        val notes = mutableListOf<Note>()
        val db = readableDatabase
        val selection = folderId?.let { "$COLUMN_FOLDER_ID = ?" }
        val selectionArgs = folderId?.let { arrayOf(it.toString()) }

        // ВАЖНО: СОРТИРОВКА — сначала закреплённые, потом по дате
        val cursor = db.query(
            TABLE_NOTES, null, selection, selectionArgs, null, null,
            "$COLUMN_IS_PINNED DESC, $COLUMN_CREATED_AT DESC"
        )

        while (cursor.moveToNext()) {
            val note = Note(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)) ?: "",
                body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY)) ?: "",
                folderId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ID)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                reminderTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)),
                isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1
            )
            notes.add(note)
        }
        cursor.close()
        db.close()
        return notes
    }

    fun getAllFolders(parentId: Int? = null): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = readableDatabase
        val selection = parentId?.let { "$COLUMN_PARENT_ID = ?" }
        val selectionArgs = parentId?.let { arrayOf(it.toString()) }
        val cursor = db.query(TABLE_FOLDERS, null, selection, selectionArgs, null, null, "$COLUMN_NAME ASC")
        while (cursor.moveToNext()) {
            val folder = Folder(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            )
            folders.add(folder)
        }
        cursor.close()
        db.close()
        return folders
    }

    fun getNoteById(id: Int): Note? {
        val db = readableDatabase
        val cursor = db.query(TABLE_NOTES, null, "$COLUMN_ID = ?", arrayOf(id.toString()), null, null, null)
        return if (cursor.moveToNext()) {
            val note = Note(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)) ?: "",
                body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY)) ?: "",
                folderId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLDER_ID)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                reminderTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)),
                isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1
            )
            cursor.close()
            db.close()
            note
        } else {
            cursor.close()
            db.close()
            null
        }
    }
}