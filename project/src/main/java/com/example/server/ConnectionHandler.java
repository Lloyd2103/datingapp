package com.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import com.example.manager.UserManager;
import com.example.manager.RoomManager;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Map<String, PrintWriter> clients;
    private UserManager userManager;
    private RoomManager roomManager;

    public ConnectionHandler(Socket socket, Map<String, PrintWriter> clients, UserManager userManager,
            RoomManager roomManager) {
        this.socket = socket;
        this.clients = clients;
        this.userManager = userManager;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String request = in.readLine();
            if (request != null && request.startsWith("LOGIN")) {
                String[] parts = request.split("\\|");
                if (parts.length == 3) {
                    String username = parts[1];
                    String password = parts[2];
                    synchronized (userManager) {
                        if (userManager.isUserExists(username) && userManager.login(username, password) && !clients.containsKey(username)) {
                            out.println("LOGIN_SUCCESS");
                            ClientHandler chatHandler = new ClientHandler(socket, clients, username,roomManager);
                            clients.put(username, out);
                            new Thread(chatHandler).start();
                            return;
                        } else {
                            out.println("LOGIN_FAILED");
                        }
                    }
                } else {
                    out.println("INVALID_LOGIN_FORMAT");
                }
            } else if (request != null && request.startsWith("REGISTER")) {
                String[] parts = request.split("\\|");
                if (parts.length == 3) {
                    String username = parts[1];
                    String password = parts[2];
                    synchronized (userManager) {
                        if (!userManager.isUserExists(username)) {
                            userManager.register(username, password);
                            out.println("REGISTER_SUCCESS");
                            ClientHandler chatHandler = new ClientHandler(socket, clients, username,roomManager);
                            clients.put(username, out); 
                            new Thread(chatHandler).start();
                            return;
                        } else {
                            out.println("REGISTER_FAILED");
                        }
                    }
                } else {
                    out.println("EXPECTING_LOGIN_OR_REGISTER");
                }
            }

            socket.close();
            out.println("Socket is closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}