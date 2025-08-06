package com.example.server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Arrays;

import com.example.manager.RoomManager;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Map<String, PrintWriter> clients;
    private RoomManager roomManager;
    private String name;

    public ClientHandler(Socket socket, Map<String, PrintWriter> clients, String name, RoomManager roomManager) {
        this.socket = socket;
        this.clients = clients;
        this.name = name;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Server.broadcastUserList();
            Server.broadcastRoomList();
            

            String command;
            while ((command = in.readLine()) != null) {
                if (command.equalsIgnoreCase("/exit")) {
                    close();
                    out.println("Exiting chat...");
                    break;
                } else if (command.equalsIgnoreCase("/users")) {
                    out.println("Online users: ");
                    allUsers();
                } else if (command.equalsIgnoreCase("/rooms")) {
                    out.println("Available rooms: ");
                    allRooms();
                } else if (command.startsWith("/create ")) {
                    String roomName = command.substring(8).trim();
                    addRoom(roomName);
                    Server.broadcastRoomList();
                } else if (command.startsWith("/join ")) {
                    String roomName = command.substring(6).trim();
                    joinRoom(roomName, name);
                    chatloop(roomName, name);
                } else if (command.startsWith("/users_room ")) {
                    String roomName = command.substring(12).trim();
                    out.println("Users in room " + roomName + ": " + roomManager.getRoom(roomName).getUsers());
                } else if (command.equalsIgnoreCase("/private")) {
                    chooseUsertoChat();
                } else if (command.equalsIgnoreCase("/help")) {
                    menuCommand();
                } else if (command.startsWith("BROADCAST|")) {
                    String[] parts = command.substring("BROADCAST|".length()).split("\\|", 3);
                    if (parts.length == 3) {
                        String sender = parts[0];
                        String currentScreen = parts[1];
                        String message = parts[2];
                        sendMessageToRoom(sender,currentScreen, message);
                    }
                } else if (command.startsWith("PRIVATE|")) {
                    String[] parts = command.substring("PRIVATE|".length()).split("\\|", 3);
                    if (parts.length == 3) {
                        String currentScreen = parts[1];
                        String message = parts[2];
                        sendMessageToUser(currentScreen, message);
                    }
                } else if (command.startsWith("JOIN|")) {
                    String[] parts = command.substring("JOIN|".length()).split("\\|", 2);
                    if (parts.length == 2) {
                        String name = parts[0];
                        String roomName = parts[1];
                        joinRoom(roomName, name);
                    }
                } else if (command.startsWith("LEAVE|")) {
                    String[] parts = command.substring("LEAVE|".length()).split("\\|", 2);
                    if (parts.length == 2) {
                        String name = parts[0];
                        String roomName = parts[1];
                        leaveRoom(roomName, name);
                    }
                } else {
                    out.println("Unknown command. Type '/help' for available commands.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void menuCommand() throws IOException {
        out.println("\nAvailable commands:");
        out.println("/users - List all users");
        out.println("/rooms - List all rooms");
        out.println("/create <room_name> - Create a new room");
        out.println("/join <room_name> - Join a room");
        out.println("/users_room <room_name> - List users in a room");
        out.println("/private - Send a private message to a user");
        out.println("/exit - Exit the chat");
        out.println("/help - Show this menu\n");
    }

    public String getUsername() {
        return name;
    }

    public void addRoom(String roomName) {
        roomManager.addRoom(roomName);
        roomManager.saveRoomsToFile();
        Server.broadcastRoomList();
    }

    public void removeRoom(String roomName) {
        roomManager.removeRoom(roomName);
        roomManager.saveRoomsToFile();
        Server.broadcastRoomList();
    }

    public void joinRoom(String roomName, String name) {
        if (!roomManager.isRoomExists(roomName)) {
            out.println("Room does not exist.");
            return;
        } else {
            if (roomManager.isUserInRoom(roomName, name)) {
                out.println("You are already in this room.");
                return;
            } else {
                roomManager.addUser(roomName, name);
                roomManager.saveRoomsToFile();
                out.println("You have joined the room: " + roomName);
            }
        }
    }

    public void leaveRoom(String roomName, String name) {
        if (!roomManager.isRoomExists(roomName)) {
            out.println("Room does not exist.");
            return;
        }
        roomManager.removeUser(roomName, name);
        roomManager.saveRoomsToFile();
        out.println("You have left the room: " + roomName);
        
    }

    public void sendMessageToUser(String username, String message) {
        PrintWriter client = clients.get(username);
        if (client != null) {
            client.println("PRIVATE|" + name + "|" + username + "|" + message);
        } else {
            System.out.println("User " + username + " is not online.");
        }
    }

    public void sendMessageToRoom(String name,String roomName, String message) {
        if (!roomManager.isRoomExists(roomName)) {
            return;
        }
        for (String user : roomManager.getRoom(roomName).getUsers()) {
            PrintWriter client = clients.get(user);
            if (client != null) {
                client.println("BROADCAST|" + name + "|" + roomName + "|" + message);
            }
        }
    }

    public void chatloop(String roomName, String name) throws IOException {
        out.println("You can start chatting now.");
        out.println("Type 'EXITCHAT|' to exit the chat.");
        out.println("Type 'HISTORY|' to view the chat history.");
        out.println("Type 'USERS|' to view users in the room.");

        String message;
        while (true) {
            message = in.readLine();
            if (message == null || message.equalsIgnoreCase("EXITCHAT|")) {
                ;
                break;
            } else if (message.equalsIgnoreCase("HISTORY|")) {
                readRoomHistory(roomName);
            } else if (message.equalsIgnoreCase("USERS|")) {
                out.println("Users in room " + roomName + ": " + roomManager.getRoom(roomName).getUsers());
            } else if (message.startsWith("LEAVE|")) {
                leaveRoom(roomName, name);
            } else {
                sendMessageToRoom(name ,roomName, message);
                Logger.logRoomMessage(roomName, name, message);
            }
        }
    }

    public void chooseUsertoChat() throws IOException {
        out.println("Enter the username of the user you want to chat with:");
        String targetname = in.readLine();
        if (clients.containsKey(targetname) && !targetname.equals(name)) {
            out.println("You are now chatting with " + targetname + ". Type 'EXITCHAT|' to end the chat.");
            String message;
            while (true) {
                message = in.readLine();
                if (message == null || message.equalsIgnoreCase("EXITCHAT|")) {
                    break;
                } else if (message.equalsIgnoreCase("HISTORY|")) {
                    readPrivateHistory(targetname);
                } else {
                    sendMessageToUser(targetname, message);
                    Logger.logPrivateMessage(name, targetname, message);
                }
            }
        } else {
            out.println("User " + targetname + " is not online or invalid username.");
        }
    }

    public void readRoomHistory(String roomName) throws IOException {
        if (roomName == null || roomName.trim().isEmpty()) {
            out.println("Room name cannot be empty.");
            return;
        }
        String history = Logger.readRoomLog(roomName);
        out.println("--- Chat history of room: " + roomName + " ---");
        out.println(history);
    }

    public void readPrivateHistory(String otherUser) throws IOException {
        if (otherUser == null || otherUser.trim().isEmpty()) {
            out.println("Username cannot be empty.");
            return;
        }
        String history = Logger.readPrivateLog(name, otherUser);
        out.println("--- Private chat history with: " + otherUser + " ---");
        out.println(history);
    }

    public void allUsers() {
        out.println("ONLINE_USERS|" + String.join(",", clients.keySet()));
        out.flush();
    }

    public void allRooms() {
        List<String> groupNames = Arrays.asList(roomManager.getRooms().split(","));
        out.println("GROUP_LIST|" + String.join(",", groupNames));
        out.flush();
    }

    public void close() {
        clients.remove(name);
        Server.broadcastUserList();
        Server.broadcastRoomList();
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}