package com.example.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

import com.example.server.Logger;

public class ChatController implements Initializable {
    @FXML
    private TextField textfield_message;
    @FXML
    private TextField search_field;
    @FXML
    private VBox vbox_messages;
    @FXML
    private VBox user_room_list;
    @FXML
    private Button send_button;
    @FXML
    private Button exit_button;
    @FXML
    private ScrollPane scrollPane_main;
    @FXML
    private ScrollPane user_room_scrollpane;
    @FXML
    private Label labelName;

    private String currentSrceen = null;
    private String username;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Map<String, VBox> chatHistories = new HashMap<>();
    private List<String> onlineUsers = new ArrayList<>();
    private List<String> groupListDisplay = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vbox_messages.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane_main.setVvalue(scrollPane_main.getVmax());
        });
        user_room_list.heightProperty().addListener((obs, oldVal, newVal) -> {
            user_room_scrollpane.setVvalue(user_room_scrollpane.getVmax());
        });
        send_button.setOnAction(this::sendMessage);
        textfield_message.setOnAction(this::sendMessage);
        search_field.textProperty().addListener((obs, oldVal, newVal) -> {
            filterList(newVal);
        });
        filterList(search_field.getText());
    }

    public void initData(String username, Socket socket) {
        this.username = username;
        this.socket = socket;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(this::receiveMessages).start();
    }

    private void handleUserAndGroupList(String message) {
        if (message.startsWith("ONLINE_USERS|")) {
            String[] users = message.substring("ONLINE_USERS|".length()).split(",");
            Platform.runLater(() -> {
                onlineUsers.clear();
                for (String user : users) {
                    if (!user.equals(this.username)) {
                        onlineUsers.add(user);
                    }
                }
                updateUserRoomList();
            });
        } else if (message.startsWith("GROUP_LIST|")) {
            String[] groups = message.substring("GROUP_LIST|".length()).split(",");
            Platform.runLater(() -> {
                groupListDisplay.clear();
                groupListDisplay.addAll(Arrays.asList(groups));
                updateUserRoomList();
            });
        }
    }

    private void receiveMessages() {
        while (socket.isConnected()) {
            try {
                String message = in.readLine();
                if (message != null) {
                    if (message.startsWith("ONLINE_USERS|") || message.startsWith("GROUP_LIST|")) {
                        handleUserAndGroupList(message);
                    } else if (message.startsWith("BROADCAST|")) {
                        String[] parts = message.split("\\|", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            String groupName = parts[2];
                            String msg = parts[3];
                            Logger.logRoomMessage(groupName, sender, msg);

                            if (!username.equals(sender)) {
                                Platform.runLater(() -> {
                                    VBox chatBox = chatHistories.computeIfAbsent("Group: " + groupName,
                                            k -> createChatBox());
                                    addReceiveLabel(sender + ": " + msg, chatBox);
                                    updateNewMessageIndicator("Group: " + groupName);
                                    if (("Group: " + groupName).equals(currentSrceen)) {
                                        scrollPane_main.setContent(chatBox);
                                    }
                                });
                            }
                        } else {
                            System.out.println("Received malformed BROADCAST message: " + message);
                        }
                    } else if (message.startsWith("PRIVATE|")) {
                        String[] parts = message.split("\\|", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            String receiver = parts[2];
                            String msg = parts[3];
                            Logger.logPrivateMessage(sender, receiver, msg);

                            if (receiver.equals(username)) {
                                Platform.runLater(() -> {
                                    VBox chatBox = chatHistories.computeIfAbsent("User: " + sender,
                                            k -> createChatBox());
                                    addReceiveLabel(msg, chatBox);
                                    updateNewMessageIndicator("User: " + sender);
                                    if (("User: " + sender).equals(currentSrceen)) {
                                        scrollPane_main.setContent(chatBox);
                                    }
                                });
                            } else if (sender.equals(username) && receiver.equals(currentSrceen)) {
                                Platform.runLater(() -> {
                                    VBox chatBox = chatHistories.computeIfAbsent("User: " + receiver,
                                            k -> createChatBox());
                                    addSentLabel(msg, chatBox);
                                });
                            }
                        }
                    } else {
                        System.out.println("Received unknown message: " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
                e.printStackTrace();
                break;
            }
        }
    }

    private VBox createChatBox() {
        VBox newVBox = new VBox();
        newVBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane_main.setVvalue(scrollPane_main.getVmax());
        });
        newVBox.prefWidthProperty().bind(scrollPane_main.widthProperty() 
                .subtract(20));
        return newVBox;
    }

    private void updateNewMessageIndicator(String sender) {
        if (!sender.equals(currentSrceen)) {
            for (javafx.scene.Node node : user_room_list.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    String labelText = label.getText();
                    System.out.println("Checking label: " + labelText);
                    if (labelText.equals(sender)) {
                        node.getStyleClass().add("new-message");
                        break;
                    }
                }
            }
        }

        for (javafx.scene.Node node : user_room_list.getChildren()) {
            if (node instanceof Label) {
                System.out.println(((Label) node).getText() + " - Styles: " + node.getStyleClass());
            } else {
                System.out.println(node);
            }
        }
    }

    private void updateUserRoomList() {
        Platform.runLater(() -> {
            user_room_list.getChildren().clear();
            List<String> combinedList = new ArrayList<>();
            combinedList.addAll(onlineUsers.stream().map(user -> "User: " + user).collect(Collectors.toList()));
            combinedList.addAll(groupListDisplay.stream().map(group -> "Group: " + group).collect(Collectors.toList()));

            for (String item : combinedList) {
                Label label = createRoomLabel(item);
                user_room_list.getChildren().add(label);
            }
            filterList(search_field.getText());
        });
    }

    private void loadChatHistory(String chatIdentifier) {
        VBox chatBox = chatHistories.computeIfAbsent(chatIdentifier, k -> createChatBox());
        if (chatBox.getChildren().isEmpty()) {
            String history = "";
            if (chatIdentifier.startsWith("Group: ")) {
                String roomName = chatIdentifier.substring(7);
                history = Logger.readRoomLog(roomName);
                String[] lines = history.split("\n");
                for (String line : lines) {
                    if (line.startsWith("BROADCAST|")) {
                        String[] parts = line.split("\\|", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            
                            String message = parts[3];
                            Platform.runLater(() -> addReceiveLabel(sender + ": " + message, chatBox));
                        }
                    }
                }
            } else if (chatIdentifier.startsWith("User: ")) {
                String otherUser = chatIdentifier.substring(6);
                history = Logger.readPrivateLog(username, otherUser);
                String[] lines = history.split("\n");
                for (String line : lines) {
                    if (line.startsWith("PRIVATE|")) {
                        String[] parts = line.split("\\|", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            String receiver = parts[2];
                            String message = parts[3];
                            if (sender.equals(username)) {
                                Platform.runLater(() -> addSentLabel(message, chatBox));
                            } else if (receiver.equals(username)) {
                                Platform.runLater(() -> addReceiveLabel(message, chatBox));
                            }
                        }
                    }
                }
            }
        }
        Platform.runLater(() -> scrollPane_main.setContent(chatBox));
    }

    private Label createRoomLabel(String itemName) {
        Label label = new Label(itemName);
        label.setMinWidth(user_room_scrollpane.getWidth());
        label.setPadding(new Insets(5));
        label.setStyle(
                "-fx-background-color: #e0e0e0; -fx-background-radius: 20px; -fx-padding: 10px;-fx-font-size: 16px;");
        label.setMaxWidth(1 * scrollPane_main.getWidth());
        label.setOnMouseClicked(e -> {
            String selectedItem = itemName.startsWith("User: ") ? itemName.substring(6) : itemName.substring(7);
            currentSrceen = itemName;
            labelName.setText(selectedItem);

            loadChatHistory(itemName);

            scrollPane_main.setContent(chatHistories.get(itemName));

            label.getStyleClass().remove("new-message");
            if (itemName.startsWith("Group: ")) {
                out.println("JOIN|" + username + "|" + selectedItem);
                System.out.println("JOIN|" + username + "|" + selectedItem);
            }
        });
        return label;
    }

    private void filterList(String searchText) {
        List<String> combinedList = new ArrayList<>();
        combinedList.addAll(onlineUsers.stream().map(user -> "User: " + user).collect(Collectors.toList()));
        combinedList.addAll(groupListDisplay.stream().map(group -> "Group: " + group).collect(Collectors.toList()));

        List<String> filteredList = new ArrayList<>();
        if (searchText == null || searchText.isEmpty()) {
            filteredList.addAll(combinedList);
        } else {
            String lowerSearchText = searchText.toLowerCase();
            for (String item : combinedList) {
                if (item.toLowerCase().contains(lowerSearchText)) {
                    filteredList.add(item);
                }
            }
        }

        Platform.runLater(() -> {
            user_room_list.getChildren().clear();
            for (String item : filteredList) {
                Label label = createRoomLabel(item);
                user_room_list.getChildren().add(label);
            }
        });
    }

    @FXML
    private void sendMessage(ActionEvent event) {
        String message = textfield_message.getText();
        if (!message.isEmpty() && currentSrceen != null) {
            if (currentSrceen.startsWith("User: ")) {
                String receiver = currentSrceen.substring(6);
                out.println("PRIVATE|" + username + "|" + receiver + "|" + message);
                VBox chatBox = chatHistories.computeIfAbsent(currentSrceen, k -> createChatBox());
                addSentLabel(message, chatBox);
            } else if (currentSrceen.startsWith("Group: ")) {
                String groupName = currentSrceen.substring(7);
                out.println("BROADCAST|" + username + "|" + groupName + "|" + message);
                VBox chatBox = chatHistories.computeIfAbsent(currentSrceen, k -> createChatBox());
                addSentLabel(message, chatBox);
            }
            textfield_message.clear();
        }
    }

    public void addReceiveLabel(String message, VBox vBox) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5, 10, 5, 5));
        hbox.setStyle("-fx-background-radius: 20px; -fx-padding: 10px;");
        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle(
                "-fx-background-color: #e0e0e0; -fx-background-radius: 20px; -fx-padding: 10px;-fx-font-size: 16px;");
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.BLACK);
        hbox.getChildren().add(textFlow);
        Platform.runLater(() -> vBox.getChildren().add(hbox));
    }

    public void addSentLabel(String message, VBox vBox) {
        HBox hbox = new HBox();
        hbox.setMaxWidth(scrollPane_main.getWidth());
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(5, 5, 5, 10));

        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle(
                "-fx-background-color:rgb(0, 119, 255); -fx-background-radius: 20px; -fx-padding: 10px;-fx-font-size: 16px;");
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.WHITE);
        hbox.getChildren().add(textFlow);
        Platform.runLater(() -> vBox.getChildren().add(hbox));
    }

    @FXML
    private void createRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter the name for the new chat group:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                if (out != null) {
                    out.println("/create " + groupName.trim());
                }
            } else {

                showAlert(Alert.AlertType.WARNING, "Warning", "Group name cannot be empty.");
            }
        });
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void exit(ActionEvent event) {
        try {
            out.println("/exit");
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
    }
}