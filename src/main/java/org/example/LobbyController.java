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
import java.util.Timer;
import java.util.TimerTask;

public class LobbyController {
    @FXML
    private ListView<String> userListView;
    @FXML
    private Button readyButton;
    @FXML
    private Button exitButton;
    @FXML
    private Label timerLabel;
    @FXML
    private ProgressBar timerProgressBar;

    private boolean isReady = false;
    private ObservableList<String> userList;
    private ClientConnection clientConnection;
    private String nickname;
    private Thread messageReceiver;
    private volatile boolean running = false;

    // Timer variables
    private Timer lobbyTimer;
    private TimerTask timeoutTask;
    private final int LOBBY_TIMEOUT_SECONDS = 5 * 60; // 5 minut = 300 sekund
    private int remainingSeconds = LOBBY_TIMEOUT_SECONDS;
    private boolean timeoutOccurred = false;

    @FXML
    public void initialize() {
        userList = FXCollections.observableArrayList();
        userListView.setItems(userList);

        // Inicjalizacja timer label i progress bar
        if (timerLabel != null) {
            timerLabel.setText(formatTime(remainingSeconds));
        }
        if (timerProgressBar != null) {
            timerProgressBar.setProgress(1.0); // Pełny pasek na początku
        }
    }

    public void setupConnection(ClientConnection connection, String nickname) {
        this.clientConnection = connection;
        this.nickname = nickname;

        if (clientConnection != null && clientConnection.isConnected()) {
            System.out.println("LobbyController: Połączenie ustawione dla " + nickname);
            startMessageReceiver();
            startLobbyTimer();
        } else {
            System.err.println("LobbyController: Brak połączenia z serwerem");
            showError("Brak połączenia z serwerem");
            try {
                goBackToMainMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startLobbyTimer() {
        System.out.println("Rozpoczynam timer lobby (5 minut)");

        // Anuluj istniejący timer jeśli istnieje
        cancelLobbyTimer();

        lobbyTimer = new Timer(true);
        remainingSeconds = LOBBY_TIMEOUT_SECONDS;
        timeoutOccurred = false;

        // Aktualizuj UI co sekundę
        TimerTask updateTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    remainingSeconds--;

                    // Aktualizuj UI
                    if (timerLabel != null) {
                        timerLabel.setText(formatTime(remainingSeconds));
                    }
                    if (timerProgressBar != null) {
                        double progress = (double) remainingSeconds / LOBBY_TIMEOUT_SECONDS;
                        timerProgressBar.setProgress(progress);

                        // Zmień kolor paska w zależności od czasu
                        if (remainingSeconds < 60) { // Ostatnia minuta - czerwony
                            timerProgressBar.setStyle("-fx-accent: red;");
                        } else if (remainingSeconds < 180) { // 3 minuty - pomarańczowy
                            timerProgressBar.setStyle("-fx-accent: orange;");
                        } else { // Powyżej 3 minut - zielony
                            timerProgressBar.setStyle("-fx-accent: green;");
                        }
                    }
                });
            }
        };

        // Zadanie timeoutu - uruchomi się po 5 minutach
        timeoutTask = new TimerTask() {
            @Override
            public void run() {
                if (!timeoutOccurred) {
                    timeoutOccurred = true;
                    handleLobbyTimeout();
                }
            }
        };

        // Uruchom aktualizację UI co sekundę
        lobbyTimer.scheduleAtFixedRate(updateTask, 0, 1000);
        // Uruchom timeout po 5 minutach
        lobbyTimer.schedule(timeoutTask, LOBBY_TIMEOUT_SECONDS * 1000);

        System.out.println("Timer lobby uruchomiony. Czas: " + LOBBY_TIMEOUT_SECONDS + " sekund");
    }

    private void cancelLobbyTimer() {
        if (lobbyTimer != null) {
            lobbyTimer.cancel();
            lobbyTimer.purge();
            lobbyTimer = null;
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }

    private void resetLobbyTimer() {
        System.out.println("Resetuję timer lobby");
        startLobbyTimer(); // Uruchom timer od nowa
    }

    private void handleLobbyTimeout() {
        System.out.println("Timeout lobby! Przekroczono 5 minut oczekiwania.");

        Platform.runLater(() -> {
            // Sprawdź czy użytkownik już nie wyszedł
            if (!running) {
                return;
            }

            timeoutOccurred = true;

            // Wyłącz przyciski
            if (readyButton != null) {
                readyButton.setDisable(true);
            }

            if (exitButton != null) {
                exitButton.setDisable(true);
            }

            // Pokaż alert informacyjny
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Czas oczekiwania wygasł");
            alert.setHeaderText("Limit czasu lobby został przekroczony");
            alert.setContentText("Oczekiwanie w lobby trwało dłużej niż 5 minut.\nZostaniesz przeniesiony do menu głównego.");

            // Ustaw timeout na alert (automatyczne zamknięcie po 3 sekundach)
            Timer closeAlertTimer = new Timer(true);
            closeAlertTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        if (alert.isShowing()) {
                            alert.close();
                            try {
                                goBackToMainMenu();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }, 3000);

            alert.showAndWait();

            // Jeśli alert został zamknięty ręcznie
            if (alert.isShowing()) {
                alert.close();
            }

            // Wróć do menu głównego
            try {
                goBackToMainMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void startMessageReceiver() {
        if (running) {
            return; // Już działa
        }

        running = true;
        messageReceiver = new Thread(() -> {
            System.out.println("LobbyController: Wątek odbioru wiadomości uruchomiony");

            while (running && clientConnection != null && clientConnection.isConnected()) {
                try {
                    String message = clientConnection.receiveMessage();
                    if (message == null) {
                        // Połączenie zostało zamknięte
                        System.err.println("LobbyController: Odebrano null od serwera - rozłączanie");

                        // Sprawdź czy timeout już wystąpił
                        if (!timeoutOccurred) {
                            Platform.runLater(() -> {
                                showError("Utracono połączenie z serwerem. Pozostaniesz w lobby przez maksymalnie 5 minut.");
                            });
                            // Nie przechodzimy od razu do menu głównego - czekamy na timeout timer
                        }
                        break;
                    }

                    if (!message.trim().isEmpty()) {
                        System.out.println("LobbyController: Otrzymano: " + message);
                        Platform.runLater(() -> handleServerMessage(message));
                    }

                    // Krótka pauza, aby nie obciążać CPU
                    Thread.sleep(50);

                } catch (Exception e) {
                    System.err.println("LobbyController: Błąd w wątku odbioru: " + e.getMessage());
                    if (!running) break; // Jeśli celowo zatrzymano, wyjdź

                    // Sprawdź czy timeout już wystąpił
                    if (!timeoutOccurred) {
                        Platform.runLater(() -> {
                            showError("Błąd połączenia: " + e.getMessage() + ". Pozostaniesz w lobby przez maksymalnie 5 minut.");
                        });
                        // Nie przechodzimy od razu do menu głównego - czekamy na timeout timer
                    }
                    break;
                }
            }

            running = false;
            System.out.println("LobbyController: Wątek odbioru wiadomości zakończony");

            // Po utracie połączenia, timer będzie nadal działał i po 5 minutach wykona handleLobbyTimeout()
        });

        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }

    private void handleServerMessage(String message) {
        System.out.println("LobbyController: Przetwarzam wiadomość: " + message);

        if (message.startsWith("USERLIST ")) {
            updateUserList(message.substring(9));
            resetLobbyTimer(); // Resetuj timer przy otrzymaniu listy graczy
        } else if (message.startsWith("READY ")) {
            String user = message.substring(6);
            updateUserStatus(user, true);
            resetLobbyTimer(); // Resetuj timer przy zmianie statusu
        } else if (message.startsWith("UNREADY ")) {
            String user = message.substring(8);
            updateUserStatus(user, false);
            resetLobbyTimer(); // Resetuj timer przy zmianie statusu
        } else if (message.startsWith("USER_JOINED ")) {
            String user = message.substring(12);
            System.out.println("Gracz " + user + " dołączył");
            resetLobbyTimer(); // Resetuj timer kiedy ktoś dołączy
        } else if (message.startsWith("USER_LEFT ")) {
            String user = message.substring(10);
            System.out.println("Gracz " + user + " opuścił lobby");
            resetLobbyTimer(); // Resetuj timer kiedy ktoś wyjdzie
        } else if (message.startsWith("JOIN_SUCCESS ")) {
            String user = message.substring(13);
            System.out.println("Witaj " + user + "!");
        } else if (message.startsWith("START_GAME")) {
            System.out.println("LobbyController: Rozpoczynam grę!");
            try {
                // Anuluj timer przed przejściem do gry
                cancelLobbyTimer();
                switch_to_game();
            } catch (IOException e) {
                e.printStackTrace();
                showError("Błąd podczas przełączania do gry: " + e.getMessage());
            }
        } else if (message.startsWith("ERROR")) {
            showError(message);
        } else if (message.equals("Server received: LIST")) {
            // Ignoruj tę wiadomość - to tylko potwierdzenie od serwera
        } else {
            System.out.println("LobbyController: Nieznana wiadomość: " + message);
        }
    }

    private void updateUserList(String usersStr) {
        Platform.runLater(() -> {
            userList.clear();
            if (usersStr == null || usersStr.trim().isEmpty()) {
                System.out.println("LobbyController: Pusta lista graczy");
                return;
            }

            System.out.println("LobbyController: Aktualizuję listę graczy: " + usersStr);

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
            System.out.println("LobbyController: Aktualizuję status " + user + " na " + (ready ? "READY" : "NOT_READY"));

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
    }

    @FXML
    private void handleReadyButton() {
        System.out.println("LobbyController: Kliknięto przycisk gotowości, obecny stan: " + (isReady ? "READY" : "NOT_READY"));

        if (!isReady) {
            if (clientConnection != null && clientConnection.isConnected()) {
                boolean sent = clientConnection.sendMessage("READY " + nickname);
                if (sent) {
                    isReady = true;
                    // Natychmiastowa aktualizacja lokalna
                    updateUserStatus(nickname, true);
                    resetLobbyTimer(); // Reset timer przy zmianie statusu
                    System.out.println("LobbyController: Wysłano READY dla " + nickname);
                } else {
                    showError("Nie udało się wysłać statusu gotowości");
                }
            } else {
                showError("Brak połączenia z serwerem");
            }
        } else {
            if (clientConnection != null && clientConnection.isConnected()) {
                boolean sent = clientConnection.sendMessage("UNREADY " + nickname);
                if (sent) {
                    isReady = false;
                    // Natychmiastowa aktualizacja lokalna
                    updateUserStatus(nickname, false);
                    resetLobbyTimer(); // Reset timer przy zmianie statusu
                    System.out.println("LobbyController: Wysłano UNREADY dla " + nickname);
                } else {
                    showError("Nie udało się wysłać statusu niegotowości");
                }
            } else {
                showError("Brak połączenia z serwerem");
            }
        }
    }

    @FXML
    private void handleExitButton(ActionEvent event) throws IOException {
        System.out.println("LobbyController: Kliknięto przycisk wyjścia");

        running = false;

        // Anuluj timer
        cancelLobbyTimer();

        if (clientConnection != null) {
            if (isReady) {
                clientConnection.sendMessage("UNREADY " + nickname);
            }
            clientConnection.sendMessage("EXIT " + nickname);
            clientConnection.disconnect();
            System.out.println("LobbyController: Rozłączono z serwerem");
        }

        if (messageReceiver != null) {
            messageReceiver.interrupt();
        }

        goBackToMainMenu();
    }

    private void goBackToMainMenu() throws IOException {
        System.out.println("LobbyController: Wracam do menu głównego");

        // Upewnij się, że timer jest anulowany
        cancelLobbyTimer();

        Stage stage = (Stage) userListView.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    private void switch_to_game() throws IOException {
        System.out.println("LobbyController: Przechodzę do gry");

        running = false;

        // Anuluj timer
        cancelLobbyTimer();

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
        stage.setFullScreen(true);
        stage.show();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            System.err.println("LobbyController: Błąd - " + message);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}