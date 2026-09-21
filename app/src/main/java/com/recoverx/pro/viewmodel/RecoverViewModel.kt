package com.recoverx.pro.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recoverx.pro.data.AppLanguage
import com.recoverx.pro.data.RecoveryItem
import com.recoverx.pro.data.ScanState
import com.recoverx.pro.engine.ForensicCarver
import com.recoverx.pro.engine.RecoveryEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecoverViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = RecoveryEngine(app)
    private val carver = ForensicCarver(app)

    private val _items = MutableStateFlow<List<RecoveryItem>>(emptyList())
    val items: StateFlow<List<RecoveryItem>> = _items.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        getApplication<Application>().getSharedPreferences("settings", 0).edit()
            .putString("language", language.name).apply()
    }

    fun toggleSelection(id: String) {
        _selected.update { current -> if (id in current) current - id else current + id }
    }

    fun selectAll() { _selected.value = _items.value.map { it.id }.toSet() }
    fun clearSelection() { _selected.value = emptySet() }

    fun startScan() {
        if (_scanState.value is ScanState.Running) return
        viewModelScope.launch {
            _scanState.value = ScanState.Running("Starting", 0)
            runCatching {
                val result = engine.scanMedia(includeTrash = true) { phase, found ->
                    _scanState.value = ScanState.Running(phase, found)
                }
                _items.value = result.filter { it.isTrashed }
                    .sortedWith(compareByDescending<RecoveryItem> { it.dateTaken ?: 0L })
                _selected.value = emptySet()
                _scanState.value = ScanState.Done(result.size, 1)
            }.onFailure {
                _scanState.value = ScanState.Error(it.message ?: "Scan failed")
            }
        }
    }

    fun scanFolder(treeUri: Uri) {
        if (_scanState.value is ScanState.Running) return
        viewModelScope.launch {
            _scanState.value = ScanState.Running("Selected folder", 0)
            runCatching {
                val result = engine.scanTree(treeUri) { phase, found ->
                    _scanState.value = ScanState.Running(phase, found)
                }
                merge(result)
            }.onFailure { _scanState.value = ScanState.Error(it.message ?: "Folder scan failed") }
        }
    }

    fun deepScan(fileUri: Uri) {
        if (_scanState.value is ScanState.Running) return
        viewModelScope.launch {
            _scanState.value = ScanState.Running("Deep scan", 0)
            runCatching {
                val result = carver.carveTree(fileUri) { found ->
                    _scanState.value = ScanState.Running("Deep scan", found)
                }
                merge(result)
            }.onFailure { _scanState.value = ScanState.Error(it.message ?: "Deep scan failed") }
        }
    }

    fun exportSelected(destination: Uri) {
        val selectedItems = _items.value.filter { it.id in _selected.value }
        if (selectedItems.isEmpty()) {
            _message.value = if (_language.value == AppLanguage.ARABIC) "حدد صورة واحدة على الأقل" else "Select at least one photo"
            return
        }
        viewModelScope.launch {
            runCatching {
                val count = engine.exportItems(selectedItems, destination) { done, total ->
                    _scanState.value = ScanState.Running("Exporting $done / $total", done)
                }
                _scanState.value = ScanState.Done(_items.value.size, 1)
                _message.value = if (_language.value == AppLanguage.ARABIC) "تم تصدير $count ملف" else "Exported $count files"
                _selected.value = emptySet()
            }.onFailure { _message.value = it.message ?: "Export failed" }
        }
    }

    fun prepareTrashRestore(intentSenderCallback: (android.content.IntentSender) -> Unit): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        val trash = _items.value.filter { it.id in _selected.value && it.isTrashed }
        if (trash.isEmpty()) return false
        val request = MediaStore.createTrashRequest(
            getApplication<Application>().contentResolver,
            trash.take(2000).map { it.uri },
            false
        )
        intentSenderCallback(request.intentSender)
        return true
    }

    fun refreshAfterRestore() { startScan() }

    fun hasImagePermission(): Boolean {
        val context = getApplication<Application>()
        return when {
            Build.VERSION.SDK_INT >= 33 ->
                context.checkSelfPermission("android.permission.READ_MEDIA_IMAGES") == PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT >= 34 && context.checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PackageManager.PERMISSION_GRANTED)
            Build.VERSION.SDK_INT >= 29 -> context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
            else -> context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
        }
    }

    fun permissionList(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
        Build.VERSION.SDK_INT >= 33 -> arrayOf("android.permission.READ_MEDIA_IMAGES")
        else -> arrayOf("android.permission.READ_EXTERNAL_STORAGE")
    }

    private fun merge(newItems: List<RecoveryItem>) {
        val map = LinkedHashMap(_items.value.associateBy { it.id })
        newItems.forEach { map[it.id] = it }
        _items.value = map.values.toList().sortedWith(compareByDescending<RecoveryItem> { it.isTrashed }.thenByDescending { it.dateTaken ?: 0L })
        _selected.value = emptySet()
        _scanState.value = ScanState.Done(_items.value.size, 1)
    }

    private fun loadLanguage(): AppLanguage {
        val value = getApplication<Application>().getSharedPreferences("settings", 0)
            .getString("language", AppLanguage.ARABIC.name)
        return runCatching { AppLanguage.valueOf(value ?: AppLanguage.ARABIC.name) }.getOrDefault(AppLanguage.ARABIC)
    }
}
