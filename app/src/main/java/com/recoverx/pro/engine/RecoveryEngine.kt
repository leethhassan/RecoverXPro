package com.recoverx.pro.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.recoverx.pro.data.RecoveryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

class RecoveryEngine(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun scanMedia(includeTrash: Boolean, onProgress: suspend (phase: String, found: Int) -> Unit): List<RecoveryItem> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, RecoveryItem>()
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.DATE_TAKEN)
            if (Build.VERSION.SDK_INT >= 30) add(MediaStore.Images.Media.IS_TRASHED)
        }

        val queryArgs = Bundle().apply {
            if (Build.VERSION.SDK_INT >= 30 && includeTrash) {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.Media.DATE_TAKEN)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
        }

        resolver.query(collection, projection.toTypedArray(), queryArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val trashCol = if (Build.VERSION.SDK_INT >= 30) cursor.getColumnIndex(MediaStore.Images.Media.IS_TRASHED) else -1

            while (cursor.moveToNext()) {
                coroutineContext.ensureActive()
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(collection, id.toString())
                val trashed = trashCol >= 0 && cursor.getInt(trashCol) != 0
                val item = RecoveryItem(
                    id = "media:$id",
                    uri = uri,
                    name = cursor.getString(nameCol) ?: "image-$id",
                    sizeBytes = cursor.getLong(sizeCol),
                    dateTaken = if (dateCol >= 0 && !cursor.isNull(dateCol)) cursor.getLong(dateCol) else null,
                    mimeType = cursor.getString(mimeCol) ?: "image/*",
                    source = if (trashed) RecoveryItem.Source.TRASH else RecoveryItem.Source.MEDIA_STORE,
                    isTrashed = trashed
                )
                results[item.id] = item
                if (results.size % 40 == 0) onProgress(if (trashed) "System trash" else "Media library", results.size)
            }
        }
        onProgress("Media scan complete", results.size)
        results.values.toList()
    }

    suspend fun scanTree(treeUri: Uri, onProgress: suspend (phase: String, found: Int) -> Unit): List<RecoveryItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext emptyList()
        val results = ArrayList<RecoveryItem>()
        val stack = ArrayDeque<DocumentFile>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = stack.removeLast()
            for (child in runCatching { directory.listFiles() }.getOrDefault(emptyArray())) {
                coroutineContext.ensureActive()
                if (child.isDirectory) {
                    stack.add(child)
                } else if (child.isFile && isImageMime(child.type, child.name)) {
                    val uri = child.uri
                    results += RecoveryItem(
                        id = "tree:${uri}",
                        uri = uri,
                        name = child.name ?: "image",
                        sizeBytes = child.length(),
                        dateTaken = child.lastModified().takeIf { it > 0 }?.div(1000),
                        mimeType = child.type ?: guessMime(child.name),
                        source = RecoveryItem.Source.USER_FOLDER,
                        isTrashed = false
                    )
                    if (results.size % 40 == 0) onProgress("Selected folder", results.size)
                }
            }
        }
        onProgress("Folder scan complete", results.size)
        results
    }

    suspend fun exportItems(items: List<RecoveryItem>, destinationTree: Uri, onProgress: suspend (done: Int, total: Int) -> Unit): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, destinationTree) ?: return@withContext 0
        var done = 0
        for (item in items) {
            coroutineContext.ensureActive()
            val safeName = sanitizeName(item.name.ifBlank { "recovered-${UUID.randomUUID()}.jpg" })
            val mime = if (item.mimeType.startsWith("image/")) item.mimeType else guessMime(safeName)
            val file = root.findFile(safeName) ?: root.createFile(mime, safeName)
            if (file != null) {
                resolver.openInputStream(item.uri)?.use { input ->
                    resolver.openOutputStream(file.uri, "w")?.use { output ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
                    }
                }
                done++
            }
            onProgress(done, items.size)
        }
        done
    }

    private fun isImageMime(mime: String?, name: String?): Boolean {
        if (mime?.startsWith("image/") == true) return true
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp", "tif", "tiff")
    }

    private fun guessMime(name: String?): String = when (name?.substringAfterLast('.', "")?.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        else -> "application/octet-stream"
    }

    private fun sanitizeName(input: String): String = input
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .ifBlank { "recovered-image" }
}

class ForensicCarver(private val context: Context) {
    suspend fun carve(inputUri: Uri, onProgress: suspend (found: Int) -> Unit): List<RecoveryItem> = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "carved-images").apply { mkdirs() }
        val results = ArrayList<RecoveryItem>()
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            var index = 0
            while (true) {
                coroutineContext.ensureActive()
                val found = ImageSignature.findNext(input) ?: break
                val ext = if (found.first == ImageSignature.Type.JPEG) "jpg" else "png"
                val file = File(outputDir, "carved-${System.currentTimeMillis()}-${index++}.$ext")
                file.outputStream().use { it.write(found.second) }
                val uri = Uri.fromFile(file)
                results += RecoveryItem(
                    id = "carved:${file.absolutePath}",
                    uri = uri,
                    name = file.name,
                    sizeBytes = file.length(),
                    dateTaken = null,
                    mimeType = if (ext == "jpg") "image/jpeg" else "image/png",
                    source = RecoveryItem.Source.CARVED_IMAGE,
                    isTrashed = false
                )
                onProgress(results.size)
                if (results.size >= 500) break
            }
        }
        results
    }
}
