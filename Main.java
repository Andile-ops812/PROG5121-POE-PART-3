package com.prog5121.poe;

import java.nio.file.Path;
import java.util.Scanner;

/** Console application for QuickChat (registration, login, messaging and Part 3 stored messages). */
public class Main {
    private static final Path STORE_FILE = Path.of(Message.DEFAULT_STORE_FILE);

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Login login = new Login();
            registerAndLogin(scanner, login);
            System.out.println("\nWelcome to QuickChat.");

            loadStoredMessages();
            runMainMenu(scanner);

            System.out.println("Thank you for using QuickChat.");
        }
    }

    // ------------------------------------------------------------------
    // Start-up
    // ------------------------------------------------------------------

    private static void registerAndLogin(Scanner scanner, Login login) {
        System.out.println("===== QUICKCHAT REGISTRATION =====");
        String result;
        do {
            System.out.print("First name: ");
            login.setFirstName(scanner.nextLine().trim());
            System.out.print("Last name: ");
            login.setLastName(scanner.nextLine().trim());
            System.out.print("Username (underscore, maximum 5 characters): ");
            login.setUsername(scanner.nextLine().trim());
            System.out.print("Password (8+ characters, capital, number and special character): ");
            login.setPassword(scanner.nextLine());
            System.out.print("Cell number (e.g. +27838968976): ");
            login.setCellPhoneNumber(scanner.nextLine().trim());
            result = login.registerUser();
            System.out.println(result);
        } while (!result.equals("User successfully registered."));

        boolean loggedIn = false;
        System.out.println("\n===== QUICKCHAT LOGIN =====");
        while (!loggedIn) {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            loggedIn = login.loginUser(username, password);
            System.out.println(login.returnLoginStatus(loggedIn));
        }
    }

    /** Reads messages.json (if it exists) into the stored-messages array. */
    private static void loadStoredMessages() {
        try {
            Message.readMessages(STORE_FILE);
            int count = Message.getStoredMessages().length;
            System.out.println(count + " stored message(s) loaded from " + STORE_FILE.toAbsolutePath());
        } catch (Exception error) {
            System.out.println("Stored messages could not be loaded: " + error.getMessage());
        }
    }

    /** Writes the stored-messages array back to messages.json. */
    private static void saveStoredMessages() {
        try {
            Message.storeMessage(STORE_FILE);
        } catch (Exception error) {
            System.out.println("Could not save messages.json: " + error.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Menus
    // ------------------------------------------------------------------

    private static void runMainMenu(Scanner scanner) {
        boolean quit = false;
        while (!quit) {
            System.out.println("\n========== QUICKCHAT ==========");
            System.out.println("1. Send a message");
            System.out.println("2. Display sent messages");
            System.out.println("3. Display total messages sent");
            System.out.println("4. Stored Messages");
            System.out.println("5. Quit");
            System.out.print("Choose an option: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> createMessage(scanner);
                case "2" -> System.out.println(Message.printMessages());
                case "3" -> System.out.println("Total messages sent: " + Message.returnTotalMessages());
                case "4" -> storedMessagesMenu(scanner);
                case "5" -> quit = true;
                default -> System.out.println("Please choose a valid menu option.");
            }
        }
    }

    private static void createMessage(Scanner scanner) {
        System.out.print("Recipient number (include international code): ");
        String recipient = scanner.nextLine().trim();
        System.out.print("Enter your message (maximum 250 characters): ");
        String body = scanner.nextLine();

        Message message = new Message(recipient, body, Message.getMessageIDs().length + 1);
        System.out.println(message.messageIdCreated());
        System.out.println("1. Send message");
        System.out.println("2. Disregard message");
        System.out.println("3. Store message");
        System.out.print("Choose: ");
        System.out.println(message.SentMessage(scanner.nextLine().trim()));

        if (message.getStatus().equals("Stored")) {
            saveStoredMessages();
            System.out.println("Stored messages saved to " + STORE_FILE.toAbsolutePath());
        }
    }

    /** Fourth main-menu option: the Part 3 stored-message functions. */
    private static void storedMessagesMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n======= STORED MESSAGES =======");
            System.out.println("1. Display sender and recipient of stored messages");
            System.out.println("2. Display the longest stored message");
            System.out.println("3. Search using a message ID");
            System.out.println("4. Search messages using a recipient number");
            System.out.println("5. Delete a message using its message hash");
            System.out.println("6. Display the full message report");
            System.out.println("7. Back to main menu");
            System.out.print("Choose an option: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> System.out.println(Message.displayStoredMessages());
                case "2" -> System.out.println(Message.displayLongestMessage());
                case "3" -> {
                    System.out.print("Enter message ID: ");
                    System.out.println(Message.searchMessageByID(scanner.nextLine()));
                }
                case "4" -> {
                    System.out.print("Enter recipient number: ");
                    System.out.println(Message.searchMessagesByRecipient(scanner.nextLine()));
                }
                case "5" -> {
                    System.out.print("Enter message hash: ");
                    System.out.println(Message.deleteMessageByHash(scanner.nextLine()));
                    saveStoredMessages();
                }
                case "6" -> System.out.println(Message.displayMessageReport());
                case "7" -> back = true;
                default -> System.out.println("Please choose a number from 1 to 7.");
            }
        }
    }
}
