/**
 * Klasa kontrolera dla widoku lobby (pokoju oczekiwania) w grze UNO.
 * Zarządza listą graczy, statusem gotowości oraz komunikacją z serwerem.
 * Obsługuje przejścia między scenami lobby, menu głównego i gry.
 *
 */
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

    /** ListView wyświetlająca listę graczy w lobby */
    @FXML
    private ListView<String> userListView;

    /** Przycisk do zmiany statusu gotowości gracza */
    @FXML
    private Button readyButton;

    /** Przycisk do opuszczenia lobby i powrotu do menu głównego */
    @FXML
    private Button exitButton;

    /** Flaga określająca gotowość bieżącego gracza */
    private boolean isReady = false;

    /** Obserwowalna lista przechowująca nazwy graczy */
    private ObservableList<String> userList;

    /** Połączenie klienta z serwerem */
    private ClientConnection clientConnection;

    /** Nazwa gracza (nickname) */
    private String nickname;

    /** Wątek odbierający wiadomości od serwera */
    private Thread messageReceiver;

    /** Flaga określająca stan działania wątku odbiorczego */
    private volatile boolean running = false;

    /**
     * Metoda inicjalizująca kontroler. Wywoływana automatycznie po załadowaniu FXML.
     * Inicjalizuje obserwowalną listę graczy i ustawia ją jako źródło dla ListView.
     */
    @FXML
    public void initialize() {
        userList = FXCollections.observableArrayList();
        userListView.setItems(userList);
    }

    /**
     * Konfiguruje połączenie z serwerem i rozpoczyna odbieranie wiadomości.
     * W przypadku braku połączenia następuje powrót do menu głównego.
     *
     * @param connection obiekt ClientConnection reprezentujący połączenie z serwerem
     * @param nickname nazwa gracza do wyświetlenia w lobby
     */
    public void setupConnection(ClientConnection connection, String nickname) {
        this.clientConnection = connection;
        this.nickname = nickname;

        if (clientConnection != null && clientConnection.isConnected()) {
            startMessageReceiver();
        } else {
            System.err.println("Brak połączenia z serwerem");
            try {
                goBackToMainMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Rozpoczyna wątek odbierający wiadomości od serwera.
     * Wątek działa w tle i przetwarza przychodzące komunikaty.
     */
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
                        System.err.println("Utracono połączenie z serwerem");
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

    /**
     * Przetwarza wiadomość otrzymaną od serwera.
     * Wywołuje odpowiednie metody w zależności od typu komunikatu.
     *
     * @param message wiadomość tekstowa otrzymana od serwera
     */
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
            System.err.println("Błąd od serwera: " + message);
        }
    }

    /**
     * Aktualizuje listę graczy w lobby na podstawie danych otrzymanych z serwera.
     * Dane są w formacie "użytkownik1:READY,użytkownik2:NOT_READY"
     *
     * @param usersStr łańcuch tekstowy zawierający listę graczy i ich statusy
     */
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

    /**
     * Aktualizuje status gotowości pojedynczego gracza na liście.
     * Dodaje lub usuwa symbol ✓ przed nazwą gracza.
     *
     * @param user nazwa gracza
     * @param ready flaga określająca gotowość gracza
     */
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

    /**
     * Aktualizuje wygląd przycisku gotowości na podstawie stanu bieżącego gracza.
     * Zmienia tekst, kolor tła i liczbę gotowych graczy.
     */
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
    }

    /**
     * Obsługuje kliknięcie przycisku gotowości.
     * W zależności od aktualnego stanu wysyła do serwera komunikat READY lub UNREADY.
     * Aktualizuje lokalny stan gracza.
     */
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
                    System.err.println("Nie udało się wysłać statusu gotowości");
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
                    System.err.println("Nie udało się wysłać statusu niegotowości");
                }
            }
        }
    }

    /**
     * Obsługuje kliknięcie przycisku wyjścia z lobby.
     * Wysyła do serwera komunikaty UNREADY i EXIT, zamyka połączenie,
     * przerywa wątek odbiorczy i wraca do menu głównego.
     *
     * @param event zdarzenie ActionEvent związane z kliknięciem przycisku
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML menu głównego
     */
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

    /**
     * Wraca do menu głównego.
     * Ładuje plik FXML main_menu.fxml i ustawia go jako aktywną scenę.
     *
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
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

    /**
     * Przełącza do sceny gry UNO.
     * Zatrzymuje wątek odbiorczy, ładuje plik FXML uno_game.fxml,
     * przekazuje połączenie i nickname do kontrolera gry oraz ustawia nową scenę.
     *
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML gry
     */
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
}