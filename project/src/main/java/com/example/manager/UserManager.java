package com.example.manager;

import com.example.object.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList; 
import java.util.List; 

interface UserManagerDAO {
    void addUser(String username, String password); 
    void removeUser(String username); 
    List<User> getAllUsers(); 
    boolean login(String username, String password); 
    boolean register(String username, String password); 
    List<User> loadUsersFromFile(); 
    void saveUsersToFile(); 
    boolean isUserExists(String username); 
}

public class UserManager {
    private List<User> users;
    private final String filepath = "src/main/java/com/example/data/users.txt"; 
    public UserManager() {
        users = loadUsersFromFile(); 
    }

    public void addUser(String username, String password) {
        users.add(new User(username, password)); 
        saveUsersToFile(); 
    }

    public void removeUser(String username) {
        users.removeIf(user -> user.getUsername().equals(username));
        saveUsersToFile(); 
    }

    public List<User> getAllUsers() {
        return users;
    }

    public boolean login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return true; 
            }
        }
        return false;
    }

    public boolean register(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return false; 
            }
        }
        addUser(username, password);
        saveUsersToFile(); 
        return true;
    }

    public List<User> loadUsersFromFile() {
        List<User> loadedUsers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) { 
                    loadedUsers.add(new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users from file: " + e.getMessage());
            try {
                new FileWriter(filepath).close(); 
                System.out.println("Created new user data file: " + filepath);
            } catch (IOException ex) {
                System.err.println("Error creating user data file: " + ex.getMessage());
            }
        }
        return loadedUsers;
    }

    public void saveUsersToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            for (User user : users) {
                writer.println(user.getUsername() + ":" + user.getPassword());
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    
    public boolean isUserExists(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
}
