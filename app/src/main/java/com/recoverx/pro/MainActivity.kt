package com.recoverx.pro
import androidx.activity.result.IntentSenderRequest

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.recoverx.pro.data.AppLanguage
import com.recoverx.pro.data.RecoveryItem
import com.recoverx.pro.data.ScanState
import com.recoverx.pro.ui.RecoverTheme
import com.recoverx.pro.util.formatBytes
import com.recoverx.pro.util.uiStrings
import com.recoverx.pro.viewmodel.RecoverViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RecoverApp() }
    }
}

@Composable
fun RecoverApp(vm: RecoverViewModel = viewModel()) {
    val language by vm.language.collectAsState()
    val s = uiStrings(language)
    val layoutDirection = if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        RecoverTheme { RecoverRoot(vm, s) }
    }
}

@Composable
private fun RecoverRoot(vm: RecoverViewModel, s: com.recoverx.pro.util.UiStrings) {
    var tab by remember { mutableStateOf(0) }
    var showPermission by remember { mutableStateOf(false) }
    var showDeep by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { vm.scanFolder(it) }
    }
    val deepPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.deepScan(it) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) vm.startScan() else showPermission = true
    }
    val destinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { vm.exportSelected(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { vm.refreshAfterRestore() }

    val snack by vm.message.collectAsState()
    LaunchedEffect(snack) { if (snack != null) { kotlinx.coroutines.delay(2200); vm.clearMessage() } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Rounded.PhotoLibrary, null) }, label = { Text(s.home) })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Rounded.ImageSearch, null) }, label = { Text(s.results) })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Rounded.Settings, null) }, label = { Text(s.settings) })
            }
        }
    ) { padding ->
        AnimatedContent(targetState = tab, modifier = Modifier.padding(padding), label = "tab") { currentTab ->
            when (currentTab) {
                0 -> HomeScreen(vm, s,
                    onScan = {
                        if (vm.hasImagePermission()) vm.startScan() else showPermission = true
                    },
                    onFolder = { folderPicker.launch(null) },
                    onDeep = { showDeep = true }
                )
                1 -> ResultsScreen(vm, s,
                    onExport = { destinationPicker.launch(null) },
                    onRestore = {
                        vm.prepareTrashRestore { sender -> restoreLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build()) }
                    }
                )
                else -> SettingsScreen(vm, s)
            }
        }
    }

    if (showPermission) {
        PermissionDialog(s, onGrant = { showPermission = false; permissionLauncher.launch(vm.permissionList()) }, onClose = { showPermission = false })
    }
    if (showDeep) {
        AlertDialog(
            onDismissRequest = { showDeep = false },
            icon = { Icon(Icons.Rounded.Storage, null) },
            title = { Text(s.deepScan) },
            text = { Text(s.deepScanDescription) },
            confirmButton = {
                Button(onClick = { showDeep = false; deepPicker.launch(arrayOf("*/*")) }) { Text(s.selectStorageImage) }
            },
            dismissButton = { TextButton(onClick = { showDeep = false }) { Text(s.cancel) } }
        )
    }
    if (snack != null) {
        Surface(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 10.dp
        ) { Text(snack!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.inverseOnSurface) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(vm: RecoverViewModel, s: com.recoverx.pro.util.UiStrings, onScan: () -> Unit, onFolder: () -> Unit, onDeep: () -> Unit) {
    val state by vm.scanState.collectAsState()
    val items by vm.items.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CenterAlignedTopAppBar(title = { Text("RecoverX", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF151725), Color(0xFF2A2148)))).padding(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF7C5CFF).copy(alpha = .18f)) {
                            Icon(Icons.Rounded.Restore, null, tint = Color(0xFFC6B6FF), modifier = Modifier.padding(13.dp).size(30.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column { Text(s.realRecovery, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp); Text(s.appSubtitle, color = Color(0xFFD5D3DF), fontSize = 13.sp) }
                    }
                    Text(s.scanDescription, color = Color(0xFFECEAF3), lineHeight = 20.sp)
                    Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(15.dp)) {
                        Icon(Icons.Rounded.Bolt, null); Spacer(Modifier.width(8.dp)); Text(s.scanNow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state is ScanState.Running) {
            val run = state as ScanState.Running
            Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(run.phase, fontWeight = FontWeight.Bold); LinearProgressIndicator(Modifier.fillMaxWidth()); Text(s.foundCount(run.found), style = MaterialTheme.typography.labelMedium) } }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(Icons.Rounded.FolderOpen, s.scanFolder, { onFolder() }, Modifier.weight(1f))
            ActionCard(Icons.Rounded.Storage, s.deepScan, { onDeep() }, Modifier.weight(1f))
        }

        if (items.isNotEmpty()) {
            Text(s.recentFinds, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            PhotoGrid(items.take(8), emptySet(), onToggle = {})
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(12.dp))
                Column { Text(s.noFakeResults, fontWeight = FontWeight.Bold); Text("MediaStore • Trash • SAF • Forensic carving", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier.clickable { onClick() }, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary); Text(title, fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsScreen(vm: RecoverViewModel, s: com.recoverx.pro.util.UiStrings, onExport: () -> Unit, onRestore: () -> Unit) {
    val items by vm.items.collectAsState(); val selected by vm.selected.collectAsState(); val state by vm.scanState.collectAsState()
    var onlyTrash by remember { mutableStateOf(false) }
    val shown = if (onlyTrash) items.filter { it.isTrashed } else items
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.results, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            AssistChip(onClick = { if (selected.size == shown.size && shown.isNotEmpty()) vm.clearSelection() else vm.selectAll() }, label = { Text(if (selected.isNotEmpty()) s.clearSelection else s.selectAll) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(selected = !onlyTrash, onClick = { onlyTrash = false }, label = { Text(s.mediaSource) })
            FilterChip(selected = onlyTrash, onClick = { onlyTrash = true }, label = { Text(s.trashSource) })
        }
        if (state is ScanState.Running) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (shown.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Rounded.ImageSearch, null, modifier = Modifier.size(56.dp)); Text(s.noResults, fontWeight = FontWeight.Bold) } }
        else PhotoGrid(shown, selected, onToggle = vm::toggleSelection)

        if (selected.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onRestore, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Restore, null); Spacer(Modifier.width(6.dp)); Text(s.restore) }
                Button(onClick = onExport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Storage, null); Spacer(Modifier.width(6.dp)); Text(s.export) }
            }
        }
    }
}

@Composable
private fun PhotoGrid(items: List<RecoveryItem>, selected: Set<String>, onToggle: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(bottom = 90.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            val picked = item.id in selected
            Card(modifier = Modifier.height(128.dp).clickable { onToggle(item.id) }, shape = RoundedCornerShape(18.dp)) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(model = item.uri, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    if (picked) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = .32f)))
                    Row(Modifier.align(Alignment.TopEnd).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.isTrashed) Surface(shape = RoundedCornerShape(9.dp), color = Color.Black.copy(alpha = .6f)) { Icon(Icons.Rounded.DeleteSweep, null, tint = Color.White, modifier = Modifier.padding(5.dp).size(16.dp)) }
                        if (picked) Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) { Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.padding(4.dp).size(18.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: RecoverViewModel, s: com.recoverx.pro.util.UiStrings) {
    val language by vm.language.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(s.settings, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Language, null); Spacer(Modifier.width(12.dp)); Text(s.language, fontWeight = FontWeight.Bold) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = language == AppLanguage.ARABIC, onClick = { vm.setLanguage(AppLanguage.ARABIC) }, label = { Text(s.arabic) })
                FilterChip(selected = language == AppLanguage.ENGLISH, onClick = { vm.setLanguage(AppLanguage.ENGLISH) }, label = { Text(s.english) })
            }
        } }
        InfoCard(Icons.Rounded.Lock, s.privacy, if (language == AppLanguage.ARABIC) "الفحص والتصدير محليان افتراضياً. الصور لا تُرفع إلى خادم RecoverX." else "Scanning and export are local by default. RecoverX does not upload your photos to a server.")
        InfoCard(Icons.Rounded.Tune, s.limitations, if (language == AppLanguage.ARABIC) "Android يمنع التطبيقات العادية من الوصول إلى المساحة الخام أو بيانات التطبيقات الأخرى. الاسترجاع العميق الحقيقي يحتاج ملف dump يختاره المستخدم أو صلاحيات خاصة يوفرها الجهاز." else "Android prevents normal apps from reading raw storage or other apps' private data. True deep carving requires a user-provided storage image/dump or device-specific privileged access.")
        InfoCard(Icons.Rounded.CheckCircle, s.ready, if (language == AppLanguage.ARABIC) "يدعم Android 6+، ويستهدف API 36 للنشر على Google Play." else "Supports Android 6+ and targets API 36 for current Google Play submission requirements.")
        Text(s.appVersion, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card(shape = RoundedCornerShape(24.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(12.dp)); Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, style = MaterialTheme.typography.bodyMedium) } } }
}

@Composable
private fun PermissionDialog(s: com.recoverx.pro.util.UiStrings, onGrant: () -> Unit, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, icon = { Icon(Icons.Rounded.PhotoLibrary, null) }, title = { Text(s.permissionTitle) }, text = { Text(s.permissionDescription) }, confirmButton = { Button(onClick = onGrant) { Text(s.grantAccess) } }, dismissButton = { TextButton(onClick = onClose) { Text(s.close) } })
}
