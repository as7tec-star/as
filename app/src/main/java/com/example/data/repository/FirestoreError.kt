package com.example.data.repository

import android.util.Log

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(e: Exception, operation: OperationType, path: String) {
    Log.e(
        "FirestoreError",
        """{"status": "error", "operation": "${operation.name}", "path": "$path", "message": "${e.message?.replace("\"", "\\\"") ?: "Unknown error"}"}"""
    )
}
