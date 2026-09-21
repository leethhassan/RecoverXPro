package com.recoverx.pro.engine

import java.io.File

object RootAccess {
    fun isAvailable(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        p.inputStream.bufferedReader().use { it.readLine() ?: "" }.contains("uid=0")
    } catch (_: Throwable) {
        false
    }
}
