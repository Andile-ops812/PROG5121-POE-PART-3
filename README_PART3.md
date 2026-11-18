# QuickChat - Programming 1A PoE Part 3 (Store Data and Display)

Package `com.prog5121.poe`. Extends Parts 1 and 2 (registration, login, sending messages).

## Part 3 features
| Requirement | Where |
|---|---|
| Arrays: sent, disregarded, stored, message hashes, message IDs (no hard-coding) | `Message.java` - filled when the user sends / disregards / stores, and from `messages.json` |
| Read the JSON file into the stored-messages array (code attributed) | `Message.readMessages` (Gson; sources credited in the code comment) |
| Main-menu option 4 "Stored Messages" | `Main.storedMessagesMenu` |
| a) Sender and recipient of stored messages | `Message.displayStoredMessages` |
| b) Longest stored message | `Message.displayLongestMessage` |
| c) Search by message ID | `Message.searchMessageByID` |
| d) Search by recipient | `Message.searchMessagesByRecipient` |
| e) Delete by message hash | `Message.deleteMessageByHash` |
| f) Report with full details | `Message.displayMessageReport` |
| Unit tests (brief's test data, `assertEquals`) | `Part3Test.java` |
| Automated tests on GitHub | `.github/workflows/TestJava.yml` (`mvn -B test`) |

The message ID, message hash and message arrays are **parallel arrays**: index *i* of each
array describes the same message, so a search finds the index in one array and reads the others.

## Run
Java 17+ and Maven. Open the folder in NetBeans (File -> Open Project), Clean and Build, then run
`com.prog5121.poe.Main`. Tests: `mvn test`.
`messages.json` (project root) holds the two stored test messages and is updated when messages
are stored or deleted. Test message 4 is sent with `sentMessageWithoutRecipientCheck` because the
brief gives it no international-format recipient.
