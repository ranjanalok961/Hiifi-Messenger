package com.demo.hiifi

data class Message(
    val chatId : String = "",
    val deletedFor: List<String> = emptyList(),
    var message: String = "",
    val receiverId : String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
)
data class UserList(
    val name : String = "",
    val id : String = ""
)
data class ChatContainer(
    val chatId: String = "",
    val message: List<Message> = emptyList()
)


