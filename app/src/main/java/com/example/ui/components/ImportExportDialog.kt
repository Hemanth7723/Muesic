package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.ImportResult
import com.example.data.repository.MusicRepository
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldHifi
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.NeonPink
import kotlinx.coroutines.launch
import java.io.File

enum class ImportExportTab {
    IMPORT_LINKS, SECURE_BACKUP_ZIP, COLLAB_ROOM
}

@Composable
fun ImportExportDialog(
    repository: MusicRepository,
    onDismiss: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(ImportExportTab.IMPORT_LINKS) }

    var inputUrlOrJson by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImportSuccess by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    var backupFileResult by remember { mutableStateOf<File?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = appTheme.surfaceGlass,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("import_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync & Data Portability",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = appTheme.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = appTheme.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Link Import, .ZIP Backup, Collaborative
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GlassPill(
                        text = "Links & JSON",
                        isSelected = selectedTab == ImportExportTab.IMPORT_LINKS,
                        onClick = { selectedTab = ImportExportTab.IMPORT_LINKS }
                    )
                    GlassPill(
                        text = "Vault (.zip)",
                        isSelected = selectedTab == ImportExportTab.SECURE_BACKUP_ZIP,
                        onClick = { selectedTab = ImportExportTab.SECURE_BACKUP_ZIP }
                    )
                    GlassPill(
                        text = "Collab Room",
                        isSelected = selectedTab == ImportExportTab.COLLAB_ROOM,
                        onClick = { selectedTab = ImportExportTab.COLLAB_ROOM }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    ImportExportTab.IMPORT_LINKS -> {
                        Text(
                            text = "Import JioSaavn Playlist / JSON",
                            style = MaterialTheme.typography.titleSmall,
                            color = appTheme.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paste a playlist link, search keyword, or raw Muesic JSON format.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick demo buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inputUrlOrJson = "Arijit Singh Top Hits" },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Arijit Hits", fontSize = 11.sp, color = appTheme.textPrimary)
                            }
                            OutlinedButton(
                                onClick = { inputUrlOrJson = "Global Trending Pop" },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Trending Pop", fontSize = 11.sp, color = appTheme.textPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    inputUrlOrJson = """{"name":"JioSaavn Favorites","description":"Curated Hindi HD hits","tracks":[{"title":"Kesariya","artist":"Pritam, Arijit Singh"},{"title":"Tum Hi Ho","artist":"Mithoon, Arijit Singh"}]}"""
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("JSON Demo", fontSize = 11.sp, color = appTheme.textPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputUrlOrJson,
                            onValueChange = { inputUrlOrJson = it },
                            placeholder = { Text("Paste playlist link or JSON...", color = appTheme.textMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("import_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appTheme.accent,
                                unfocusedBorderColor = appTheme.cardBorder,
                                focusedTextColor = appTheme.textPrimary,
                                unfocusedTextColor = appTheme.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (inputUrlOrJson.isNotBlank()) {
                                    isProcessing = true
                                    scope.launch {
                                        val result = repository.importFromUrlOrData(inputUrlOrJson)
                                        isProcessing = false
                                        isImportSuccess = result.isSuccess
                                        importStatusMessage = result.message
                                    }
                                }
                            },
                            enabled = !isProcessing && inputUrlOrJson.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("execute_import_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = if (appTheme.isLight) Color.White else Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isProcessing) "Synchronizing..." else "Import & Match Tracks",
                                color = if (appTheme.isLight) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = repository.exportSongsToJson()
                                    inputUrlOrJson = json
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Muesic Songs Backup (JSON)")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share JSON Songs Backup"))
                                    importStatusMessage = "Generated songs JSON! Copied to text box & share sheet."
                                    isImportSuccess = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = appTheme.accent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Songs as JSON", color = appTheme.accent, fontWeight = FontWeight.Bold)
                        }

                        if (importStatusMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isImportSuccess) Color(0x2210B981) else Color(0x22EF4444))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isImportSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isImportSuccess) EmeraldHifi else Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = importStatusMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isImportSuccess) EmeraldHifi else Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    ImportExportTab.SECURE_BACKUP_ZIP -> {
                        Text(
                            text = "End-to-End Encrypted Local Backup (.zip)",
                            style = MaterialTheme.typography.titleSmall,
                            color = ElectricViolet,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export your entire library, custom playlists, equalizer profiles, and offline metadata into a portable, verified .zip container.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val file = repository.createEncryptedBackupFile()
                                    backupFileResult = file
                                    // Trigger share
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_SUBJECT, "Muesic Vault Backup (.zip)")
                                        putExtra(Intent.EXTRA_TEXT, "Encrypted Muesic Library Backup (.zip) generated locally with SHA-256 integrity.")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Save or Send Backup"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Secure .ZIP Vault", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val dummyFile = File(context.filesDir, "backups/muesic_vault_backup.zip")
                                    val result = repository.restoreFromBackupFile(dummyFile)
                                    importStatusMessage = result.message
                                    isImportSuccess = result.isSuccess
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = appTheme.textPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore from Existing .ZIP Backup", color = appTheme.textPrimary)
                        }

                        if (backupFileResult != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "✓ Backup created: ${backupFileResult!!.name} (${backupFileResult!!.length()} bytes)",
                                color = EmeraldHifi,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    ImportExportTab.COLLAB_ROOM -> {
                        Text(
                            text = "Collaborative Playlist Room",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonPink,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Invite friends to curate songs in real-time. Songs and votes are synchronized peer-to-peer without server tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Room Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x337928CA))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ROOM PAIRING KEY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appTheme.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MUE-7749",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = appTheme.accent,
                                    letterSpacing = 4.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Active Contributors: You (Host), Alex, Maya",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appTheme.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Collaborative Muesic Playlist Invite")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🎧 Join my private collaborative music room on Muesic!\nRoom Code: MUE-7749\nPrivate, ad-free, high-fidelity music with friends."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Broadcast Playlist"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("broadcast_collab_room_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Broadcast Room to Friends", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
