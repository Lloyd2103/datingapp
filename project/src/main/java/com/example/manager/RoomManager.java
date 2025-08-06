package com.example.manager;

import com.example.object.Room;

import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

interface RoomManagerDAO{
    void addRoom(String roomName);
    void removeRoom(String roomName);
    void removeRoom(Room room);
    void reNameRoom(String oldName, String newName);
    void addUser(String roomName, String name);
    void removeUser(String roomName, String name);
    boolean isUserInRoom(String roomName, String name);
    Room getRoom(String roomName);
    String getRooms();
    boolean isRoomExists(String roomName);
    void loadRoomsFromFile();
    void saveRoomsToFile();
}

public class RoomManager implements RoomManagerDAO {
    private List<Room> rooms;
    private final String filepath = "src/main/java/com/example/data/rooms.txt";

    public RoomManager() {
        rooms = new ArrayList<>();
    }

    public void addRoom(String roomName) {
        rooms.add(new Room(roomName));
    }

    public void removeRoom(String roomName) {
        rooms.removeIf(room -> room.getRoomName().equals(roomName));
    }
    public void removeRoom(Room room) {
        rooms.remove(room);
    }
    public void reNameRoom(String oldName, String newName) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(oldName)) {
                room.setRoomName(newName);
                return;
            }
        }
    }

    public void addUser(String roomName, String name) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                room.addUser(name);
                return;
            }
        }
    }
    public void removeUser(String roomName, String name) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                room.removeUser(name);
                return;
            }
        }
    }

    public boolean isUserInRoom(String roomName, String name) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                return room.isUserInRoom(name);
            }
        }
        return false;
    }

    public Room getRoom(String roomName) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                return room;
            }
        }
        return null;
    }

    
    public String getRooms() {
        String roomsString = "";
        for (Room room : rooms) {
            roomsString += room.getRoomName() + ",";
        }
        return roomsString;
    }
    
    public boolean isRoomExists(String roomName) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                return true;
            }
        }
        return false;
    }

    public void loadRoomsFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                String roomName = parts[0];
                Room room = new Room(roomName);
                if (parts.length > 1) {
                    String[] users = parts[1].split(",");
                    for (String user : users) {
                        if (!user.trim().isEmpty()) {
                            room.getUsers().add(user.trim());
                        }
                    }
                }
                rooms.add(room);
            }
        } catch (IOException e) {
            System.err.println("Error loading rooms from file: " + e.getMessage());
        }
    }

    public void saveRoomsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            for (Room room : rooms) {
                StringBuilder line = new StringBuilder();
                line.append(room.getRoomName());

                if (!room.getUsers().isEmpty()) {
                    line.append(":");
                    line.append(String.join(",", room.getUsers()));
                }
                writer.write(line.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving rooms to file: " + e.getMessage());
        }
    }
}

