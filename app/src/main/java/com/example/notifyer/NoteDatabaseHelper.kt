package com.example.notifyer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class NoteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 5 // Увеличиваем версию
        private const val TABLE_NOTES = "notes"
        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_CATEGORY_ID = "category_id"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"
        private const val COLUMN_REMINDER_TIME = "reminder_time"
        private const val COLUMN_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("NoteDatabaseHelper", "Creating database tables")
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL
            )
        """
        db.execSQL(createCategoriesTable)

        val createNotesTable = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_CATEGORY_ID INTEGER,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL,
                $COLUMN_REMINDER_TIME INTEGER,
                FOREIGN KEY ($COLUMN_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_ID)
            )
        """
        db.execSQL(createNotesTable)

        // Начальные категории
        Log.d("NoteDatabaseHelper", "Inserting default categories")
        db.execSQL("INSERT INTO $TABLE_CATEGORIES ($COLUMN_NAME) VALUES ('Personal')")
        db.execSQL("INSERT INTO $TABLE_CATEGORIES ($COLUMN_NAME) VALUES ('Work')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("NoteDatabaseHelper", "Upgrading database from version $oldVersion to $newVersion")
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_REMINDER_TIME INTEGER")
            } catch (e: Exception) {
                Log.e("NoteDatabaseHelper", "Error adding reminder_time column", e)
            }
        }
        if (oldVersion < 5) {
            // Пересоздаём таблицы для синхронизации
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
            onCreate(db)
        }
    }

    fun addCategory(name: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
        }
        val id = db.insert(TABLE_CATEGORIES, null, values)
        Log.d("NoteDatabaseHelper", "Added category: $name, id: $id")
        db.close()
        return id
    }

    fun addNote(text: String, categoryId: Int, reminderTime: Long? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, text)
            put(COLUMN_CATEGORY_ID, categoryId)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            reminderTime?.let { put(COLUMN_REMINDER_TIME, it) }
        }
        val id = db.insert(TABLE_NOTES, null, values)
        Log.d("NoteDatabaseHelper", "Added note: $text, categoryId: $categoryId, id: $id, reminderTime: $reminderTime")
        db.close()
    }

    fun updateNote(id: Int, text: String, categoryId: Int, reminderTime: Long? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, text)
            put(COLUMN_CATEGORY_ID, categoryId)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            if (reminderTime != null) {
                put(COLUMN_REMINDER_TIME, reminderTime)
            } else {
                putNull(COLUMN_REMINDER_TIME)
            }
        }
        val rows = db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        Log.d("NoteDatabaseHelper", "Updated note id: $id, text: $text, categoryId: $categoryId, rows affected: $rows")
        db.close()
    }

    fun deleteNote(id: Int) {
        val db = writableDatabase
        val rows = db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id.toString()))
        Log.d("NoteDatabaseHelper", "Deleted note id: $id, rows affected: $rows")
        db.close()
    }

    fun getAllNotes(categoryId: Int? = null): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase
        val selection = categoryId?.let { "$COLUMN_CATEGORY_ID = ?" }
        val selectionArgs = categoryId?.let { arrayOf(it.toString()) }
        val cursor = db.query(TABLE_NOTES, arrayOf(COLUMN_ID, COLUMN_TEXT, COLUMN_CATEGORY_ID, COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_REMINDER_TIME),
            selection, selectionArgs, null, null, null)
        Log.d("NoteDatabaseHelper", "Fetched ${cursor.count} notes")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT))
            val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
            val reminderTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))
            notes.add(Note(id, text, catId, createdAt, updatedAt, reminderTime))
        }
        cursor.close()
        db.close()
        return notes
    }

    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, arrayOf(COLUMN_ID, COLUMN_NAME), null, null, null, null, null)
        Log.d("NoteDatabaseHelper", "Fetched ${cursor.count} categories")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            categories.add(Category(id, name))
        }
        cursor.close()
        db.close()
        return categories
    }

    fun getNoteById(id: Int): Note? {
        val db = readableDatabase
        val cursor = db.query(TABLE_NOTES, arrayOf(COLUMN_ID, COLUMN_TEXT, COLUMN_CATEGORY_ID, COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_REMINDER_TIME),
            "$COLUMN_ID = ?", arrayOf(id.toString()), null, null, null)
        return if (cursor.moveToNext()) {
            val text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT))
            val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
            val reminderTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))
            cursor.close()
            db.close()
            Note(id, text, catId, createdAt, updatedAt, reminderTime)
        } else {
            cursor.close()
            db.close()
            null
        }
    }
}