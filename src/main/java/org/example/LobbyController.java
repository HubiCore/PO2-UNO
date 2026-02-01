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
    private Thread messageReceiver;
    private volatile boolean running = false;

    @FXML
    public void initialize() {
        userList = FXCollections.observableArrayList();
        userListView.setItems(userList);
    }

    public void setupConnection(ClientConnection connection, String nickname) {
        this.clientConnection = connection;
        this.nickname = nickname;

        if (clientConnection != null && clientConnection.isConnected()) {
            startMessageReceiver();
        } else {
            showError("Brak połączenia z serwerem");
            try {
                goBackToMainMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startMessageReceiver() {
        if (running) {
            return; // Już działa
        }

        running = true;
        messageReceiver = new Thread(() -> {
            while (running && clientConnection != null && clientConnection.isConnected()) {
                String message = clientConnection.receiveMessage();
                if (message == null) {
                    // Połączenie zostało zamknięte
                    Platform.runLater(() -> {
                        showError("Utracono połączenie z serwerem");
                        try {
                            goBackToMainMenu();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                    break;
                }
                Platform.runLater(() -> handleServerMessage(message));
            }
            running = false;
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }

    private void handleServerMessage(String message) {
        System.out.println("Otrzymano od serwera: " + message);

        if (message.startsWith("USERLIST ")) {
            updateUserList(message.substring(9));
        } else if (message.startsWith("READY ")) {
            String user = message.substring(6);
            updateUserStatus(user, true);
        } else if (message.startsWith("UNREADY ")) {
            String user = message.substring(8);
            updateUserStatus(user, false);
        } else if (message.startsWith("USER_JOINED ")) {
            String user = message.substring(12);
            System.out.println("Gracz " + user + " dołączył");
        } else if (message.startsWith("USER_LEFT ")) {
            String user = message.substring(10);
            System.out.println("Gracz " + user + " opuścił lobby");
        } else if (message.startsWith("JOIN_SUCCESS ")) {
            String user = message.substring(13);
            System.out.println("Witaj " + user + "!");
        } else if (message.startsWith("START_GAME")) {
            try {
                switch_to_game();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (message.startsWith("ERROR")) {
            showError(message);
        }
    }

    private void updateUserList(String usersStr) {
        Platform.runLater(() -> {
            userList.clear();
            if (usersStr == null || usersStr.trim().isEmpty()) {
                return;
            }

            String[] users = usersStr.split(",");
            for (String userEntry : users) {
                if (!userEntry.isEmpty()) {
                    // Format: "username:READY" lub "username:NOT_READY"
                    String[] parts = userEntry.split(":");
                    if (parts.length == 2) {
                        String username = parts[0].trim();
                        String status = parts[1].trim();

                        if (status.equals("READY")) {
                            userList.add("✓ " + username);
                        } else {
                            userList.add(username);
                        }
                    } else {
                        // Dla kompatybilności z poprzednimi wersjami
                        userList.add(userEntry);
                    }
                }
            }

            updateReadyButtonState();
        });
    }

    private void updateUserStatus(String user, boolean ready) {
        Platform.runLater(() -> {
            for (int i = 0; i < userList.size(); i++) {
                String listUser = userList.get(i);
                String baseUser = listUser.replace("✓ ", "").trim();

                if (baseUser.equals(user)) {
                    if (ready) {
                        userList.set(i, "✓ " + user);
                    } else {
                        userList.set(i, user);
                    }
                    break;
                }
            }

            updateReadyButtonState();
        });
    }

    private void updateReadyButtonState() {
        // Aktualizuj przycisk gotowości na podstawie stanu użytkownika
        if (isReady) {
            readyButton.setText("Gotowość ✓");
            readyButton.setStyle("-fx-background-color: green; -fx-text-fill: white;");
        } else {
            readyButton.setText("Gotowy?");
            readyButton.setStyle("");
        }

        // Sprawdź czy można rozpocząć grę (co najmniej 2 graczy gotowych)
        int readyCount = 0;
        int totalPlayers = userList.size();

        for (String user : userList) {
            if (user.startsWith("✓ ")) {
                readyCount++;
            }
        }

        // Możesz dodać logikę, która pokazuje informację o liczbie gotowych graczy
        // np. instrukcja.setText("Gotowych: " + readyCount + "/" + totalPlayers);
    }

    @FXML
    private void handleReadyButton() {
        if (!isReady) {
            if (clientConnection != null && clientConnection.isConnected()) {
                boolean sent = clientConnection.sendMessage("READY " + nickname);
                if (sent) {
                    isReady = true;
                    // Natychmiastowa aktualizacja lokalna
                    updateUserStatus(nickname, true);
                } else {
                    showError("Nie udało się wysłać statusu gotowości");
                }
            }
        } else {
            if (clientConnection != null && clientConnection.isConnected()) {
                boolean sent = clientConnection.sendMessage("UNREADY " + nickname);
                if (sent) {
                    isReady = false;
                    // Natychmiastowa aktualizacja lokalna
                    updateUserStatus(nickname, false);
                } else {
                    showError("Nie udało się wysłać statusu niegotowości");
                }
            }
        }
    }

    @FXML
    private void handleExitButton(ActionEvent event) throws IOException {
        running = false;

        if (clientConnection != null) {
            if (isReady) {
                clientConnection.sendMessage("UNREADY " + nickname);
            }
            clientConnection.sendMessage("EXIT " + nickname);
            clientConnection.disconnect();
        }

        if (messageReceiver != null) {
            messageReceiver.interrupt();
        }

        goBackToMainMenu();
    }

    private void goBackToMainMenu() throws IOException {
        Stage stage = (Stage) userListView.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    private void switch_to_game() throws IOException {
        running = false;

        if (messageReceiver != null) {
            messageReceiver.interrupt();
        }

        Stage stage = (Stage) userListView.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/uno_game.fxml"));
        Parent root = loader.load();

        UnoController gameController = loader.getController();
        gameController.setupConnection(clientConnection, nickname);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}