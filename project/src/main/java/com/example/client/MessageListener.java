package com.example.client;

import java.io.BufferedReader;
import java.io.IOException;

public class MessageListener implements Runnable {
    private final BufferedReader in;

    public MessageListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("BROADCAST|")) {
                    String[] parts = line.substring("BROADCAST|".length()).split("\\|", 3);
                    if (parts.length == 3) {
                        String username = parts[0];
                        String currentScreen = parts[1];
                        String message = parts[2];
                        System.out.println(currentScreen + " - " + username + ": " + message);
                    } else {
                        System.out.println("Received malformed BROADCAST message: " + line);
                    }
                } else if (line.startsWith("PRIVATE|")) {
                    String[] parts = line.substring("PRIVATE|".length()).split("\\|", 3);
                    if (parts.length == 3) {
                        String sender = parts[0];
                        String receiver = parts[1];
                        String message = parts[2];
                        System.out.println("Private message from " + sender + " to " + receiver + ": " + message);
                    } else {
                        System.out.println("Received malformed PRIVATE message: " + line);
                    }
                } else {
                    System.out.println("Received message: " + line);
                }
                
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }
}