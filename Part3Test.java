package com.prog5121.poe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Part 3 unit tests, using the five test messages from the PoE brief. */
class Part3Test {

    private static final String RECIPIENT_1 = "+27834557896";
    private static final String RECIPIENT_2 = "+27838884567";
    private static final String RECIPIENT_3 = "+27834484567";
    private static final String DEVELOPER_NUMBER = "0838884567";

    private static final String TEXT_1 = "Did you get the cake?";
    private static final String TEXT_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String TEXT_3 = "Yohoooo, I am at your gate.";
    private static final String TEXT_4 = "It is dinner time!";
    private static final String TEXT_5 = "Ok, I am leaving without you.";

    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void reset() {
        Message.resetTotalMessagesForTests();
        message1 = new Message("1000000001", "Developer", RECIPIENT_1, TEXT_1, 1);
        message2 = new Message("1000000002", "Developer", RECIPIENT_2, TEXT_2, 2);
        message3 = new Message("1000000003", "Developer", RECIPIENT_3, TEXT_3, 3);
        // Test message 4 has no formatted recipient: its contact is the developer number.
        message4 = new Message(DEVELOPER_NUMBER, "Developer", DEVELOPER_NUMBER, TEXT_4, 4);
        message5 = new Message("1000000005", "Developer", RECIPIENT_2, TEXT_5, 5);
    }

    /** Puts test messages 1-4 into the arrays with the flags given in the brief. */
    private void populateMessages1To4() {
        message1.SentMessage("1");
        message2.SentMessage("3");
        message3.SentMessage("2");
        message4.sentMessageWithoutRecipientCheck("1");
    }

    private void populateAllFiveMessages() {
        populateMessages1To4();
        message5.SentMessage("3");
    }

    @Test
    @DisplayName("Sent messages array is correctly populated (messages 1-4)")
    void sentMessagesArrayCorrectlyPopulated() {
        populateMessages1To4();
        String[] sentTexts = new String[Message.getSentMessages().length];
        for (int i = 0; i < sentTexts.length; i++) {
            sentTexts[i] = Message.getSentMessages()[i].getMessage();
        }
        assertArrayEquals(new String[]{"Did you get the cake?", "It is dinner time!"}, sentTexts);
    }

    @Test
    @DisplayName("Disregarded, stored, hash and ID arrays are correctly populated")
    void otherArraysCorrectlyPopulated() {
        populateAllFiveMessages();
        assertEquals(1, Message.getDisregardedMessages().length);
        assertEquals(TEXT_3, Message.getDisregardedMessages()[0].getMessage());
        assertEquals(2, Message.getStoredMessages().length);
        assertArrayEquals(
                new String[]{"1000000001", "1000000002", "1000000003", "0838884567", "1000000005"},
                Message.getMessageIDs());
        assertArrayEquals(
                new String[]{"10:1:DIDCAKE", "10:2:WHERETIME", "10:3:YOHOOOOGATE",
                             "08:4:ITTIME", "10:5:OKYOU"},
                Message.getMessageHashes());
    }

    @Test
    @DisplayName("Display the longest message")
    void displayLongestMessage() {
        populateMessages1To4();
        assertEquals("Where are you? You are late! I have asked you to be on time.",
                Message.displayLongestMessage());
    }

    @Test
    @DisplayName("Search for message ID")
    void searchForMessageID() {
        populateMessages1To4();
        assertEquals("Recipient: " + DEVELOPER_NUMBER + System.lineSeparator()
                + "Message: It is dinner time!", Message.searchMessageByID("0838884567"));
        assertEquals("Message ID not found.", Message.searchMessageByID("9999999999"));
    }

    @Test
    @DisplayName("Search all messages sent or stored for a recipient")
    void searchMessagesForRecipient() {
        populateAllFiveMessages();
        assertEquals("Where are you? You are late! I have asked you to be on time."
                + System.lineSeparator() + "Ok, I am leaving without you.",
                Message.searchMessagesByRecipient("+27838884567"));
    }

    @Test
    @DisplayName("Disregarded messages are not returned by the recipient search")
    void recipientSearchIgnoresDisregardedMessages() {
        populateAllFiveMessages();
        assertEquals("No messages found for this recipient.",
                Message.searchMessagesByRecipient(RECIPIENT_3));
    }

    @Test
    @DisplayName("Delete a message using its message hash")
    void deleteMessageUsingHash() {
        populateAllFiveMessages();
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time.\""
                + " successfully deleted.", Message.deleteMessageByHash(message2.getMessageHash()));
        assertEquals(1, Message.getStoredMessages().length);
        assertEquals(4, Message.getMessageIDs().length);
        assertEquals(4, Message.getMessageHashes().length);
        assertEquals("No message was found with that hash.",
                Message.deleteMessageByHash(message2.getMessageHash()));
    }

    @Test
    @DisplayName("Display report shows hash, recipient and message of every message")
    void displayReport() {
        populateAllFiveMessages();
        String report = Message.displayMessageReport();
        for (Message item : new Message[]{message1, message2, message4, message5}) {
            assertTrue(report.contains("Message Hash: " + item.getMessageHash()));
            assertTrue(report.contains("Recipient: " + item.getRecipient()));
            assertTrue(report.contains("Message: " + item.getMessage()));
        }
        assertTrue(report.contains("Status: Sent"));
        assertTrue(report.contains("Status: Stored"));
    }

    @Test
    @DisplayName("Stored messages are written to and read back from the JSON file")
    void jsonFileCanBeWrittenAndRead() throws Exception {
        populateAllFiveMessages();
        Path file = Files.createTempFile("quickchat-part3", ".json");
        try {
            Message.storeMessage(file);
            Message.resetTotalMessagesForTests();
            Message.readMessages(file);

            assertEquals(2, Message.getStoredMessages().length);
            assertEquals(TEXT_2, Message.getStoredMessages()[0].getMessage());
            assertEquals(TEXT_5, Message.getStoredMessages()[1].getMessage());
            assertEquals(2, Message.getMessageIDs().length);

            // Reading the same file twice must not duplicate the messages.
            Message.readMessages(file);
            assertEquals(2, Message.getStoredMessages().length);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
