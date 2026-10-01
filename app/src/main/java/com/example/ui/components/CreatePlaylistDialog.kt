package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalAppThemeColors

@Composable
fun CreatePlaylistDialog(
    onCreate: (title: String, description: String, isCollab: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCollaborative by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = appTheme.surfaceGlass,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("create_playlist_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = appTheme.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = appTheme.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("e.g. Midnight Cyber Vibes", color = appTheme.textMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.accent,
                        unfocusedBorderColor = appTheme.cardBorder,
                        focusedTextColor = appTheme.textPrimary,
                        unfocusedTextColor = appTheme.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Optional description", color = appTheme.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.accent,
                        unfocusedBorderColor = appTheme.cardBorder,
                        focusedTextColor = appTheme.textPrimary,
                        unfocusedTextColor = appTheme.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Collaborative Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = appTheme.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Generates a real-time peer code for friends to add songs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )
                    }
                    Switch(
                        checked = isCollaborative,
                        onCheckedChange = { isCollaborative = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricViolet,
                            checkedTrackColor = Color(0x667928CA)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreate(title.trim(), description.trim(), isCollaborative)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_create_playlist_button")
                ) {
                    Text("Create Playlist", color = if (appTheme.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
