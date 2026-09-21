package com.recoverx.pro.util

import android.content.Context
import android.text.format.Formatter
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatBytes(context: Context, bytes: Long): String = Formatter.formatFileSize(context, bytes)

fun formatDate(epochSeconds: Long?, locale: Locale): String? = epochSeconds?.let {
    DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(it * 1000L))
}
