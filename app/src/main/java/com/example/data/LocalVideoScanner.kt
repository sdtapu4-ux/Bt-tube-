package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaPermissionHelper {
    fun requiredVideoPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasVideoStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredVideoPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }
}

data class ImportedDeviceVideo(
    val uriString: String,
    val title: String,
    val durationSeconds: Int
)

object LocalVideoScanner {

    private val videoExtensions = setOf("mp4", "mkv", "webm", "mov", "3gp", "avi", "m4v")

    suspend fun scanAllMediaStoreVideos(
        context: Context,
        maxVideos: Int = 300
    ): List<ImportedDeviceVideo> = withContext(Dispatchers.IO) {
        if (!MediaPermissionHelper.hasVideoStoragePermission(context)) {
            return@withContext emptyList()
        }

        val results = mutableListOf<ImportedDeviceVideo>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext() && results.size < maxVideos) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val mediaTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val rawTitle = mediaTitle?.takeIf { it.isNotBlank() }
                        ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                        ?: "Phone Video"
                    val durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val durationSec = if (durationMs > 0L) {
                        (durationMs / 1000L).toInt().coerceAtLeast(1)
                    } else {
                        extractDurationSeconds(context, contentUri)
                    }

                    results.add(
                        ImportedDeviceVideo(
                            uriString = contentUri.toString(),
                            title = rawTitle,
                            durationSeconds = durationSec
                        )
                    )
                }
            }
        }
        results
    }

    suspend fun extractMetadataFromUris(
        context: Context,
        uris: List<Uri>
    ): List<ImportedDeviceVideo> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            runCatching {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Photo Picker URIs grant temporary read access without persistable permission
                }
                val name = queryDisplayName(context, uri) ?: "Phone Video"
                val cleanTitle = name.substringBeforeLast('.').ifBlank { name }
                val durationSec = extractDurationSeconds(context, uri)
                ImportedDeviceVideo(
                    uriString = uri.toString(),
                    title = cleanTitle,
                    durationSeconds = durationSec
                )
            }.getOrNull()
        }
    }

    suspend fun scanFolderTree(
        context: Context,
        treeUri: Uri,
        maxVideos: Int = 250
    ): List<ImportedDeviceVideo> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        val discovered = mutableListOf<Pair<Uri, String>>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val queue = ArrayDeque<String>()
        queue.add(rootDocId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        while (queue.isNotEmpty() && discovered.size < maxVideos) {
            val parentDocId = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext() && discovered.size < maxVideos) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                        val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Video" else "Video"
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(docId)
                        } else if (isVideoFile(mime, displayName)) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            discovered.add(fileUri to displayName)
                        }
                    }
                }
            }
        }

        discovered.map { (uri, name) ->
            val cleanTitle = name.substringBeforeLast('.').ifBlank { name }
            val durationSec = extractDurationSeconds(context, uri)
            ImportedDeviceVideo(
                uriString = uri.toString(),
                title = cleanTitle,
                durationSeconds = durationSec
            )
        }
    }

    suspend fun loadVideoThumbnailBitmap(
        context: Context,
        uriString: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uriString))
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun isVideoFile(mimeType: String, fileName: String): Boolean {
        if (mimeType.startsWith("video/", ignoreCase = true)) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in videoExtensions
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun extractDurationSeconds(context: Context, uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            (ms / 1000L).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            60
        } finally {
            runCatching { retriever.release() }
        }
    }
}
