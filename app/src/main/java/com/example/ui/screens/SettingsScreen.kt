package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import com.example.data.repository.MusicRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.EmeraldHifi
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    isLowBatteryMode: Boolean,
    repository: MusicRepository,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleLowBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val settingsManager = repository.settingsManager

    var cacheBytes by remember { mutableLongStateOf(repository.getCacheSizeBytes()) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isMessageSuccess by remember { mutableStateOf(true) }

    var currentBackupFolderPath by remember { mutableStateOf(settingsManager.getBackupFolderPath()) }
    var showFolderEditDialog by remember { mutableStateOf(false) }

    var aboutInfo by remember { mutableStateOf(settingsManager.getAboutInfo()) }
    var showAboutEditDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    var showJsonImportDialog by remember { mutableStateOf(false) }
    var jsonInputText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        // Screen Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SETTINGS & BACKUP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = appTheme.textPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Preferences, offline storage & device sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.accent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(appTheme.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = appTheme.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Global Action Feedback Banner
                if (actionMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMessageSuccess) Color(0x3310B981) else Color(0x33EF4444))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = actionMessage!!,
                            color = if (isMessageSuccess) EmeraldHifi else Color(0xFFFF6B6B),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section 1: Backup & Offline Storage
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "BACKUP & OFFLINE STORAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = appTheme.accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = appTheme.accent, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Destination Folder", color = appTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = currentBackupFolderPath,
                                        color = appTheme.textSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { showFolderEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Change Folder", tint = appTheme.accent)
                            }
                        }

                        Text(
                            text = "Songs downloaded offline and all JSON backup files are mapped directly to this folder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textMuted
                        )

                        // Scan & Refresh Folder Button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val count = repository.scanOfflineFolder()
                                    isMessageSuccess = true
                                    actionMessage = "Folder scan completed! Refreshed $count songs/contents."
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = appTheme.accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Refresh Offline Songs", color = appTheme.textPrimary, fontSize = 13.sp)
                        }

                        // Export & Import Buttons (for moving to a new phone)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val file = repository.exportConfigAndPlaylists()
                                            isMessageSuccess = true
                                            actionMessage = "Exported config & playlists JSON to:\n${file.name}"
                                        } catch (e: Exception) {
                                            isMessageSuccess = false
                                            actionMessage = "Export failed: ${e.message}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = if (appTheme.isLight) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", color = if (appTheme.isLight) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    showJsonImportDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appTheme.accentSecondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // App Update Backup Option
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val backupFile = repository.createAppUpdateBackup()
                                        isMessageSuccess = true
                                        actionMessage = "Full update backup saved:\n${backupFile.name}"
                                    } catch (e: Exception) {
                                        isMessageSuccess = false
                                        actionMessage = "Backup failed: ${e.message}"
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = EmeraldHifi, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create App Update Backup", color = appTheme.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Memory & Playback Cache
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "MEMORY & PLAYBACK CACHE",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldHifi,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = EmeraldHifi, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Temp Playback Cache", color = appTheme.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f MB", cacheBytes / (1024f * 1024f)),
                                color = appTheme.accent,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Streaming track cache is automatically cleared right after playback completes so the app stays fast and consumes minimal memory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textMuted
                        )

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.clearCache()
                                    cacheBytes = repository.getCacheSizeBytes()
                                    isMessageSuccess = true
                                    actionMessage = "Cleared temporary playback cache successfully."
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = appTheme.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Temp Cache Now", color = appTheme.textPrimary, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val purgeRes = repository.purgeSampleTestData()
                                    isMessageSuccess = true
                                    actionMessage = purgeRes.message
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = appTheme.accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Purge Sample & Test Data", color = appTheme.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 3: About & Developer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ABOUT & DEVELOPER",
                        style = MaterialTheme.typography.labelSmall,
                        color = appTheme.accentSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showAboutEditDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit About", tint = appTheme.accentSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("App", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                            Text(aboutInfo.appName, style = MaterialTheme.typography.bodySmall, color = appTheme.textPrimary, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Version", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                            Text(aboutInfo.appVersion, style = MaterialTheme.typography.bodySmall, color = appTheme.accent, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Developer", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                            Text(aboutInfo.developerName, style = MaterialTheme.typography.bodySmall, color = appTheme.textPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Updated Date", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                            Text(aboutInfo.updatedDate, style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GitHub", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val url = if (aboutInfo.githubAccount.startsWith("http")) {
                                            aboutInfo.githubAccount
                                        } else {
                                            "https://${aboutInfo.githubAccount}"
                                        }
                                        try {
                                            uriHandler.openUri(url)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (ex: Exception) {}
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = aboutInfo.githubAccount,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appTheme.accent,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open Link", tint = appTheme.accent, modifier = Modifier.size(14.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("License", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showLicenseDialog = true }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "MIT License",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appTheme.accent,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = "View License", tint = appTheme.accent, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Theme & Appearance
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "THEME & APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = appTheme.accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPill(
                        text = "Auto",
                        isSelected = currentThemeMode == ThemeMode.AUTO,
                        onClick = { onThemeModeChange(ThemeMode.AUTO) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassPill(
                        text = "AMOLED",
                        isSelected = currentThemeMode == ThemeMode.AMOLED_BLACK,
                        onClick = { onThemeModeChange(ThemeMode.AMOLED_BLACK) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassPill(
                        text = "Deep Glass",
                        isSelected = currentThemeMode == ThemeMode.DEEP_GLASS,
                        onClick = { onThemeModeChange(ThemeMode.DEEP_GLASS) },
                        modifier = Modifier.weight(1.1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPill(
                        text = "Titanium Slate",
                        isSelected = currentThemeMode == ThemeMode.TITANIUM_GRAY,
                        onClick = { onThemeModeChange(ThemeMode.TITANIUM_GRAY) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassPill(
                        text = "Clean Light",
                        isSelected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Low Battery Mode", style = MaterialTheme.typography.bodyMedium, color = appTheme.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Reduces animations and polling during long playback.", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                        }
                        Switch(
                            checked = isLowBatteryMode,
                            onCheckedChange = { onToggleLowBattery() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldHifi,
                                checkedTrackColor = Color(0x6610B981)
                            )
                        )
                    }
                }
            }
        }
    }

    // FOLDER EDIT DIALOG
    if (showFolderEditDialog) {
        var tempPath by remember { mutableStateOf(currentBackupFolderPath) }
        Dialog(onDismissRequest = { showFolderEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = appTheme.surfaceElevated,
                border = BorderStroke(1.dp, appTheme.cardBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Choose Backup & Storage Folder", style = MaterialTheme.typography.titleSmall, color = appTheme.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Offline downloaded songs and exports will be placed in this directory.", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempPath,
                        onValueChange = { tempPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Folder Path") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appTheme.textPrimary,
                            unfocusedTextColor = appTheme.textPrimary,
                            focusedBorderColor = appTheme.accent,
                            unfocusedBorderColor = appTheme.accent.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Quick Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath}/Muesic"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Music", fontSize = 11.sp, color = appTheme.textPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                tempPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}/Muesic"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Downloads", fontSize = 11.sp, color = appTheme.textPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                tempPath = "${context.filesDir.absolutePath}/muesic_backup"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Internal", fontSize = 11.sp, color = appTheme.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showFolderEditDialog = false }) {
                            Text("Cancel", color = appTheme.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                settingsManager.setBackupFolderPath(tempPath.trim())
                                currentBackupFolderPath = settingsManager.getBackupFolderPath()
                                showFolderEditDialog = false
                                isMessageSuccess = true
                                actionMessage = "Backup folder updated to:\n${currentBackupFolderPath}"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent)
                        ) {
                            Text("Save Folder", color = if (appTheme.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ABOUT INFO EDIT DIALOG
    if (showAboutEditDialog) {
        var tempDev by remember { mutableStateOf(aboutInfo.developerName) }
        var tempDate by remember { mutableStateOf(aboutInfo.updatedDate) }
        var tempGithub by remember { mutableStateOf(aboutInfo.githubAccount) }

        Dialog(onDismissRequest = { showAboutEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = appTheme.surfaceElevated,
                border = BorderStroke(1.dp, appTheme.cardBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit About Information", style = MaterialTheme.typography.titleSmall, color = appTheme.textPrimary, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = tempDev,
                        onValueChange = { tempDev = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Developer Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appTheme.textPrimary,
                            unfocusedTextColor = appTheme.textPrimary,
                            focusedBorderColor = appTheme.accent
                        )
                    )

                    OutlinedTextField(
                        value = tempDate,
                        onValueChange = { tempDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Updated Date") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appTheme.textPrimary,
                            unfocusedTextColor = appTheme.textPrimary,
                            focusedBorderColor = appTheme.accent
                        )
                    )

                    OutlinedTextField(
                        value = tempGithub,
                        onValueChange = { tempGithub = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("GitHub Account / URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appTheme.textPrimary,
                            unfocusedTextColor = appTheme.textPrimary,
                            focusedBorderColor = appTheme.accent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                aboutInfo = settingsManager.resetAboutInfoToGit()
                                tempDev = aboutInfo.developerName
                                tempDate = aboutInfo.updatedDate
                                tempGithub = aboutInfo.githubAccount
                                isMessageSuccess = true
                                actionMessage = "Synced with Git commit & push details."
                            }
                        ) {
                            Text("Sync with Git", color = appTheme.accent, fontSize = 12.sp)
                        }

                        Row {
                            OutlinedButton(onClick = { showAboutEditDialog = false }) {
                                Text("Cancel", color = appTheme.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    settingsManager.saveAboutInfo(tempDev.trim(), tempDate.trim(), tempGithub.trim())
                                    aboutInfo = settingsManager.getAboutInfo()
                                    showAboutEditDialog = false
                                    isMessageSuccess = true
                                    actionMessage = "About details updated successfully."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent)
                            ) {
                                Text("Save", color = if (appTheme.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // JSON IMPORT DIALOG
    if (showJsonImportDialog) {
        Dialog(onDismissRequest = { showJsonImportDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = appTheme.surfaceElevated,
                border = BorderStroke(1.dp, appTheme.cardBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Import Config & Playlists", style = MaterialTheme.typography.titleSmall, color = appTheme.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Paste your exported JSON configuration or backup payload below to restore to your library:", style = MaterialTheme.typography.bodySmall, color = appTheme.textSecondary)

                    OutlinedTextField(
                        value = jsonInputText,
                        onValueChange = { jsonInputText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        label = { Text("JSON Content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appTheme.textPrimary,
                            unfocusedTextColor = appTheme.textPrimary,
                            focusedBorderColor = appTheme.accent
                        )
                    )

                    // Also option to load from backup folder if exists
                    val latestBackupFile = remember {
                        val folder = settingsManager.getBackupFolder()
                        folder.listFiles()?.filter { it.name.endsWith(".json") }?.maxByOrNull { it.lastModified() }
                    }

                    if (latestBackupFile != null) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    jsonInputText = latestBackupFile.readText()
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load latest (${latestBackupFile.name})", fontSize = 11.sp, color = appTheme.accent)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showJsonImportDialog = false }) {
                            Text("Cancel", color = appTheme.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = repository.importConfigAndPlaylists(jsonInputText)
                                    isMessageSuccess = res.isSuccess
                                    actionMessage = res.message
                                    showJsonImportDialog = false
                                    // Refresh states
                                    aboutInfo = settingsManager.getAboutInfo()
                                    currentBackupFolderPath = settingsManager.getBackupFolderPath()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent),
                            enabled = jsonInputText.isNotBlank()
                        ) {
                            Text("Restore", color = if (appTheme.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // LICENSE DIALOG
    if (showLicenseDialog) {
        Dialog(onDismissRequest = { showLicenseDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = appTheme.surfaceElevated,
                border = BorderStroke(1.dp, appTheme.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MIT License",
                        style = MaterialTheme.typography.titleMedium,
                        color = appTheme.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Copyright (c) 2026 Muesic Contributors",
                        style = MaterialTheme.typography.labelMedium,
                        color = appTheme.accent,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = appTheme.cardGlass,
                        border = BorderStroke(1.dp, appTheme.divider)
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.",
                                style = MaterialTheme.typography.bodySmall,
                                color = appTheme.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showLicenseDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent)
                        ) {
                            Text(
                                "Close",
                                color = if (appTheme.isLight) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
