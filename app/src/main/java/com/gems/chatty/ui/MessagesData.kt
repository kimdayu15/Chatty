package com.gems.chatty.ui

data class MessageData(
    val senderId: String? = null,
    val lastMessage: String? = null,
    val timestamp: Long? = null,
    val uid: String? = null,
    val online: Boolean? = null,
    val text: String = "",
    val receiverId: String = "",
    val status: String? = null,
    val isSeen: Boolean = false
)
