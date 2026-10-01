package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.playback.EqualizerState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalAppThemeColors
import kotlin.math.roundToInt

@Composable
fun EqualizerDialog(
    state: EqualizerState,
    onStateChange: (EqualizerState) -> Unit,
    onApplyPreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = appTheme.surfaceGlass,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("equalizer_dialog")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = appTheme.accent
                        )
                        Text(
                            text = "5-Band Equalizer",
                            style = MaterialTheme.typography.titleMedium,
                            color = appTheme.textPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = { onStateChange(state.copy(isEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = appTheme.accent,
                                checkedTrackColor = appTheme.accent.copy(alpha = 0.4f),
                                uncheckedThumbColor = appTheme.textMuted,
                                uncheckedTrackColor = if (appTheme.isLight) Color(0x22000000) else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("equalizer_switch")
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = appTheme.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets Horizontal Row
                Text(
                    text = "Acoustic Presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = appTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EqualizerState.PRESETS.keys.forEach { presetName ->
                        GlassPill(
                            text = presetName,
                            isSelected = state.currentPreset == presetName,
                            onClick = { onApplyPreset(presetName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frequency Curve Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        // Center 0 dB baseline
                        drawLine(
                            color = if (appTheme.isLight) Color(0x22000000) else Color(0x33FFFFFF),
                            start = Offset(0f, midY),
                            end = Offset(w, midY),
                            strokeWidth = 1.dp.toPx()
                        )

                        val bands = listOf(
                            state.band60Hz,
                            state.band230Hz,
                            state.band910Hz,
                            state.band3600Hz,
                            state.band14000Hz
                        )

                        val step = w / 4f
                        val path = Path()
                        bands.forEachIndexed { index, gain ->
                            val x = index * step
                            val normalized = (gain / 12f).coerceIn(-1f, 1f)
                            val y = midY - (normalized * (h * 0.42f))
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                val prevX = (index - 1) * step
                                val prevGain = bands[index - 1]
                                val prevNorm = (prevGain / 12f).coerceIn(-1f, 1f)
                                val prevY = midY - (prevNorm * (h * 0.42f))
                                val cx = (prevX + x) / 2f
                                path.cubicTo(cx, prevY, cx, y, x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = if (state.isEnabled) appTheme.accent else appTheme.textMuted,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Sliders
                data class BandConfig(
                    val frequency: String,
                    val description: String,
                    val gain: Float,
                    val onChange: (Float) -> Unit
                )

                val bandConfigs = listOf(
                    BandConfig("60 Hz", "Sub-Bass", state.band60Hz) { v: Float -> onStateChange(state.copy(band60Hz = v, currentPreset = "Custom")) },
                    BandConfig("230 Hz", "Bass", state.band230Hz) { v: Float -> onStateChange(state.copy(band230Hz = v, currentPreset = "Custom")) },
                    BandConfig("910 Hz", "Mids", state.band910Hz) { v: Float -> onStateChange(state.copy(band910Hz = v, currentPreset = "Custom")) },
                    BandConfig("3.6 kHz", "Treble", state.band3600Hz) { v: Float -> onStateChange(state.copy(band3600Hz = v, currentPreset = "Custom")) },
                    BandConfig("14 kHz", "Air / Brilliance", state.band14000Hz) { v: Float -> onStateChange(state.copy(band14000Hz = v, currentPreset = "Custom")) }
                )

                bandConfigs.forEach { config ->
                    BandSliderItem(
                        frequency = config.frequency,
                        description = config.description,
                        gain = config.gain,
                        enabled = state.isEnabled,
                        onGainChange = config.onChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Enhancements: Bass Boost & 3D Virtualizer
                Text(
                    text = "Acoustic Enhancements",
                    style = MaterialTheme.typography.labelSmall,
                    color = appTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Bass Boost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deep Bass Boost", color = appTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("${state.bassBoost.roundToInt()}%", color = appTheme.accent, style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = state.bassBoost,
                    onValueChange = { onStateChange(state.copy(bassBoost = it, currentPreset = "Custom")) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = appTheme.accent,
                        activeTrackColor = appTheme.accent,
                        inactiveTrackColor = if (appTheme.isLight) Color(0x22000000) else Color(0x33FFFFFF)
                    )
                )

                // 3D Virtualizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3D Spatial Virtualizer", color = appTheme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("${state.virtualizer.roundToInt()}%", color = ElectricViolet, style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = state.virtualizer,
                    onValueChange = { onStateChange(state.copy(virtualizer = it, currentPreset = "Custom")) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricViolet,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = if (appTheme.isLight) Color(0x22000000) else Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onApplyPreset("Hi-Fi Studio Master") }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = appTheme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BandSliderItem(
    frequency: String,
    description: String,
    gain: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit
) {
    val appTheme = LocalAppThemeColors.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = frequency,
                    color = if (enabled) appTheme.textPrimary else appTheme.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "($description)",
                    color = appTheme.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                text = "${if (gain > 0) "+" else ""}${gain.roundToInt()} dB",
                color = if (enabled) appTheme.accent else appTheme.textMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Slider(
            value = gain,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = appTheme.accent,
                activeTrackColor = appTheme.accent,
                inactiveTrackColor = if (appTheme.isLight) Color(0x1A000000) else Color(0x2BFFFFFF)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
