package com.recoverx.pro.data

sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val phase: String, val found: Int) : ScanState
    data class Done(val resultCount: Int, val sourcesScanned: Int) : ScanState
    data class Error(val message: String) : ScanState
}
