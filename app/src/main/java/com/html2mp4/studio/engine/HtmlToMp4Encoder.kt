package com.html2mp4.studio.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.html2mp4.studio.model.VideoConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class RenderProgress(
    val progressPercent: Int,
    val currentFrame: Int,
    val totalFrames: Int,
    val estimatedRemainingSeconds: Int
)

class HtmlToMp4Encoder(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun renderHtmlToMp4(
        htmlContent: String,
        config: VideoConfig,
        outputFile: File,
        hostRootView: ViewGroup? = null,
        onProgress: (RenderProgress) -> Unit
    ) {
        val width = config.activeWidth
        val height = config.activeHeight
        val fps = config.fps.coerceIn(1, 60)
        val durationSec = config.durationSeconds.coerceIn(1, 300)
        val totalFrames = fps * durationSec
        val frameIntervalMs = 1000L / fps
        val frameDurationUs = 1_000_000L / fps

        val parsedBgColor = try {
            Color.parseColor(config.backgroundColorHex.trim())
        } catch (_: IllegalArgumentException) {
            Color.WHITE
        }

        var offscreenWebView: WebView? = null
        var offscreenContainer: FrameLayout? = null
        var encoder: MediaCodec? = null
        var inputSurface: Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            // 1. Siapkan WebView pada Main Thread agar JavaScript, DOM Storage, dan CSS animasi berjalan penuh
            val webView = withContext(Dispatchers.Main) {
                createAndLoadWebView(
                    htmlContent = htmlContent,
                    width = width,
                    height = height,
                    backgroundColor = parsedBgColor,
                    hostRootView = hostRootView
                ).also { (wv, container) ->
                    offscreenWebView = wv
                    offscreenContainer = container
                }.first
            }

            // Beri jeda singkat agar halaman & animasi CSS/JS awal ter-layout sempurna
            delay(250L)

            // 2. Konfigurasi MediaCodec H.264/AVC & MediaMuxer
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitrateMbps * 1_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_PROFILE, config.codecProfile.avcProfile)
                setInteger(MediaFormat.KEY_LEVEL, config.codecProfile.avcLevel)
            }

            val createdEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            try {
                createdEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (_: Exception) {
                // Fallback jika perangkat tidak mendukung profil High/Main tertentu
                val fallbackFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, config.bitrateMbps * 1_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                createdEncoder.configure(fallbackFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            encoder = createdEncoder
            inputSurface = createdEncoder.createInputSurface()
            createdEncoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            val bufferInfo = MediaCodec.BufferInfo()
            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val bitmapCanvas = Canvas(frameBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            val startTimeMs = System.currentTimeMillis()

            // 3. Tangkap frame secara sinkron sesuai FPS dan durasi
            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val loopStartMs = System.currentTimeMillis()

                // Tangkap visual WebView saat ini ke Bitmap di Main Thread
                withContext(Dispatchers.Main) {
                    bitmapCanvas.drawColor(parsedBgColor)
                    webView.draw(bitmapCanvas)
                }

                // Kirim frame Bitmap ke Surface input MediaCodec
                val surface = inputSurface ?: break
                val surfaceCanvas = surface.lockHardwareCanvas()
                try {
                    surfaceCanvas.drawBitmap(frameBitmap, 0f, 0f, paint)
                } finally {
                    surface.unlockCanvasAndPost(surfaceCanvas)
                }

                // Drain paket ter-encode ke MediaMuxer
                val drainResult = drainEncoder(
                    encoder = createdEncoder,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    trackIndex = trackIndex,
                    muxerStarted = muxerStarted,
                    endOfStream = false,
                    presentationTimeUsOverride = frameIdx * frameDurationUs
                )
                trackIndex = drainResult.first
                muxerStarted = drainResult.second

                // Hitung persentase & estimasi sisa waktu
                val completed = frameIdx + 1
                val percent = ((completed.toFloat() / totalFrames.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000.0).coerceAtLeast(0.1)
                val framesPerSecMeasured = completed / elapsedSec
                val remainingFrames = (totalFrames - completed).coerceAtLeast(0)
                val etaSeconds = if (framesPerSecMeasured > 0.0) {
                    (remainingFrames / framesPerSecMeasured).roundToInt()
                } else {
                    0
                }

                onProgress(
                    RenderProgress(
                        progressPercent = percent,
                        currentFrame = completed,
                        totalFrames = totalFrames,
                        estimatedRemainingSeconds = etaSeconds
                    )
                )

                // Sinkronisasi waktu antar frame agar animasi CSS/JS berjalan pada kecepatan FPS riil
                val renderCostMs = System.currentTimeMillis() - loopStartMs
                val sleepMs = (frameIntervalMs - renderCostMs).coerceAtLeast(1L)
                delay(sleepMs)
            }

            // 4. Akhiri stream MediaCodec & flush frame terakhir
            createdEncoder.signalEndOfInputStream()
            val finalDrain = drainEncoder(
                encoder = createdEncoder,
                muxer = muxer,
                bufferInfo = bufferInfo,
                trackIndex = trackIndex,
                muxerStarted = muxerStarted,
                endOfStream = true,
                presentationTimeUsOverride = totalFrames * frameDurationUs
            )
            muxerStarted = finalDrain.second

            frameBitmap.recycle()
        } catch (ce: CancellationException) {
            if (outputFile.exists()) outputFile.delete()
            throw ce
        } finally {
            withContext(Dispatchers.Main) {
                offscreenWebView?.apply {
                    stopLoading()
                    destroy()
                }
                offscreenContainer?.let { container ->
                    (container.parent as? ViewGroup)?.removeView(container)
                }
            }
            try {
                encoder?.stop()
            } catch (_: Exception) {
            }
            try {
                encoder?.release()
            } catch (_: Exception) {
            }
            try {
                inputSurface?.release()
            } catch (_: Exception) {
            }
            try {
                if (muxerStarted) muxer?.stop()
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Utilitas opsional PixelCopy untuk menangkap Surface/Window dengan akselerasi GPU (API 26+).
     */
    suspend fun captureViewWithPixelCopy(
        window: android.view.Window,
        targetBitmap: Bitmap
    ): Boolean = suspendCancellableCoroutine { cont ->
        PixelCopy.request(
            window,
            targetBitmap,
            { copyResult ->
                if (cont.isActive) {
                    cont.resume(copyResult == PixelCopy.SUCCESS)
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun createAndLoadWebView(
        htmlContent: String,
        width: Int,
        height: Int,
        backgroundColor: Int,
        hostRootView: ViewGroup?
    ): Pair<WebView, FrameLayout?> = suspendCancellableCoroutine { cont ->
        val webView = WebView(context).apply {
            setBackgroundColor(backgroundColor)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            layoutParams = FrameLayout.LayoutParams(width, height)
        }

        var container: FrameLayout? = null
        if (hostRootView != null) {
            container = FrameLayout(context).apply {
                alpha = 0.001f
                translationX = -20000f
                addView(webView)
            }
            hostRootView.addView(container, ViewGroup.LayoutParams(width, height))
        }

        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        webView.measure(widthSpec, heightSpec)
        webView.layout(0, 0, width, height)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.measure(widthSpec, heightSpec)
                webView.layout(0, 0, width, height)
                if (cont.isActive) {
                    cont.resume(webView to container)
                }
            }
        }

        webView.loadDataWithBaseURL(
            "https://localhost/",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer?,
        bufferInfo: MediaCodec.BufferInfo,
        trackIndex: Int,
        muxerStarted: Boolean,
        endOfStream: Boolean,
        presentationTimeUsOverride: Long
    ): Pair<Int, Boolean> {
        var currentTrackIndex = trackIndex
        var isMuxerStarted = muxerStarted
        val timeoutUs = if (endOfStream) 10_000L else 0L

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }

                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (isMuxerStarted) {
                        throw IllegalStateException("Format output encoder berubah setelah muxer dimulai.")
                    }
                    val newFormat = encoder.outputFormat
                    currentTrackIndex = muxer?.addTrack(newFormat) ?: -1
                    muxer?.start()
                    isMuxerStarted = true
                }

                outputIndex >= 0 -> {
                    val encodedData: ByteBuffer? = encoder.getOutputBuffer(outputIndex)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0 && isMuxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            if (bufferInfo.presentationTimeUs <= 0L) {
                                bufferInfo.presentationTimeUs = presentationTimeUsOverride
                            }
                            muxer?.writeSampleData(currentTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }

        return currentTrackIndex to isMuxerStarted
    }
}
