package com.example.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    public void initialize() {

        Platform.runLater(() -> usernameField.requestFocus());
        passwordField.setOnAction(event -> handleLogin(event));
        
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Login Error", "Username and password cannot be empty.");
            return;
        }

        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN|" + username + "|" + password);
            String response = in.readLine();

            if ("LOGIN_SUCCESS".equals(response)) {
                switchToChatScene(event, username, socket);
                return;
            } else if ("LOGIN_FAILED".equals(response)) {
                showAlert(AlertType.ERROR, "Login Failed", "Invalid username or password.");
            } else {
                showAlert(AlertType.ERROR, "Server Error", "Unexpected response: " + response);
            }

        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Login Error", "Could not connect to server: " + e.getMessage());
        }
    }


    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Register Error", "Username and password cannot be empty.");
            return;
        }
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            socket = new Socket("192.168.2.1", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("REGISTER|" + username + "|" + password);
            String response = in.readLine();

            if ("REGISTER_SUCCESS".equals(response)) {
                showAlert(AlertType.INFORMATION, "Register Success", "Account created successfully.");
                
            } else if ("REGISTER_FAILED".equals(response)) {
                showAlert(AlertType.ERROR, "Register Failed", "Username already exists.");
            } else {
                showAlert(AlertType.ERROR, "Server Error", "Unexpected response: " + response);
            }
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Register Error", "Could not connect to server: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {                                  
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void switchToChatScene(ActionEvent event, String username, Socket socket) {
        try {
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Chat.fxml"));
            root = loader.load();

       
            ChatController chatController = loader.getController();
            chatController.initData(username, socket);

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/example/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Scene Switch Error", "Could not load chat interface: " + e.getMessage());
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}