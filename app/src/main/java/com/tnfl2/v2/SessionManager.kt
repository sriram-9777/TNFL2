package com.tnfl2.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SessionManager {
    var authToken: String? = null
    val draftClosingStocks = mutableStateMapOf<String, String>()
    var expenseReason by mutableStateOf("")
    var expenseAmount by mutableStateOf("")

    private var loadingCount by mutableIntStateOf(0)
    val isGlobalLoading: Boolean get() = loadingCount > 0

    fun incrementLoading() {
        loadingCount++
    }

    fun decrementLoading() {
        if (loadingCount > 0) loadingCount--
    }

    fun clearDraft() {
        draftClosingStocks.clear()
        expenseReason = ""
        expenseAmount = ""
    }
}
