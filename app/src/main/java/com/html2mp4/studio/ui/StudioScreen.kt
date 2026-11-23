package com.html2mp4.studio.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.html2mp4.studio.model.CodecProfile
import com.html2mp4.studio.model.RenderStatus
import com.html2mp4.studio.model.ResolutionPreset
import com.html2mp4.studio.model.VideoConfig
import com.html2mp4.studio.ui.theme.StudioBackground
import com.html2mp4.studio.ui.theme.StudioBorder
import com.html2mp4.studio.ui.theme.StudioMuted
import com.html2mp4.studio.ui.theme.StudioOnSurface
import com.html2mp4.studio.ui.theme.StudioPrimary
import com.html2mp4.studio.ui.theme.StudioSuccess
import com.html2mp4.studio.ui.theme.StudioSurface
import com.html2mp4.studio.ui.theme.StudioSurfaceVariant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val htmlCode by viewModel.htmlCode.collectAsState()
    val videoConfig by viewModel.videoConfig.collectAsState()
    val renderStatus by viewModel.renderStatus.collectAsState()

    val context = LocalContext.current
    val rootView = LocalView.current.rootView as? ViewGroup

    var selectedTab by remember { mutableIntStateOf(0) }

    val openHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadHtmlFromUri(uri)
        }
    }

    Scaffold(
        containerColor = StudioBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HTML2MP4 Studio",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface,
                    titleContentColor = StudioOnSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = StudioSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    HorizontalDivider(color = StudioBorder, thickness = 1.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startRender(rootView) },
                            enabled = renderStatus !is RenderStatus.Rendering,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Render ke MP4",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Bagian Atas: Area Pratinjau Langsung
            PreviewSection(
                htmlCode = htmlCode,
                videoConfig = videoConfig,
                onLoadSampleClick = { viewModel.loadSampleAnimationHtml() },
                onPickHtmlFileClick = {
                    openHtmlLauncher.launch(arrayOf("text/html", "text/plain", "*/*"))
                }
            )

            // 2. Bagian Tengah: Tab Pengaturan (2 Tab)
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = StudioSurface,
                        contentColor = StudioOnSurface,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = StudioPrimary,
                                height = 3.dp
                            )
                        },
                        divider = {
                            HorizontalDivider(color = StudioBorder)
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Editor Kode", maxLines = 1) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            selectedContentColor = StudioPrimary,
                            unselectedContentColor = StudioMuted
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Tuning Video", maxLines = 1) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            selectedContentColor = StudioPrimary,
                            unselectedContentColor = StudioMuted
                        )
                    }

                    when (selectedTab) {
                        0 -> CodeEditorTab(
                            htmlCode = htmlCode,
                            onHtmlCodeChange = { viewModel.updateHtmlCode(it) }
                        )
                        1 -> VideoTuningTab(
                            config = videoConfig,
                            onPresetSelected = viewModel::setResolutionPreset,
                            onCustomDimensionsChange = viewModel::setCustomDimensions,
                            onFpsSelected = viewModel::setFps,
                            onDurationChange = viewModel::setDurationSeconds,
                            onBitrateChange = viewModel::setBitrateMbps,
                            onCodecProfileSelected = viewModel::setCodecProfile,
                            onBackgroundHexChange = viewModel::setBackgroundColorHex
                        )
                    }
                }
            }
        }
    }

    // Dialog Progress / Sukses / Error
    when (val status = renderStatus) {
        is RenderStatus.Rendering -> {
            AlertDialog(
                onDismissRequest = { /* Tidak dapat ditutup sembarangan saat render berjalan */ },
                containerColor = StudioSurface,
                titleContentColor = StudioOnSurface,
                textContentColor = StudioOnSurface,
                title = {
                    Text(
                        text = "Merender Video MP4...",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LinearProgressIndicator(
                            progress = { status.progressPercent / 100f },
                            color = StudioPrimary,
                            trackColor = StudioSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${status.progressPercent}%",
                                fontWeight = FontWeight.Bold,
                                color = StudioPrimary
                            )
                            Text(
                                text = "Frame ${status.currentFrame}/${status.totalFrames}",
                                color = StudioMuted,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Estimasi sisa waktu: ${status.estimatedRemainingSeconds} detik",
                            color = StudioMuted,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.cancelRender() }) {
                        Text("Batalkan", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        is RenderStatus.Success -> {
            val fileSizeMb = String.format("%.2f MB", status.fileSizeBytes / (1024.0 * 1024.0))
            AlertDialog(
                onDismissRequest = { viewModel.dismissStatusDialog() },
                containerColor = StudioSurface,
                titleContentColor = StudioOnSurface,
                textContentColor = StudioOnSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StudioSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Render Selesai!",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Video berhasil disimpan ke Galeri (Movies/HTML2MP4).")
                        Text(
                            text = "${status.fileName} ($fileSizeMb)",
                            fontSize = 13.sp,
                            color = StudioMuted
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, status.outputUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Bagikan Video MP4")
                                )
                            },
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan")
                        }
                        Button(
                            onClick = {
                                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(status.outputUri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(playIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Putar Video")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissStatusDialog() }) {
                        Text("Tutup", color = StudioMuted)
                    }
                }
            )
        }

        is RenderStatus.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissStatusDialog() },
                containerColor = StudioSurface,
                titleContentColor = StudioOnSurface,
                textContentColor = StudioOnSurface,
                title = { Text("Gagal Merender") },
                text = { Text(status.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissStatusDialog() }) {
                        Text("Tutup", color = StudioPrimary)
                    }
                }
            )
        }

        RenderStatus.Idle -> Unit
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PreviewSection(
    htmlCode: String,
    videoConfig: VideoConfig,
    onLoadSampleClick: () -> Unit,
    onPickHtmlFileClick: () -> Unit
) {
    val parsedBg = remember(videoConfig.backgroundColorHex) {
        try {
            AndroidColor.parseColor(videoConfig.backgroundColorHex.trim())
        } catch (_: Exception) {
            AndroidColor.WHITE
        }
    }

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pratinjau Langsung",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${videoConfig.activeWidth}×${videoConfig.activeHeight} · ${videoConfig.fps} fps · ${videoConfig.durationSeconds}d",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Kotak aspek rasio proporsional
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 280.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090E1A))
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(videoConfig.aspectRatio.coerceIn(0.4f, 2.5f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(parsedBg))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { webView ->
                            webView.setBackgroundColor(parsedBg)
                            webView.loadDataWithBaseURL(
                                "https://localhost/",
                                htmlCode,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onLoadSampleClick,
                    border = BorderStroke(1.dp, StudioPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = StudioPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Muat Contoh Animasi HTML",
                        color = StudioOnSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPickHtmlFileClick,
                    border = BorderStroke(1.dp, StudioBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = StudioMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Muat File .html",
                        color = StudioOnSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeEditorTab(
    htmlCode: String,
    onHtmlCodeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Kode HTML / CSS / JavaScript Lengkap",
            style = MaterialTheme.typography.labelMedium
        )
        OutlinedTextField(
            value = htmlCode,
            onValueChange = onHtmlCodeChange,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = StudioOnSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0B1120),
                unfocusedContainerColor = Color(0xFF0B1120),
                focusedBorderColor = StudioPrimary,
                unfocusedBorderColor = StudioBorder
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 380.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoTuningTab(
    config: VideoConfig,
    onPresetSelected: (ResolutionPreset) -> Unit,
    onCustomDimensionsChange: (Int, Int) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onCodecProfileSelected: (CodecProfile) -> Unit,
    onBackgroundHexChange: (String) -> Unit
) {
    var resolutionExpanded by remember { mutableStateOf(false) }
    var codecExpanded by remember { mutableStateOf(false) }

    val colorSwatches = listOf("#FFFFFF", "#0F172A", "#000000", "#1E293B", "#3B82F6", "#10B981")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Resolusi Video
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resolusi Video", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = resolutionExpanded,
                onExpandedChange = { resolutionExpanded = !resolutionExpanded }
            ) {
                OutlinedTextField(
                    value = config.resolutionPreset.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioPrimary,
                        unfocusedBorderColor = StudioBorder,
                        focusedContainerColor = StudioBackground,
                        unfocusedContainerColor = StudioBackground
                    ),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = resolutionExpanded,
                    onDismissRequest = { resolutionExpanded = false },
                    containerColor = StudioSurface
                ) {
                    ResolutionPreset.entries.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.label, color = StudioOnSurface) },
                            onClick = {
                                onPresetSelected(preset)
                                resolutionExpanded = false
                            }
                        )
                    }
                }
            }

            if (config.resolutionPreset == ResolutionPreset.CUSTOM) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = config.customWidth.toString(),
                        onValueChange = { raw ->
                            raw.toIntOrNull()?.let { w ->
                                onCustomDimensionsChange(w, config.customHeight)
                            }
                        },
                        label = { Text("Lebar (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = config.customHeight.toString(),
                        onValueChange = { raw ->
                            raw.toIntOrNull()?.let { h ->
                                onCustomDimensionsChange(config.customWidth, h)
                            }
                        },
                        label = { Text("Tinggi (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Frame Rate / FPS (Segmented Button)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Frame Rate (FPS)", style = MaterialTheme.typography.labelMedium)
            val fpsOptions = listOf(24, 30, 60)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                fpsOptions.forEachIndexed { index, fps ->
                    SegmentedButton(
                        selected = config.fps == fps,
                        onClick = { onFpsSelected(fps) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = fpsOptions.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = StudioPrimary,
                            activeContentColor = Color.White,
                            inactiveContainerColor = StudioBackground,
                            inactiveContentColor = StudioMuted
                        )
                    ) {
                        Text("$fps fps")
                    }
                }
            }
        }

        // 3. Durasi Render (detik)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Durasi Render (detik)", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = config.durationSeconds.toString(),
                    onValueChange = { raw ->
                        raw.toIntOrNull()?.let(onDurationChange)
                    },
                    suffix = { Text("detik", color = StudioMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                listOf(5, 10, 30).forEach { presetSec ->
                    OutlinedButton(
                        onClick = { onDurationChange(presetSec) },
                        border = BorderStroke(
                            1.dp,
                            if (config.durationSeconds == presetSec) StudioPrimary else StudioBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${presetSec}d",
                            color = if (config.durationSeconds == presetSec) StudioPrimary else StudioOnSurface
                        )
                    }
                }
            }
        }

        // 4. Kualitas / Bitrate (1 - 20 Mbps)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kualitas / Bitrate", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${config.bitrateMbps} Mbps",
                    color = StudioPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            Slider(
                value = config.bitrateMbps.toFloat(),
                onValueChange = { onBitrateChange(it.roundToInt()) },
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = StudioPrimary,
                    activeTrackColor = StudioPrimary,
                    inactiveTrackColor = StudioSurfaceVariant
                )
            )
        }

        // 5. Video Codec Profile
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Profil Codec Video", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(
                expanded = codecExpanded,
                onExpandedChange = { codecExpanded = !codecExpanded }
            ) {
                OutlinedTextField(
                    value = config.codecProfile.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = codecExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioPrimary,
                        unfocusedBorderColor = StudioBorder,
                        focusedContainerColor = StudioBackground,
                        unfocusedContainerColor = StudioBackground
                    ),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = codecExpanded,
                    onDismissRequest = { codecExpanded = false },
                    containerColor = StudioSurface
                ) {
                    CodecProfile.entries.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.label, color = StudioOnSurface) },
                            onClick = {
                                onCodecProfileSelected(profile)
                                codecExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 6. Warna Latar Belakang Dasar (Color Picker + Input Hex)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Warna Latar Belakang Dasar", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = config.backgroundColorHex,
                    onValueChange = onBackgroundHexChange,
                    singleLine = true,
                    placeholder = { Text("#FFFFFF") },
                    modifier = Modifier.weight(1f)
                )
                colorSwatches.forEach { hex ->
                    val swatchColor = Color(AndroidColor.parseColor(hex))
                    val isSelected = config.backgroundColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) StudioPrimary else StudioBorder,
                                shape = CircleShape
                            )
                            .clickable { onBackgroundHexChange(hex) }
                    )
                }
            }
        }
    }
}
