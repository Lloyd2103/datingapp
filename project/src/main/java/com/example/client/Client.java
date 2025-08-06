package com.example.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    public static void main(String[] args) throws IOException {
        try (
                Socket socket = new Socket("localhost", 12345);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)) {

            System.out.println("PROMPT|Enter command (REGISTER|username|pass or LOGIN|username|pass):");
            String initialInput = scanner.nextLine();
            out.println(initialInput);

            boolean loggedIn = false;
            while (!loggedIn) {
                String serverMsg = in.readLine();
                if (serverMsg == null) {
                    System.out.println("Server closed connection during login.");
                    return;
                }
                System.out.println(serverMsg);
                if (serverMsg.startsWith("REGISTER_SUCCESS") || serverMsg.startsWith("LOGIN_SUCCESS")) {
                    System.out.println("You are now logged in. Type '/help' to see available commands.");
                    loggedIn = true;
                    System.out.println("Breaking login loop");
                    out.println("/help");
                    break;
                } else if (serverMsg.startsWith("Enter 'REGISTER") || serverMsg.startsWith("Invalid")
                        || serverMsg.startsWith("Username already exists.")
                        || serverMsg.startsWith("Unknown command")
                        || serverMsg.startsWith("REGISTER_FAILED")
                        || serverMsg.startsWith("LOGIN_FAILED")
                        || serverMsg.startsWith("EXPECTING_LOGIN_OR_REGISTER")) {
                    System.out.println("Please try again:");
                    String retryInput = scanner.nextLine();
                    out.println(retryInput);
                }
            }

            System.out.println("Entering chat loop");
            new Thread(new MessageListener(in)).start();

            while (true) {
                String input = scanner.nextLine();
                out.println(input);
                if (input.equalsIgnoreCase("/exit")) {
                    System.out.println("Exiting chat...");
                    break;
                }
            }
        }
    }
}
