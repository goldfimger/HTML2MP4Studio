package com.html2mp4.studio.model

import android.media.MediaCodecInfo
import android.net.Uri

enum class ResolutionPreset(
    val label: String,
    val width: Int,
    val height: Int
) {
    HD_720P("720p HD (1280×720)", 1280, 720),
    FHD_1080P("1080p Full HD (1920×1080)", 1920, 1080),
    VERTICAL_REELS("1080×1920 (Vertikal / Reels)", 1080, 1920),
    SQUARE("1080×1080 (Persegi)", 1080, 1080),
    CUSTOM("Kustom (Lebar × Tinggi)", 1280, 720)
}

enum class CodecProfile(
    val label: String,
    val avcProfile: Int,
    val avcLevel: Int
) {
    BASELINE(
        label = "H.264 Baseline",
        avcProfile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
        avcLevel = MediaCodecInfo.CodecProfileLevel.AVCLevel4
    ),
    MAIN(
        label = "H.264 Main",
        avcProfile = MediaCodecInfo.CodecProfileLevel.AVCProfileMain,
        avcLevel = MediaCodecInfo.CodecProfileLevel.AVCLevel41
    ),
    HIGH(
        label = "H.264 High",
        avcProfile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
        avcLevel = MediaCodecInfo.CodecProfileLevel.AVCLevel42
    )
}

data class VideoConfig(
    val resolutionPreset: ResolutionPreset = ResolutionPreset.HD_720P,
    val customWidth: Int = 1280,
    val customHeight: Int = 720,
    val fps: Int = 30,
    val durationSeconds: Int = 5,
    val bitrateMbps: Int = 8,
    val codecProfile: CodecProfile = CodecProfile.HIGH,
    val backgroundColorHex: String = "#FFFFFF"
) {
    val activeWidth: Int
        get() = if (resolutionPreset == ResolutionPreset.CUSTOM) {
            customWidth.coerceIn(256, 3840).let { if (it % 2 == 0) it else it - 1 }
        } else {
            resolutionPreset.width
        }

    val activeHeight: Int
        get() = if (resolutionPreset == ResolutionPreset.CUSTOM) {
            customHeight.coerceIn(256, 3840).let { if (it % 2 == 0) it else it - 1 }
        } else {
            resolutionPreset.height
        }

    val aspectRatio: Float
        get() = activeWidth.toFloat() / activeHeight.toFloat().coerceAtLeast(1f)
}

sealed interface RenderStatus {
    data object Idle : RenderStatus
    data class Rendering(
        val progressPercent: Int,
        val currentFrame: Int,
        val totalFrames: Int,
        val estimatedRemainingSeconds: Int
    ) : RenderStatus

    data class Success(
        val outputUri: Uri,
        val fileName: String,
        val fileSizeBytes: Long
    ) : RenderStatus

    data class Error(val message: String) : RenderStatus
}
