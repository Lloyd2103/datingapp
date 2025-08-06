package com.example.server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    private static final String log_file_path = "src/main/java/com/example/data/";

    public static void logRoomMessage(String room, String sender, String message) {
        try {
            String filename = "room_" + room + ".log";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(log_file_path + filename, true))) {
                writer.write("BROADCAST|" + sender + "|" + room + "|" + message);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to room log file: " + e.getMessage());
        }
    }
    
    public static void logPrivateMessage(String sender, String receiver, String message) {
        try {
            String user1 = sender.compareTo(receiver) < 0 ? sender : receiver;
            String user2 = sender.compareTo(receiver) < 0 ? receiver : sender;
            String filename = "private_" + user1 + "_" + user2 + ".log";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(log_file_path + filename, true))) {
                writer.write("PRIVATE|" + sender + "|" + receiver + "|" + message);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    public static String readRoomLog(String roomName) {
        StringBuilder chatHistory = new StringBuilder();
        String filename = log_file_path + "room_" + roomName + ".log";
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatHistory.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error reading room log file: " + e.getMessage());
            return "No chat history found for room: " + roomName;
        }
        return chatHistory.toString();
    }

    public static String readPrivateLog(String user1, String user2) {
        StringBuilder chatHistory = new StringBuilder();
        String first = user1.compareTo(user2) < 0 ? user1 : user2;
        String second = user1.compareTo(user2) < 0 ? user2 : user1;
        String filename = log_file_path + "private_" + first + "_" + second + ".log";
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatHistory.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error reading private log file: " + e.getMessage());
            return "No chat history found between " + user1 + " and " + user2;
        }
        return chatHistory.toString();
    }
}
