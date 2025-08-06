package com.example.object;

import java.util.ArrayList;
import java.util.List;

interface InnerRoom {
    void addUser(String user);
    void removeUser(String user);
    boolean isUserInRoom(String user);
    List<String> getUsers();
}

public class Room implements InnerRoom {
    private String room_name;
    private List<String> users;

    public Room(String room_name) {
        this.room_name = room_name;
        this.users = new ArrayList<>();
    }

    public String getRoomName() {
        return room_name;
    }
    public void setRoomName(String room_name) {
        this.room_name = room_name;
    }

    public void addUser(String user) {
        users.add(user);
    }
    public void removeUser(String user) {
        users.remove(user);
    }
    
    public List<String> getUsers() {
        return users;
    }
    public boolean isUserInRoom(String user) {
        return users.contains(user);
    }
    public void getRoomName(String room_name) {
        this.room_name = room_name;
    }
}
