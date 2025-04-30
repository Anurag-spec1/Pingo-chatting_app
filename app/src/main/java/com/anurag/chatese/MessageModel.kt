package com.anurag.chatese

data class MessageModel(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val status: MessageStatus = MessageStatus.SENT
)


enum class MessageStatus {
    SENT, DELIVERED, SEEN
}




