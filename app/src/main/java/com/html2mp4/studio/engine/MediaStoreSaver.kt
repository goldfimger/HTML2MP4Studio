package com.html2mp4.studio.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedVideoResult(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long
)

object MediaStoreSaver {

    suspend fun saveMp4ToMoviesGallery(
        context: Context,
        tempMp4File: File
    ): SavedVideoResult = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "HTML2MP4_$timestamp.mp4"
        val fileSize = tempMp4File.length()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/HTML2MP4"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Gagal membuat entri MediaStore untuk video MP4.")

            try {
                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    FileInputStream(tempMp4File).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IllegalStateException("Gagal membuka OutputStream MediaStore.")

                val updateValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, updateValues, null, null)
                SavedVideoResult(uri = itemUri, fileName = fileName, fileSizeBytes = fileSize)
            } catch (t: Throwable) {
                resolver.delete(itemUri, null, null)
                throw t
            }
        } else {
            @Suppress("DEPRECATION")
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val targetDir = File(moviesDir, "HTML2MP4").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(targetDir, fileName)
            tempMp4File.copyTo(destFile, overwrite = true)

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )

            SavedVideoResult(
                uri = Uri.fromFile(destFile),
                fileName = fileName,
                fileSizeBytes = destFile.length()
            )
        }
    }
}
