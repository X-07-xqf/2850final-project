package com.goodfood.messages

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [MessageService].
 *
 * Job stories covered (see Wiki: Job-Stories — Subscriber & Professional comms):
 *  - "Professional Advice" — when my dietitian sends me advice, I want to see
 *    a clear unread badge so I notice it.
 *  - "Conversation View" — when I open a conversation, I want previous messages
 *    to be marked as read so the badge clears.
 *
 * Acceptance criteria exercised:
 *  - AC-MSG-1  Sending a message increases the recipient's unread count.    [sendMessageIncreasesUnreadCount]
 *  - AC-MSG-2  Opening the conversation marks messages as read.             [getConversationMarksMessagesAsRead]
 */
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