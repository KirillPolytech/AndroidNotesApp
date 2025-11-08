package com.example.notifyer

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // === VIEW ===
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var dbHelper: NoteDatabaseHelper
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var menuButton: ImageButton
    private lateinit var titleTextView: TextView
    private lateinit var searchInput: EditText
    private lateinit var noteCountTextView: TextView

    // === DATA ===
    private val scope = CoroutineScope(Dispatchers.Main)
    private var folders: List<Folder> = emptyList()
    private var currentFolderId: Int = -1
    private var searchQuery: String = ""

    companion object {
        private const val REQUEST_CODE_NOTIFICATIONS = 100
        private const val FOLDER_ALL_NOTES = 1
        private const val NAV_ADD_FOLDER_ID = 999
    }

    // === ПРИНУДИТЕЛЬНЫЙ РУССКИЙ ЯЗЫК ===
    override fun attachBaseContext(base: Context) {
        val locale = Locale("ru")
        Locale.setDefault(locale)

        val config = base.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            super.attachBaseContext(base.createConfigurationContext(config))
        } else {
            config.locale = locale
            base.resources.updateConfiguration(config, base.resources.displayMetrics)
            super.attachBaseContext(base)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupNavigation()
        setupBackPress()
        loadFoldersAndNotes()
        requestPermissions()
    }

    // === ИНИЦИАЛИЗАЦИЯ ВСЕХ VIEW ===
    private fun initViews() {
        // Находим все элементы
        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        recyclerView = findViewById(R.id.recyclerView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        menuButton = findViewById(R.id.menuButton)
        titleTextView = findViewById(R.id.titleTextView)
        searchInput = findViewById(R.id.searchInput)
        noteCountTextView = findViewById(R.id.noteCountTextView)
        dbHelper = NoteDatabaseHelper(this)

        // Открываем клавиатуру
        noteInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(noteInput, InputMethodManager.SHOW_IMPLICIT)

        // Лог для проверки языка
        Log.d("LOCALE", "Current locale: ${Locale.getDefault()}")
        Log.d("LOCALE", "All Notes: ${getString(R.string.all_notes)}")
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            notes = mutableListOf(),
            folders = folders,
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { showDeleteDialog(it) },
            onPinToggle = { note, pinned -> togglePin(note, pinned) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter
    }

    private fun setupClickListeners() {
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        addButton.setOnClickListener {
            val text = noteInput.text.toString().trim()
            if (text.isNotEmpty() && currentFolderId != -1) {
                showReminderDialog { reminderTime ->
                    addNoteWithReminder(text, reminderTime)
                }
            } else {
                Toast.makeText(this, "Введите текст и выберите папку", Toast.LENGTH_SHORT).show()
            }
        }

        // ПОИСК
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().lowercase()
                loadNotes()
            }
        })
    }

    private fun setupNavigation() {
        navView.setNavigationItemSelectedListener(this)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadFoldersAndNotes() {
        scope.launch {
            folders = withContext(Dispatchers.IO) {
                val list = dbHelper.getAllFolders()
                if (list.isEmpty()) {
                    dbHelper.addFolder(getString(R.string.all_notes))
                    dbHelper.addFolder(getString(R.string.personal))
                    dbHelper.addFolder(getString(R.string.work))
                    dbHelper.getAllFolders()
                } else {
                    list
                }
            }
            updateNavigationMenu()
            currentFolderId = FOLDER_ALL_NOTES
            updateTitle()
            loadNotes()
        }
    }

    private fun updateNavigationMenu() {
        val menu: Menu = navView.menu
        menu.clear()

        menu.add(0, FOLDER_ALL_NOTES, Menu.NONE, getString(R.string.all_notes))
            .setIcon(R.drawable.ic_folder)
            .setCheckable(true)

        folders.filter { it.id != FOLDER_ALL_NOTES }.forEach { folder ->
            menu.add(0, folder.id, Menu.NONE, folder.name)
                .setIcon(R.drawable.ic_folder)
                .setCheckable(true)
        }

        menu.add(0, NAV_ADD_FOLDER_ID, Menu.NONE, getString(R.string.add_folder))
            .setIcon(android.R.drawable.ic_menu_add)
    }

    private fun loadNotes() {
        scope.launch {
            val allNotes = withContext(Dispatchers.IO) {
                if (currentFolderId == FOLDER_ALL_NOTES) {
                    dbHelper.getAllNotes()
                } else {
                    dbHelper.getAllNotes(currentFolderId)
                }
            }

            val filteredNotes = if (searchQuery.isEmpty()) {
                allNotes
            } else {
                allNotes.filter { note ->
                    note.body.lowercase().contains(searchQuery)
                }
            }

            withContext(Dispatchers.Main) {
                noteAdapter.updateNotes(filteredNotes)
                noteAdapter.updateFolders(folders)
                updateNoteCount(filteredNotes.size)
            }
        }
    }

    private fun updateTitle() {
        val folderName = if (currentFolderId == FOLDER_ALL_NOTES) {
            getString(R.string.all_notes)
        } else {
            folders.find { it.id == currentFolderId }?.name ?: getString(R.string.all_notes)
        }
        titleTextView.text = folderName
    }

    private fun updateNoteCount(count: Int) {
        noteCountTextView.text = count.toString()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            NAV_ADD_FOLDER_ID -> {
                showAddFolderDialog()
                return true
            }
            else -> {
                currentFolderId = item.itemId
                item.isChecked = true
                updateTitle()
                searchInput.text.clear()
                loadNotes()
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
        }
    }

    private fun showAddFolderDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_folder)
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    scope.launch {
                        withContext(Dispatchers.IO) { dbHelper.addFolder(name) }
                        loadFoldersAndNotes()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addNoteWithReminder(text: String, reminderTime: Long?) {
        scope.launch {
            try {
                val noteId = withContext(Dispatchers.IO) {
                    dbHelper.addNote(
                        title = null,
                        body = text,
                        folderId = currentFolderId,
                        reminderTime = reminderTime,
                        isPinned = false
                    ).toInt()
                }
                if (reminderTime != null) {
                    setReminder(noteId, text, reminderTime)
                }
                withContext(Dispatchers.Main) {
                    noteInput.text.clear()
                    showNotification(text, getString(R.string.note_added_notification))
                }
                loadNotes()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun togglePin(note: Note, pinned: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.updateNote(
                    id = note.id,
                    title = note.title,
                    body = note.body,
                    folderId = note.folderId,
                    reminderTime = note.reminderTime,
                    isPinned = pinned
                )
            }
            noteAdapter.togglePin(note.id, pinned)
        }
    }

    private fun showEditDialog(note: Note) {
        val editText = EditText(this).apply { setText(note.body) }
        AlertDialog.Builder(this)
            .setTitle("Редактировать заметку")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.updateNote(
                                id = note.id,
                                title = note.title,
                                body = newText,
                                folderId = note.folderId,
                                reminderTime = note.reminderTime,
                                isPinned = note.isPinned
                            )
                        }
                        loadNotes()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку")
            .setMessage("Вы уверены?")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) { dbHelper.deleteNote(note.id) }
                    loadNotes()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReminderDialog(onSet: (Long?) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Установить напоминание")
            .setMessage("Добавить напоминание?")
            .setPositiveButton("Да") { _, _ ->
                val cal = Calendar.getInstance()
                DatePickerDialog(
                    this,
                    { _, y, m, d ->
                        cal.set(y, m, d)
                        TimePickerDialog(
                            this,
                            { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h)
                                cal.set(Calendar.MINUTE, min)
                                val time = cal.timeInMillis
                                if (time > System.currentTimeMillis()) {
                                    onSet(time)
                                } else {
                                    Toast.makeText(this, "Выберите время в будущем", Toast.LENGTH_SHORT).show()
                                    onSet(null)
                                }
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .setNegativeButton("Нет") { _, _ -> onSet(null) }
            .show()
    }

    private suspend fun setReminder(noteId: Int, text: String, time: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED
        ) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Нет разрешения на точные будильники", Toast.LENGTH_LONG).show()
            }
            return
        }
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("noteId", noteId)
            putExtra("text", text)
        }
        val pending = PendingIntent.getBroadcast(
            this,
            noteId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pending)
    }

    private fun showNotification(text: String, title: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "note_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Заметки", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQUEST_CODE_NOTIFICATIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // OK
        } else {
            Toast.makeText(this, "Некоторые разрешения отклонены", Toast.LENGTH_LONG).show()
        }
    }
}