package com.example.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Arrays;

import com.example.manager.RoomManager;
import com.example.manager.UserManager;

public class Server {
    private static final int PORT = 12345;
    private static final String HOST = "192.168.2.1";
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static RoomManager roomManager = new RoomManager();
    private static UserManager userManager = new UserManager();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on " + HOST + ":" + PORT);

            userManager.loadUsersFromFile();
            roomManager.loadRoomsFromFile();
            System.out.println("Loaded existing rooms from file.");

            new Thread(() -> {
                try (Scanner scanner = new Scanner(System.in)) {
                    while (true) {
                        String adminCommand = scanner.nextLine();
                        if (adminCommand.equalsIgnoreCase("D")) {
                            try {
                                System.out.println("Server shutting down...");
                                userManager.saveUsersToFile();
                                roomManager.saveRoomsToFile();
                                serverSocket.close();
                                System.exit(0);
                            } catch (IOException e) {
                                System.err.println("Error closing server socket: " + e.getMessage());
                            }
                        }
                    }
                }
            }).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, clients, userManager,
                        roomManager);
                new Thread(connectionHandler).start();
                System.out.println("New client connected: " + clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    public static Map<String, PrintWriter> getClients() {
        return clients;
    }

    public static RoomManager getRoomManager() {
        return roomManager;
    }

    public static void broadcastUserList() {
        String onlineUsers = "ONLINE_USERS|" + String.join(",", clients.keySet());
        for (PrintWriter writer : clients.values()) {
            writer.println(onlineUsers);
            writer.flush();
        }
    }

    public static void broadcastRoomList() {
        List<String> groupNames = Arrays.asList(roomManager.getRooms().split(","));
        String groupList = "GROUP_LIST|" + String.join(",", groupNames);
        for (PrintWriter writer : clients.values()) {
            writer.println(groupList);
            writer.flush();
        }
    }

}