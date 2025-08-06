package com.example.object;

interface InnerUser {
    String getUsername();
    String getPassword();   
    void setUsername(String username);
    void setPassword(String password);
}

public class User implements InnerUser {
    private String username;
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
