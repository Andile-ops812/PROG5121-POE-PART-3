package com.prog5121.poe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A QuickChat message (Part 2) together with the arrays that store every
 * message (Part 3).
 *
 * <p>Arrays used by the application:
 * <ul>
 *   <li>{@code sentMessages}, {@code disregardedMessages}, {@code storedMessages}
 *       - the messages in each state.</li>
 *   <li>{@code allMessages}, {@code messageIDs}, {@code messageHashes} - three
 *       <b>parallel arrays</b>: index {@code i} of each array describes the same
 *       message, so a search finds the index in one array and reads the others.</li>
 * </ul>
 * Nothing is hard-coded: the arrays are filled when the user sends, disregards or
 * stores a message, and when the stored messages are read from the JSON file.
 */
public class Message {

    // ------------------------------------------------------------------
    // Constants and static arrays
    // ------------------------------------------------------------------
    public static final int MAX_LENGTH = 250;
    public static final String DEFAULT_STORE_FILE = "messages.json";
    private static final String LINE = System.lineSeparator();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Message[] sentMessages = new Message[0];
    private static Message[] disregardedMessages = new Message[0];
    private static Message[] storedMessages = new Message[0];

    // Parallel arrays (same index = same message).
    private static Message[] allMessages = new Message[0];
    private static String[] messageIDs = new String[0];
    private static String[] messageHashes = new String[0];

    // ------------------------------------------------------------------
    // Instance fields
    // ------------------------------------------------------------------
    private final String messageID;
    private final int messageNumber;
    private final String sender;
    private final String recipient;
    private final String message;
    private final String messageHash;
    private String status;

    public Message(String recipient, String message, int messageNumber) {
        this(String.format("%010d", RANDOM.nextLong(10_000_000_000L)),
                "Developer", recipient, message, messageNumber);
    }

    public Message(String messageID, String recipient, String message, int messageNumber) {
        this(messageID, "Developer", recipient, message, messageNumber);
    }

    public Message(String messageID, String sender, String recipient,
                   String message, int messageNumber) {
        this.messageID = clean(messageID);
        this.sender = clean(sender);
        this.recipient = clean(recipient);
        this.message = clean(message);
        this.messageNumber = messageNumber;
        this.messageHash = buildHash();
        this.status = "Pending";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // ------------------------------------------------------------------
    // Validation (Part 2)
    // ------------------------------------------------------------------
    public boolean checkMessageID() {
        return messageID.matches("\\d{1,10}");
    }

    public static boolean isValidCellNumber(String cell) {
        return cell != null && cell.trim().matches("\\+[1-9]\\d{7,14}");
    }

    public boolean isRecipientValid() {
        return isValidCellNumber(recipient);
    }

    public String checkRecipientCell() {
        return isRecipientValid()
                ? "Cell phone number successfully captured."
                : "Cell phone number is incorrectly formatted or does not contain an "
                  + "international code. Please correct the number and try again.";
    }

    public boolean isMessageLengthValid() {
        return message.length() <= MAX_LENGTH;
    }

    public String checkMessageLength() {
        if (isMessageLengthValid()) {
            return "Message ready to send.";
        }
        return "Message exceeds " + MAX_LENGTH + " characters by "
                + (message.length() - MAX_LENGTH) + "; please reduce the size.";
    }

    public String messageIdCreated() {
        return "Message ID generated: " + messageID;
    }

    public String createMessageHash() {
        return messageHash;
    }

    /** Hash format: first two ID digits : message number : first word + last word (upper case). */
    private String buildHash() {
        String[] words = message.isEmpty() ? new String[0] : message.split("\\s+");
        String first = words.length == 0 ? "" : words[0];
        String last = words.length == 0 ? "" : words[words.length - 1];
        String firstTwo = messageID.length() >= 2 ? messageID.substring(0, 2) : messageID;
        return (firstTwo + ":" + messageNumber + ":" + first + last)
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9:]", "");
    }

    // ------------------------------------------------------------------
    // Send / Disregard / Store
    // ------------------------------------------------------------------

    /** Applies the user's choice, checking the recipient number first. */
    public String SentMessage(String choice) {
        return applyChoice(choice, true);
    }

    /**
     * Same as {@link #SentMessage(String)} but does not reject an unformatted
     * recipient number. Needed for the brief's test message 4, whose only contact
     * detail is the developer's local number 0838884567.
     */
    public String sentMessageWithoutRecipientCheck(String choice) {
        return applyChoice(choice, false);
    }

    private String applyChoice(String choice, boolean checkRecipient) {
        if (choice == null) {
            return "Invalid option. Choose Send, Disregard, or Store.";
        }
        switch (choice.trim().toLowerCase(Locale.ROOT)) {
            case "1", "send" -> {
                String problem = validateForDelivery(checkRecipient);
                if (problem != null) {
                    return problem;
                }
                status = "Sent";
                sentMessages = append(sentMessages, this);
                register(this);
                return "Message successfully sent.";
            }
            case "2", "disregard" -> {
                status = "Disregarded";
                disregardedMessages = append(disregardedMessages, this);
                register(this);
                return "Press 0 to delete the message.";
            }
            case "3", "store" -> {
                String problem = validateForDelivery(checkRecipient);
                if (problem != null) {
                    return problem;
                }
                status = "Stored";
                storedMessages = append(storedMessages, this);
                register(this);
                return "Message successfully stored.";
            }
            default -> {
                return "Invalid option. Choose Send, Disregard, or Store.";
            }
        }
    }

    private String validateForDelivery(boolean checkRecipient) {
        if (!isMessageLengthValid()) {
            return "Please enter a message of less than 250 characters.";
        }
        if (checkRecipient && !isRecipientValid()) {
            return checkRecipientCell();
        }
        return null;
    }

    /** Adds the message to the three parallel arrays. */
    private static void register(Message item) {
        allMessages = append(allMessages, item);
        messageIDs = append(messageIDs, item.messageID);
        messageHashes = append(messageHashes, item.messageHash);
    }

    // ------------------------------------------------------------------
    // Part 3 - display, search and delete
    // ------------------------------------------------------------------

    /** 2(a) Sender and recipient of every stored message. */
    public static String displayStoredMessages() {
        if (storedMessages.length == 0) {
            return "No stored messages were found.";
        }
        StringBuilder output = new StringBuilder("STORED MESSAGES");
        for (Message item : storedMessages) {
            output.append(LINE).append("--------------------")
                    .append(LINE).append("Sender: ").append(item.sender)
                    .append(LINE).append("Recipient: ").append(item.recipient);
        }
        return output.toString();
    }

    /** 2(b) The longest stored message. */
    public static String displayLongestMessage() {
        Message longest = null;
        for (Message item : storedMessages) {
            if (longest == null || item.message.length() > longest.message.length()) {
                longest = item;
            }
        }
        return longest == null ? "No stored messages were found." : longest.message;
    }

    /** 2(c) Search the message ID array; show the matching recipient and message. */
    public static String searchMessageByID(String id) {
        int index = indexOf(messageIDs, clean(id));
        if (index < 0) {
            return "Message ID not found.";
        }
        Message found = allMessages[index];
        return "Recipient: " + found.recipient + LINE + "Message: " + found.message;
    }

    /** 2(d) All sent or stored messages for one recipient (disregarded ones are ignored). */
    public static String searchMessagesByRecipient(String number) {
        String wanted = clean(number);
        StringBuilder result = new StringBuilder();
        for (Message item : allMessages) {
            boolean delivered = item.status.equals("Sent") || item.status.equals("Stored");
            if (delivered && item.recipient.equals(wanted)) {
                if (result.length() > 0) {
                    result.append(LINE);
                }
                result.append(item.message);
            }
        }
        return result.length() == 0 ? "No messages found for this recipient." : result.toString();
    }

    /** 2(e) Search the message hash array and delete the matching message. */
    public static String deleteMessageByHash(String hash) {
        int index = indexOf(messageHashes, clean(hash).toUpperCase(Locale.ROOT));
        if (index < 0) {
            return "No message was found with that hash.";
        }
        Message found = allMessages[index];
        found.deleteMessage();
        return "Message: \"" + found.message + "\" successfully deleted.";
    }

    /** 2(f) Full details of all sent and stored messages. */
    public static String displayMessageReport() {
        StringBuilder report = new StringBuilder("QUICKCHAT MESSAGE REPORT");
        int count = 0;
        for (Message item : allMessages) {
            if (item.status.equals("Sent") || item.status.equals("Stored")) {
                report.append(LINE).append("====================")
                        .append(LINE).append(item.getMessageDetails())
                        .append(LINE).append("Status: ").append(item.status);
                count++;
            }
        }
        return count == 0 ? "There are no sent or stored messages to include in the report."
                : report.toString();
    }

    /** Sent messages only (main-menu option 2). */
    public static String printMessages() {
        if (sentMessages.length == 0) {
            return "No messages have been sent yet.";
        }
        StringBuilder output = new StringBuilder();
        for (Message item : sentMessages) {
            if (output.length() > 0) {
                output.append(LINE).append(LINE);
            }
            output.append(item.getMessageDetails());
        }
        return output.toString();
    }

    public String getMessageDetails() {
        return "Message ID: " + messageID + LINE
                + "Message Hash: " + messageHash + LINE
                + "Sender: " + sender + LINE
                + "Recipient: " + recipient + LINE
                + "Message: " + message;
    }

    /** Removes this message from every array (used by "disregard" and delete-by-hash). */
    public String deleteMessage() {
        int index = indexOfSame(allMessages, this);
        if (index >= 0) {
            allMessages = removeAt(allMessages, index);
            messageIDs = removeAt(messageIDs, index);
            messageHashes = removeAt(messageHashes, index);
        }
        sentMessages = removeAt(sentMessages, indexOfSame(sentMessages, this));
        storedMessages = removeAt(storedMessages, indexOfSame(storedMessages, this));
        disregardedMessages = removeAt(disregardedMessages, indexOfSame(disregardedMessages, this));
        status = "Deleted";
        return "Message deleted.";
    }

    // ------------------------------------------------------------------
    // Array helpers
    // ------------------------------------------------------------------
    private static Message[] append(Message[] old, Message value) {
        Message[] result = Arrays.copyOf(old, old.length + 1);
        result[old.length] = value;
        return result;
    }

    private static String[] append(String[] old, String value) {
        String[] result = Arrays.copyOf(old, old.length + 1);
        result[old.length] = value;
        return result;
    }

    private static Message[] removeAt(Message[] old, int index) {
        if (index < 0) {
            return old;
        }
        Message[] result = new Message[old.length - 1];
        System.arraycopy(old, 0, result, 0, index);
        System.arraycopy(old, index + 1, result, index, old.length - index - 1);
        return result;
    }

    private static String[] removeAt(String[] old, int index) {
        if (index < 0) {
            return old;
        }
        String[] result = new String[old.length - 1];
        System.arraycopy(old, 0, result, 0, index);
        System.arraycopy(old, index + 1, result, index, old.length - index - 1);
        return result;
    }

    private static int indexOf(String[] values, String wanted) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfSame(Message[] values, Message wanted) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == wanted) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Getters for the arrays (copies, so callers cannot change them)
    // ------------------------------------------------------------------
    public static int returnTotalMessages() { return sentMessages.length; }
    public static Message[] getSentMessages() { return Arrays.copyOf(sentMessages, sentMessages.length); }
    public static Message[] getDisregardedMessages() { return Arrays.copyOf(disregardedMessages, disregardedMessages.length); }
    public static Message[] getStoredMessages() { return Arrays.copyOf(storedMessages, storedMessages.length); }
    public static String[] getMessageHashes() { return Arrays.copyOf(messageHashes, messageHashes.length); }
    public static String[] getMessageIDs() { return Arrays.copyOf(messageIDs, messageIDs.length); }

    /** Empties every array. Used by the unit tests so each test starts clean. */
    public static void resetTotalMessagesForTests() {
        sentMessages = new Message[0];
        disregardedMessages = new Message[0];
        storedMessages = new Message[0];
        allMessages = new Message[0];
        messageIDs = new String[0];
        messageHashes = new String[0];
    }

    // ------------------------------------------------------------------
    // JSON storage
    // ------------------------------------------------------------------

    /*
     * ATTRIBUTION - reading and writing the JSON file.
     * The approach (a Gson object that turns a JSON array into a Java array with
     * fromJson(reader, Type[].class), and writes one back with toJson) follows the
     * official "Gson User Guide", section "Array Examples":
     *   https://github.com/google/gson/blob/main/UserGuide.md
     * and the Baeldung tutorial "Gson Serialization and Deserialization":
     *   https://www.baeldung.com/gson-deserialization-guide
     * Gson is an open-source library by Google (Apache License 2.0).
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** The fields of one message as they appear in messages.json. */
    private static class JsonRecord {
        String messageID;
        String messageHash;
        String sender;
        String recipient;
        String message;
        String status;
        int messageNumber;

        JsonRecord(Message m) {
            messageID = m.messageID;
            messageHash = m.messageHash;
            sender = m.sender;
            recipient = m.recipient;
            message = m.message;
            status = m.status;
            messageNumber = m.messageNumber;
        }
    }

    /** Saves the stored-messages array to the JSON file. */
    public static void storeMessage(Path file) throws IOException {
        writeJson(storedMessages, file);
    }

    /** Saves the given list of messages to the JSON file. */
    public static void storeMessage(List<Message> messages, Path file) throws IOException {
        writeJson(messages.toArray(new Message[0]), file);
    }

    private static void writeJson(Message[] messages, Path file) throws IOException {
        JsonRecord[] records = new JsonRecord[messages.length];
        for (int i = 0; i < messages.length; i++) {
            records[i] = new JsonRecord(messages[i]);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(records, writer);
        }
    }

    /**
     * Reads the JSON file and places every message in the stored-messages array
     * (and the parallel ID / hash arrays). A message whose ID is already loaded is skipped.
     */
    public static void readMessages(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        JsonRecord[] records;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            records = GSON.fromJson(reader, JsonRecord[].class);
        } catch (JsonParseException error) {
            throw new IOException("messages.json is not valid JSON: " + error.getMessage(), error);
        }
        if (records == null) {
            return;
        }
        for (JsonRecord record : records) {
            if (record == null || record.messageID == null || record.messageID.isBlank()
                    || indexOf(messageIDs, record.messageID.trim()) >= 0) {
                continue;
            }
            Message item = new Message(record.messageID, record.sender, record.recipient,
                    record.message, record.messageNumber);
            item.status = "Stored";
            storedMessages = append(storedMessages, item);
            register(item);
        }
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------
    public String getMessageID() { return messageID; }
    public int getMessageNumber() { return messageNumber; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public String getMessageHash() { return messageHash; }
    public String getStatus() { return status; }
}
