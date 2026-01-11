package org.example;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.IOException;

public class LobbyController {
    @FXML
    private ListView<String> userListView;
    @FXML
    private Button readyButton;
    @FXML
    private Button exitButton;

    private boolean isReady = false;
    private ObservableList<String> userList;
    private ClientConnection clientConnection;
    private String nickname;


    @FXML
    public void initialize() {
        userList = FXCollections.observableArrayList();
        //addUser("test1");
        //addUser("test2");
        //addUser("test3");
        userListView.setItems(userList);
        Thread messageReceiver = new Thread(this::receiveMessages);
        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }

    // Metoda do ustawienia połączenia i nicku
    public void setupConnection(ClientConnection connection, String nickname) {
        this.clientConnection = connection;
        this.nickname = nickname;

        if (clientConnection != null && clientConnection.isConnected()) {
            // Zarejestruj się na serwerze
            clientConnection.sendMessage("JOIN " + nickname);
        }
    }

    private void receiveMessages() {
        try {
            while (clientConnection != null && clientConnection.isConnected()) {
                String message = clientConnection.receiveMessage();
                if (message != null) {
                    Platform.runLater(() -> handleServerMessage(message));
                }
            }
        } catch (IOException e) {
            Platform.runLater(() ->
                    System.out.println("Utracono połączenie z serwerem"));
        }
    }
    private void handleServerMessage(String message) {
        System.out.println("Otrzymano od serwera: " + message);

        if (message.startsWith("USERLIST ")) {
            String[] users = message.substring(9).split(",");
            userList.clear();
            for (String user : users) {
                if (!user.isEmpty()) {
                    userList.add(user);
                }
            }
        } else if (message.startsWith("READY ")) {
            String user = message.substring(6);
            // Aktualizuj UI dla gotowego użytkownika
        } else if (message.startsWith("UNREADY ")) {
            String user = message.substring(8);
            // Aktualizuj UI dla niegotowego użytkownika
        } else if (message.startsWith("START_GAME")) {
            // Przejdź do ekranu gry
        }
    }
    @FXML
    private void handleReadyButton() {
        if (!isReady) {
            if (clientConnection != null) {
                clientConnection.sendMessage("READY " + nickname);
            }
            readyButton.setText("Gotowość");
            readyButton.getStyleClass().add("ready");
            isReady = true;
        } else {
            if (clientConnection != null) {
                clientConnection.sendMessage("UNREADY " + nickname);
            }
            readyButton.setText("Gotowy?");
            readyButton.getStyleClass().remove("ready");
            isReady = false;
        }
    }

    @FXML
    private void handleExitButton(ActionEvent event) throws IOException {
        if (clientConnection != null) {
            clientConnection.sendMessage("EXIT " + nickname);
            clientConnection.disconnect();
        }

        Stage stage;
        Scene scene;
        Parent root;
        root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }


    @FXML
    private void addUser() {
        // Metoda pozostaje niezmieniona, jeśli potrzebujesz
    }

    public void addUser(String nickname) {
        if (nickname != null && !nickname.trim().isEmpty() && !userList.contains(nickname)) {
            userList.add(nickname);
        }
    }

    @FXML
    private void removeSelectedUser() {
        int selectedIndex = userListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            userList.remove(selectedIndex);
        }
    }

    public ObservableList<String> getUserList() {
        return userList;
    }

    public void setUserList(ObservableList<String> userList) {
        this.userList = userList;
        userListView.setItems(userList);
    }
}