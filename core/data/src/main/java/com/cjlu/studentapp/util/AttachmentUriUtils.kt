package com.cjlu.studentapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile

fun Context.resolveAttachmentDisplayName(uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    val fromProvider = contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx < 0) return@use null
        cursor.getString(idx)?.trim()?.takeIf { it.isNotBlank() }
    }
    val fromDocument = DocumentFile.fromSingleUri(this, uri)
        ?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val fromPath = uri.lastPathSegment
        ?.let(Uri::decode)
        ?.substringAfterLast('/')
        ?.substringAfterLast(':')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val raw = fromProvider ?: fromDocument ?: fromPath ?: "attachment_${System.currentTimeMillis()}"
    return sanitizeFileName(raw)
}

/** Keeps read access after the picker activity finishes (no-op if the provider disallows it). */
fun Context.tryTakePersistableReadPermission(uri: Uri) {
    try {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    }
}

fun Context.mimeTypeForUri(uri: Uri): String {
    return contentResolver.getType(uri)?.substringBefore(';')?.trim()?.takeIf { it.isNotBlank() }
        ?: "application/octet-stream"
}

private val uploadExtensionFromName = Regex("^[a-zA-Z0-9._+-]{1,16}$")

fun extensionForUpload(displayName: String, mimeType: String): String {
    if (displayName.contains('.')) {
        val dotName = displayName.substringAfterLast('.')
        val fromName = dotName.takeIf { part ->
            part.isNotBlank() && uploadExtensionFromName.matches(part)
        }?.lowercase()
        if (fromName != null) return fromName
    }
    val mime = mimeType.lowercase()
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
}

fun sanitizeFileName(name: String): String {
    val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim('.', ' ')
    return cleaned.ifBlank { "file_${System.currentTimeMillis()}" }
}

fun baseNameWithoutExtension(sanitizedFileName: String): String {
    val base = sanitizedFileName.substringBeforeLast('.', sanitizedFileName)
    return base.ifBlank { "upload" }.take(80)
}
