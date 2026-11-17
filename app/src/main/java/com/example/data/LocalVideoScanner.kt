package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    fun requiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun requiredMediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun hasVideoStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredVideoPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredAudioPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAnyMediaPermission(context: Context): Boolean {
        return hasVideoStoragePermission(context) || hasAudioStoragePermission(context)
    }

    fun hasAllMediaPermissions(context: Context): Boolean {
        return hasVideoStoragePermission(context) && hasAudioStoragePermission(context)
    }
}

enum class DeviceMediaType {
    VIDEO,
    AUDIO
}

data class ImportedDeviceVideo(
    val uriString: String,
    val title: String,
    val durationSeconds: Int,
    val mediaType: DeviceMediaType = DeviceMediaType.VIDEO,
    val artistOrFolder: String = "",
    val albumArtUri: String? = null,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val qualityLabel: String = ""
)

object LocalVideoScanner {

    private val videoExtensions = setOf(
        "mp4", "3gp", "3gpp", "3g2", "mkv", "webm", "mov", "avi", "m4v",
        "flv", "wmv", "ts", "mts", "m2ts", "mpg", "mpeg", "vob", "divx", "f4v", "rmvb", "asf"
    )
    private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "wma")

    fun classifyQualityLabel(width: Int, height: Int): String {
        val shortSide = minOf(width, height).takeIf { it > 0 } ?: maxOf(width, height)
        return when {
            shortSide <= 0 -> "SD"
            shortSide <= 180 -> "144p SD"
            shortSide <= 280 -> "240p SD"
            shortSide <= 400 -> "360p SD"
            shortSide <= 540 -> "480p SD"
            shortSide <= 800 -> "720p HD"
            shortSide <= 1200 -> "1080p HD"
            else -> "4K UHD"
        }
    }

    suspend fun scanAllMediaStoreVideos(
        context: Context,
        maxVideos: Int = 300
    ): List<ImportedDeviceVideo> = scanAllDeviceMedia(context, maxItems = maxVideos)

    suspend fun scanAllDeviceMedia(
        context: Context,
        maxItems: Int = 500
    ): List<ImportedDeviceVideo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ImportedDeviceVideo>()

        if (MediaPermissionHelper.hasVideoStoragePermission(context)) {
            results += scanMediaStoreVideosInternal(context, maxItems / 2)
        }
        if (MediaPermissionHelper.hasAudioStoragePermission(context)) {
            results += scanMediaStoreAudioInternal(context, maxItems - results.size)
        }

        results
    }

    private fun scanMediaStoreVideosInternal(
        context: Context,
        maxVideos: Int
    ): List<ImportedDeviceVideo> {
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
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
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
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext() && results.size < maxVideos) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val mediaTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val rawTitle = mediaTitle?.takeIf { it.isNotBlank() }
                        ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                        ?: "Phone Video"
                    val durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val meta = extractVideoMetadata(context, contentUri, durationMs <= 0L)
                    val durationSec = if (durationMs > 0L) {
                        (durationMs / 1000L).toInt().coerceAtLeast(1)
                    } else {
                        meta.durationSeconds
                    }
                    val w = if (widthCol >= 0) cursor.getInt(widthCol) else meta.widthPx
                    val h = if (heightCol >= 0) cursor.getInt(heightCol) else meta.heightPx
                    val resolvedW = if (w > 0) w else meta.widthPx
                    val resolvedH = if (h > 0) h else meta.heightPx
                    val qLabel = classifyQualityLabel(resolvedW, resolvedH)

                    results.add(
                        ImportedDeviceVideo(
                            uriString = contentUri.toString(),
                            title = rawTitle,
                            durationSeconds = durationSec,
                            mediaType = DeviceMediaType.VIDEO,
                            widthPx = resolvedW,
                            heightPx = resolvedH,
                            qualityLabel = qLabel
                        )
                    )
                }
            }
        }
        return results
    }

    private fun scanMediaStoreAudioInternal(
        context: Context,
        maxAudio: Int
    ): List<ImportedDeviceVideo> {
        if (maxAudio <= 0) return emptyList()
        val results = mutableListOf<ImportedDeviceVideo>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("3000")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext() && results.size < maxAudio) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val mediaTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val rawTitle = mediaTitle?.takeIf { it.isNotBlank() }
                        ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                        ?: "Audio Track"
                    val rawArtist = if (artistCol >= 0) cursor.getString(artistCol) else null
                    val artist = rawArtist?.takeIf {
                        it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
                    } ?: "Device Audio"
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
                    val albumArtUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                    } else {
                        null
                    }
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
                            durationSeconds = durationSec,
                            mediaType = DeviceMediaType.AUDIO,
                            artistOrFolder = artist,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        }
        return results
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
                val name = queryDisplayName(context, uri) ?: "Device Media"
                val cleanTitle = name.substringBeforeLast('.').ifBlank { name }
                val mime = context.contentResolver.getType(uri).orEmpty()
                val mediaType = if (isAudioFile(mime, name)) {
                    DeviceMediaType.AUDIO
                } else {
                    DeviceMediaType.VIDEO
                }
                val meta = if (mediaType == DeviceMediaType.VIDEO) {
                    extractVideoMetadata(context, uri, needDuration = true)
                } else {
                    ExtractedVideoMeta(extractDurationSeconds(context, uri), 0, 0)
                }
                val qLabel = if (mediaType == DeviceMediaType.VIDEO) {
                    classifyQualityLabel(meta.widthPx, meta.heightPx)
                } else {
                    "Audio"
                }
                ImportedDeviceVideo(
                    uriString = uri.toString(),
                    title = cleanTitle,
                    durationSeconds = meta.durationSeconds,
                    mediaType = mediaType,
                    artistOrFolder = if (mediaType == DeviceMediaType.AUDIO) "Device Audio" else "",
                    widthPx = meta.widthPx,
                    heightPx = meta.heightPx,
                    qualityLabel = qLabel
                )
            }.getOrNull()
        }
    }

    suspend fun scanFolderTree(
        context: Context,
        treeUri: Uri,
        maxVideos: Int = 300
    ): List<ImportedDeviceVideo> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        val discovered = mutableListOf<Triple<Uri, String, DeviceMediaType>>()
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
                        val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Media" else "Media"
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(docId)
                        } else if (isVideoFile(mime, displayName)) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            discovered.add(Triple(fileUri, displayName, DeviceMediaType.VIDEO))
                        } else if (isAudioFile(mime, displayName)) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            discovered.add(Triple(fileUri, displayName, DeviceMediaType.AUDIO))
                        }
                    }
                }
            }
        }

        discovered.map { (uri, name, mediaType) ->
            val cleanTitle = name.substringBeforeLast('.').ifBlank { name }
            val meta = if (mediaType == DeviceMediaType.VIDEO) {
                extractVideoMetadata(context, uri, needDuration = true)
            } else {
                ExtractedVideoMeta(extractDurationSeconds(context, uri), 0, 0)
            }
            val qLabel = if (mediaType == DeviceMediaType.VIDEO) {
                classifyQualityLabel(meta.widthPx, meta.heightPx)
            } else {
                "Audio"
            }
            ImportedDeviceVideo(
                uriString = uri.toString(),
                title = cleanTitle,
                durationSeconds = meta.durationSeconds,
                mediaType = mediaType,
                artistOrFolder = if (mediaType == DeviceMediaType.AUDIO) "Device Audio" else "",
                widthPx = meta.widthPx,
                heightPx = meta.heightPx,
                qualityLabel = qLabel
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

    suspend fun loadAlbumArtBitmap(
        context: Context,
        albumArtUri: String?
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (albumArtUri.isNullOrBlank()) return@withContext null
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(albumArtUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private fun isVideoFile(mimeType: String, fileName: String): Boolean {
        if (mimeType.startsWith("video/", ignoreCase = true)) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in videoExtensions
    }

    private fun isAudioFile(mimeType: String, fileName: String): Boolean {
        if (mimeType.startsWith("audio/", ignoreCase = true)) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in audioExtensions
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

    private data class ExtractedVideoMeta(
        val durationSeconds: Int,
        val widthPx: Int,
        val heightPx: Int
    )

    private fun extractVideoMetadata(
        context: Context,
        uri: Uri,
        needDuration: Boolean
    ): ExtractedVideoMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durSec = if (needDuration) {
                val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                (ms / 1000L).toInt().coerceAtLeast(1)
            } else {
                1
            }
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            ExtractedVideoMeta(durSec, w, h)
        } catch (_: Exception) {
            ExtractedVideoMeta(if (needDuration) 60 else 1, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
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
