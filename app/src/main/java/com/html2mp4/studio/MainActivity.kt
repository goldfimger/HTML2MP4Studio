package com.html2mp4.studio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.html2mp4.studio.ui.StudioScreen
import com.html2mp4.studio.ui.StudioViewModel
import com.html2mp4.studio.ui.theme.HTML2MP4StudioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* MediaStore menangani izin secara otomatis pada Android 10+ */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        setContent {
            HTML2MP4StudioTheme {
                StudioScreen(viewModel = viewModel)
            }
        }
    }
}
