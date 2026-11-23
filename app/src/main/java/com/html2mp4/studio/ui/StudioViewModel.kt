package com.html2mp4.studio.ui

import android.app.Application
import android.net.Uri
import android.view.ViewGroup
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.html2mp4.studio.engine.HtmlToMp4Encoder
import com.html2mp4.studio.engine.MediaStoreSaver
import com.html2mp4.studio.model.CodecProfile
import com.html2mp4.studio.model.RenderStatus
import com.html2mp4.studio.model.ResolutionPreset
import com.html2mp4.studio.model.VideoConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val encoder = HtmlToMp4Encoder(application.applicationContext)
    private var renderJob: Job? = null

    private val _htmlCode = MutableStateFlow(DEFAULT_SAMPLE_HTML)
    val htmlCode: StateFlow<String> = _htmlCode.asStateFlow()

    private val _videoConfig = MutableStateFlow(VideoConfig())
    val videoConfig: StateFlow<VideoConfig> = _videoConfig.asStateFlow()

    private val _renderStatus = MutableStateFlow<RenderStatus>(RenderStatus.Idle)
    val renderStatus: StateFlow<RenderStatus> = _renderStatus.asStateFlow()

    fun updateHtmlCode(newCode: String) {
        _htmlCode.value = newCode
    }

    fun loadSampleAnimationHtml() {
        _htmlCode.value = DEFAULT_SAMPLE_HTML
    }

    fun loadHtmlFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).readText()
                    } ?: ""
                }
                if (content.isNotBlank()) {
                    _htmlCode.value = content
                }
            } catch (e: Exception) {
                _renderStatus.value = RenderStatus.Error(
                    "Gagal membaca file HTML: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}"
                )
            }
        }
    }

    fun setResolutionPreset(preset: ResolutionPreset) {
        _videoConfig.update { it.copy(resolutionPreset = preset) }
    }

    fun setCustomDimensions(width: Int, height: Int) {
        _videoConfig.update {
            it.copy(
                customWidth = width.coerceIn(128, 3840),
                customHeight = height.coerceIn(128, 3840)
            )
        }
    }

    fun setFps(fps: Int) {
        _videoConfig.update { it.copy(fps = fps) }
    }

    fun setDurationSeconds(seconds: Int) {
        _videoConfig.update { it.copy(durationSeconds = seconds.coerceIn(1, 300)) }
    }

    fun setBitrateMbps(mbps: Int) {
        _videoConfig.update { it.copy(bitrateMbps = mbps.coerceIn(1, 20)) }
    }

    fun setCodecProfile(profile: CodecProfile) {
        _videoConfig.update { it.copy(codecProfile = profile) }
    }

    fun setBackgroundColorHex(hex: String) {
        _videoConfig.update { it.copy(backgroundColorHex = hex) }
    }

    fun startRender(hostRootView: ViewGroup?) {
        if (_renderStatus.value is RenderStatus.Rendering) return

        val currentHtml = _htmlCode.value
        val currentConfig = _videoConfig.value
        val context = getApplication<Application>().applicationContext
        val tempFile = File(context.cacheDir, "render_temp_${System.currentTimeMillis()}.mp4")

        renderJob = viewModelScope.launch(Dispatchers.Default) {
            _renderStatus.value = RenderStatus.Rendering(
                progressPercent = 0,
                currentFrame = 0,
                totalFrames = currentConfig.fps * currentConfig.durationSeconds,
                estimatedRemainingSeconds = currentConfig.durationSeconds
            )

            try {
                encoder.renderHtmlToMp4(
                    htmlContent = currentHtml,
                    config = currentConfig,
                    outputFile = tempFile,
                    hostRootView = hostRootView
                ) { progress ->
                    _renderStatus.value = RenderStatus.Rendering(
                        progressPercent = progress.progressPercent,
                        currentFrame = progress.currentFrame,
                        totalFrames = progress.totalFrames,
                        estimatedRemainingSeconds = progress.estimatedRemainingSeconds
                    )
                }

                val savedResult = MediaStoreSaver.saveMp4ToMoviesGallery(context, tempFile)
                tempFile.delete()

                _renderStatus.value = RenderStatus.Success(
                    outputUri = savedResult.uri,
                    fileName = savedResult.fileName,
                    fileSizeBytes = savedResult.fileSizeBytes
                )
            } catch (_: CancellationException) {
                if (tempFile.exists()) tempFile.delete()
                _renderStatus.value = RenderStatus.Idle
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                _renderStatus.value = RenderStatus.Error(
                    "Proses render gagal: ${e.localizedMessage ?: "Terjadi kesalahan pada MediaCodec"}"
                )
            }
        }
    }

    fun cancelRender() {
        renderJob?.cancel()
        renderJob = null
        _renderStatus.value = RenderStatus.Idle
    }

    fun dismissStatusDialog() {
        _renderStatus.value = RenderStatus.Idle
    }

    companion object {
        val DEFAULT_SAMPLE_HTML = """
            <!DOCTYPE html>
            <html lang="id">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  width: 100vw;
                  height: 100vh;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  background: radial-gradient(circle at 25% 25%, #1e3a8a, #0f172a 70%);
                  font-family: sans-serif;
                  overflow: hidden;
                  color: #f8fafc;
                }
                .orbit {
                  position: relative;
                  width: 340px;
                  height: 340px;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                }
                .ring {
                  position: absolute;
                  inset: 0;
                  border-radius: 50%;
                  border: 4px solid rgba(59, 130, 246, 0.35);
                  border-top-color: #3b82f6;
                  animation: spin 3s linear infinite;
                }
                .ring-inner {
                  position: absolute;
                  inset: 34px;
                  border-radius: 50%;
                  border: 3px dashed rgba(96, 165, 250, 0.5);
                  animation: spinReverse 5s linear infinite;
                }
                .card {
                  text-align: center;
                  padding: 28px 36px;
                  border-radius: 20px;
                  background: rgba(30, 41, 59, 0.85);
                  border: 1px solid rgba(148, 163, 184, 0.25);
                  animation: pulseFloat 2.4s ease-in-out infinite;
                }
                .title {
                  font-size: 28px;
                  font-weight: 800;
                  letter-spacing: -0.02em;
                  color: #ffffff;
                }
                .subtitle {
                  margin-top: 8px;
                  font-size: 14px;
                  color: #93c5fd;
                }
                @keyframes spin {
                  to { transform: rotate(360deg); }
                }
                @keyframes spinReverse {
                  to { transform: rotate(-360deg); }
                }
                @keyframes pulseFloat {
                  0%, 100% { transform: translateY(0px) scale(1); }
                  50% { transform: translateY(-10px) scale(1.03); }
                }
              </style>
            </head>
            <body>
              <div class="orbit">
                <div class="ring"></div>
                <div class="ring-inner"></div>
                <div class="card">
                  <div class="title">HTML2MP4</div>
                  <div class="subtitle">Animasi CSS &amp; JS Offline</div>
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
