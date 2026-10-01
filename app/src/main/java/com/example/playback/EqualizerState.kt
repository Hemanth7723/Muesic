package com.example.playback

data class EqualizerState(
    val isEnabled: Boolean = true,
    val currentPreset: String = "Hi-Fi Studio Master",
    // 5 Bands in dB (-12 to +12)
    val band60Hz: Float = 2.0f,
    val band230Hz: Float = 0.0f,
    val band910Hz: Float = 1.0f,
    val band3600Hz: Float = 2.5f,
    val band14000Hz: Float = 4.0f,
    val bassBoost: Float = 45f, // 0 to 100%
    val virtualizer: Float = 30f // 0 to 100%
) {
    companion object {
        val PRESETS = mapOf(
            "Flat" to EqualizerState(true, "Flat", 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Hi-Fi Studio Master" to EqualizerState(true, "Hi-Fi Studio Master", 2f, 0f, 1f, 2.5f, 4f, 40f, 30f),
            "Bass Boost" to EqualizerState(true, "Bass Boost", 8f, 6f, 0f, 1f, 2f, 85f, 20f),
            "Electronic" to EqualizerState(true, "Electronic", 6f, 4f, -1f, 3f, 5f, 65f, 40f),
            "Rock" to EqualizerState(true, "Rock", 5f, 3f, -1f, 4f, 6f, 50f, 25f),
            "Vocal / Acoustic" to EqualizerState(true, "Vocal / Acoustic", -2f, 1f, 4f, 5f, 3f, 15f, 35f),
            "Lo-Fi Mellow" to EqualizerState(true, "Lo-Fi Mellow", 4f, 3f, 2f, -2f, -4f, 55f, 15f)
        )
    }
}
