package com.goodfood.messages

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageServiceTest {

    @Test
    fun sendMessageIncreasesUnreadCount() {
        TestDatabase.setup()

        val senderId = TestDatabase.insertUser(name = "Sender")
        val receiverId = TestDatabase.insertUser(name = "Receiver")

        MessageService.sendMessage(senderId, receiverId, "Eat more vegetables")

        assertEquals(1, MessageService.getUnreadCount(receiverId))
    }

    @Test
    fun getConversationMarksMessagesAsRead() {
        TestDatabase.setup()

        val senderId = TestDatabase.insertUser(name = "Professional", role = "professional")
        val receiverId = TestDatabase.insertUser(name = "Client")

        MessageService.sendMessage(senderId, receiverId, "Remember your goals")

        val conversation = MessageService.getConversation(receiverId, senderId)

        assertEquals(1, conversation.size)
        assertEquals(0, MessageService.getUnreadCount(receiverId))
    }
}