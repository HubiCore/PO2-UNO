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
    private static final Logger logger = Logger.getInstance();

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
        logger.info("Inicjalizacja LobbyController");
        userList = FXCollections.observableArrayList();
        userListView.setItems(userList);
        logger.info("LobbyController zainicjalizowany");
    }

    public void setupConnection(ClientConnection connection, String nickname) {
        logger.info("Konfiguracja połączenia dla użytkownika: " + nickname);
        this.clientConnection = connection;
        this.nickname = nickname;

        if (clientConnection != null && clientConnection.isConnected()) {
            logger.info("Połączenie aktywne, uruchamiam odbieranie wiadomości");
            startMessageReceiver();
        } else {
            logger.error("Brak połączenia z serwerem");
            showError("Brak połączenia z serwerem");
            try {
                goBackToMainMenu();
            } catch (IOException e) {
                logger.error(e, "Błąd powrotu do menu głównego");
            }
        }
    }

    private void startMessageReceiver() {
        if (running) {
            logger.warning("Odbieranie wiadomości już działa");
            return; // Już działa
        }

        running = true;
        messageReceiver = new Thread(() -> {
            logger.info("Wątek odbierania wiadomości uruchomiony");
            while (running && clientConnection != null && clientConnection.isConnected()) {
                String message = clientConnection.receiveMessage();
                if (message == null) {
                    // Połączenie zostało zamknięte
                    logger.error("Utracono połączenie z serwerem");
                    Platform.runLater(() -> {
                        showError("Utracono połączenie z serwerem");
                        try {
                            goBackToMainMenu();
                        } catch (IOException ex) {
                            logger.error(ex, "Błąd powrotu do menu głównego");
                        }
                    });
                    break;
                }
                Platform.runLater(() -> handleServerMessage(message));
            }
            running = false;
            logger.info("Wątek odbierania wiadomości zakończony");
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }

    private void handleServerMessage(String message) {
        logger.debug("Otrzymano od serwera: " + message);

        if (message.startsWith("USERLIST ")) {
            logger.debug("Aktualizacja listy użytkowników");
            updateUserList(message.substring(9));
        } else if (message.startsWith("READY ")) {
            String user = message.substring(6);
            logger.debug("Użytkownik gotowy: " + user);
            updateUserStatus(user, true);
        } else if (message.startsWith("UNREADY ")) {
            String user = message.substring(8);
            logger.debug("Użytkownik niegotowy: " + user);
            updateUserStatus(user, false);
        } else if (message.startsWith("USER_JOINED ")) {
            String user = message.substring(12);
            logger.info("Gracz " + user + " dołączył");
        } else if (message.startsWith("USER_LEFT ")) {
            String user = message.substring(10);
            logger.info("Gracz " + user + " opuścił lobby");
        } else if (message.startsWith("JOIN_SUCCESS ")) {
            String user = message.substring(13);
            logger.info("Witaj " + user + "!");
        } else if (message.startsWith("START_GAME")) {
            logger.info("Rozpoczynanie gry...");
            try {
                switch_to_game();
            } catch (IOException e) {
                logger.error(e, "Błąd przejścia do gry");
            }
        } else if (message.startsWith("ERROR")) {
            logger.error("Błąd serwera: " + message);
            showError(message);
        } else {
            logger.debug("Nieznany typ wiadomości: " + message);
        }
    }

    private void updateUserList(String usersStr) {
        Platform.runLater(() -> {
            logger.debug("Aktualizuję listę użytkowników: " + usersStr);
            userList.clear();
            if (usersStr == null || usersStr.trim().isEmpty()) {
                logger.warning("Pusta lista użytkowników");
                return;
            }

            String[] users = usersStr.split(",");
            logger.debug("Liczba użytkowników: " + users.length);

            for (String userEntry : users) {
                if (!userEntry.isEmpty()) {
                    // Format: "username:READY" lub "username:NOT_READY"
                    String[] parts = userEntry.split(":");
                    if (parts.length == 2) {
                        String username = parts[0].trim();
                        String status = parts[1].trim();

                        if (status.equals("READY")) {
                            userList.add("✓ " + username);
                            logger.debug("Dodano gotowego użytkownika: " + username);
                        } else {
                            userList.add(username);
                            logger.debug("Dodano niegotowego użytkownika: " + username);
                        }
                    } else {
                        // Dla kompatybilności z poprzednimi wersjami
                        userList.add(userEntry);
                        logger.debug("Dodano użytkownika (stary format): " + userEntry);
                    }
                }
            }

            updateReadyButtonState();
            logger.debug("Lista użytkowników zaktualizowana: " + userList.size() + " użytkowników");
        });
    }

    private void updateUserStatus(String user, boolean ready) {
        Platform.runLater(() -> {
            logger.debug("Aktualizacja statusu użytkownika: " + user + " -> " + (ready ? "gotowy" : "niegotowy"));
            for (int i = 0; i < userList.size(); i++) {
                String listUser = userList.get(i);
                String baseUser = listUser.replace("✓ ", "").trim();

                if (baseUser.equals(user)) {
                    if (ready) {
                        userList.set(i, "✓ " + user);
                        logger.debug("Ustawiono gotowość dla: " + user);
                    } else {
                        userList.set(i, user);
                        logger.debug("Usunięto gotowość dla: " + user);
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
            logger.debug("Przycisk gotowości: gotowy");
        } else {
            readyButton.setText("Gotowy?");
            readyButton.setStyle("");
            logger.debug("Przycisk gotowości: niegotowy");
        }

        // Sprawdź czy można rozpocząć grę (co najmniej 2 graczy gotowych)
        int readyCount = 0;
        int totalPlayers = userList.size();

        for (String user : userList) {
            if (user.startsWith("✓ ")) {
                readyCount++;
            }
        }

        logger.debug("Gotowych graczy: " + readyCount + "/" + totalPlayers);

        if (readyCount >= 2 && totalPlayers >= 2) {
            logger.info("Można rozpocząć grę (" + readyCount + "/" + totalPlayers + " gotowych)");
        }
    }

    @FXML
    private void handleReadyButton() {
        logger.debug("Kliknięto przycisk gotowości. Aktualny stan: " + (isReady ? "gotowy" : "niegotowy"));

        if (!isReady) {
            if (clientConnection != null && clientConnection.isConnected()) {
                boolean sent = clientConnection.sendMessage("READY " + nickname);
                if (sent) {
                    isReady = true;
                    logger.info("Wysłano status gotowości dla: " + nickname);
                    // Natychmiastowa aktualizacja lokalna
                    updateUserStatus(nickname, true);
                } else {
                    logger.error("Nie udało się wysłać statusu gotowości");
                    showError("Nie udało się wysłać statusu gotowości");
                }
            }
        } else {
            if (clientConnection != null && clientConnection.isConnected()) {
                boolean sent = clientConnection.sendMessage("UNREADY " + nickname);
                if (sent) {
                    isReady = false;
                    logger.info("Wysłano status niegotowości dla: " + nickname);
                    // Natychmiastowa aktualizacja lokalna
                    updateUserStatus(nickname, false);
                } else {
                    logger.error("Nie udało się wysłać statusu niegotowości");
                    showError("Nie udało się wysłać statusu niegotowości");
                }
            }
        }
    }

    @FXML
    private void handleExitButton(ActionEvent event) throws IOException {
        logger.info("Wychodzę z lobby dla użytkownika: " + nickname);
        running = false;

        if (clientConnection != null) {
            if (isReady) {
                logger.debug("Wysyłanie statusu niegotowości przed wyjściem");
                clientConnection.sendMessage("UNREADY " + nickname);
            }
            logger.debug("Wysyłanie komendy EXIT");
            clientConnection.sendMessage("EXIT " + nickname);
            clientConnection.disconnect();
            logger.info("Połączenie zamknięte");
        }

        if (messageReceiver != null) {
            logger.debug("Przerywanie wątku odbierania wiadomości");
            messageReceiver.interrupt();
        }

        goBackToMainMenu();
        logger.info("Powrót do menu głównego");
    }

    private void goBackToMainMenu() throws IOException {
        logger.debug("Przełączanie do menu głównego");
        Stage stage = (Stage) userListView.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
        logger.info("Menu główne załadowane");
    }

    private void switch_to_game() throws IOException {
        logger.info("Przechodzę do gry...");
        running = false;

        if (messageReceiver != null) {
            logger.debug("Przerywanie wątku odbierania wiadomości lobby");
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

        logger.info("Gra załadowana dla użytkownika: " + nickname);
    }

    private void showError(String message) {
        logger.error("Wyświetlam błąd: " + message);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}