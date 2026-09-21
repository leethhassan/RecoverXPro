package com.recoverx.pro.data

import android.net.Uri

/** A real source item discovered through MediaStore or SAF. */
data class RecoveryItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateTaken: Long?,
    val mimeType: String,
    val source: Source,
    val isTrashed: Boolean,
    val isCorrupted: Boolean = false
) {
    enum class Source { MEDIA_STORE, TRASH, USER_FOLDER, CARVED_IMAGE }
}
