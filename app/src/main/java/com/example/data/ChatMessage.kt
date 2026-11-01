package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val sender: String, // "USER" or "JARVIS"
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // e.g., "FLASHLIGHT", "BATTERY", "ALARM", "APP_LAUNCH", "SEARCH"
    val actionDetails: String? = null
) {
    val isJarvis: Boolean get() = sender == "JARVIS"
}
