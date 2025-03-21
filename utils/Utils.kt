package com.example.vitalrite_1.utils

fun getTimeAsList(timeRaw: Any?): List<String> {
    return when (timeRaw) {
        is List<*> -> (timeRaw as List<*>).filterIsInstance<String>()
        is String -> listOf(timeRaw)
        else -> emptyList()
    }
}