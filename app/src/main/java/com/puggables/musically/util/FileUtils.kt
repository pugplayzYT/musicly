package com.puggables.musically.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {

    fun getMime(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    fun getDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cr: ContentResolver = context.contentResolver
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            var cursor: Cursor? = null
            try {
                cursor = cr.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    fun copyUriToTempFile(context: Context, uri: Uri, suffix: String = ""): File {
        val baseName = getDisplayName(context, uri) ?: "upload_${System.currentTimeMillis()}"
        val finalName = if (suffix.isNotEmpty() && !baseName.lowercase().endsWith(suffix.lowercase())) {
            "$baseName$suffix"
        } else {
            baseName
        }
        val safeName = sanitize(finalName)
        val temp = File(context.cacheDir, safeName)

        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(temp).use { out ->
                copy(input, out)
            }
        }
        return temp
    }

    fun suffixFromMime(mime: String?, fallback: String): String {
        return when {
            mime == null -> fallback
            "mpeg" in mime || "mp3" in mime -> ".mp3"
            "wav" in mime -> ".wav"
            "ogg" in mime -> ".ogg"
            "flac" in mime -> ".flac"
            "m4a" in mime -> ".m4a"
            "png" in mime -> ".png"
            "jpeg" in mime || "jpg" in mime -> ".jpg"
            else -> fallback
        }
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun copy(input: InputStream?, out: FileOutputStream) {
        if (input == null) return
        val buf = ByteArray(8 * 1024)
        while (true) {
            val r = input.read(buf)
            if (r == -1) break
            out.write(buf, 0, r)
        }
        out.flush()
    }
}
