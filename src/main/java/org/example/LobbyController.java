/**
 * Kontroler głównego okna lobby gry.
 *
 * <p>Klasa odpowiedzialna za zarządzanie interfejsem użytkownika lobby,
 * obsługę listy graczy, statusów gotowości oraz komunikację z serwerem
 * w celu synchronizacji stanu lobby i przejścia do właściwej rozgrywki.</p>
 *
 * <p>Główne funkcjonalności:
 * <ul>
 *   <li>Wyświetlanie aktualnej listy graczy w lobby</li>
 *   <li>Obsługa zmiany statusu gotowości gracza</li>
 *   <li>Odbieranie i interpretacja komunikatów z serwera</li>
 *   <li>Automatyczne przejście do okna gry po rozpoczęciu rozgrywki</li>
 *   <li>Bezpieczne zarządzanie połączeniem sieciowym</li>
 * </ul>
 * </p>
 *
 * <p>Klasa wymaga poprawnego skonfigurowania połączenia przed użyciem
 * poprzez metodę {@link #setupConnection(ClientConnection, String)}.</p>
 *
 * @see ClientConnection
 * @since 1.0
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
    /** Lista widokowa przechowująca nazwy graczy w lobby */
    @FXML
    private ListView<String> userListView;

    /** Przycisk do zmiany statusu gotowości gracza */
    @FXML
    private Button readyButton;

    /** Przycisk do opuszczenia lobby i powrotu do menu głównego */
    @FXML
    private Button exitButton;

    /** Flaga przechowująca aktualny status gotowości lokalnego gracza */
    private boolean isReady = false;

    /** Obserwowalna lista przechowująca nazwy graczy */
    private ObservableList<String> userList;

    /** Połączenie sieciowe z serwerem */
    private ClientConnection clientConnection;

    /** Nazwa gracza (nickname) */
    private String nickname;

    /** Wątek odbierający komunikaty z serwera */
    private Thread messageReceiver;

    /** Flaga kontrolująca działanie wątku odbierającego komunikaty */
    private volatile boolean running = false;

    /**
     * Inicjalizuje kontroler po załadowaniu widoku FXML.
     *
     * <p>Metoda automatycznie wywoływana przez JavaFX. Inicjalizuje
     * obserwowalną listę użytkowników i wiąże ją z widokiem ListView.</p>
     */
    @FXML
    public void initialize() {
        userList = FXCollections.observableArrayList();
        userListView.setItems(userList);
    }

    /**
     * Konfiguruje połączenie sieciowe i uruchamia odbieranie komunikatów.
     *
     * <p>Metoda musi być wywołana przed rozpoczęciem używania kontrolera.
     * Uruchamia wątek odbierający komunikaty z serwera lub, w przypadku
     * braku połączenia, wyświetla błąd i wraca do menu głównego.</p>
     *
     * @param connection aktywne połączenie z serwerem
     * @param nickname   nazwa gracza do użycia w lobby
     * @throws IllegalArgumentException jeśli połączenie jest null lub nieaktywne
     */
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

    /**
     * Uruchamia wątek odbierający komunikaty z serwera.
     *
     * <p>Wątek działa w tle i nasłuchuje na przychodzące wiadomości.
     * Każda odebrana wiadomość jest przekazywana do głównego wątku JavaFX
     * poprzez {@link Platform#runLater(Runnable)}.</p>
     *
     * <p>W przypadku utraty połączenia, wyświetlany jest błąd i następuje
     * powrót do menu głównego.</p>
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

    /**
     * Przetwarza wiadomość otrzymaną z serwera.
     *
     * <p>Metoda analizuje prefiks wiadomości i wywołuje odpowiednią
     * akcję w zależności od typu komunikatu.</p>
     *
     * <p>Obsługiwane typy wiadomości:
     * <ul>
     *   <li>USERLIST - aktualizacja listy graczy</li>
     *   <li>READY/UNREADY - zmiana statusu gotowości gracza</li>
     *   <li>USER_JOINED/USER_LEFT - powiadomienia o dołączeniu/opuszczeniu lobby</li>
     *   <li>JOIN_SUCCESS - potwierdzenie udanego dołączenia</li>
     *   <li>START_GAME - rozpoczęcie rozgrywki</li>
     *   <li>ERROR - komunikaty błędów</li>
     * </ul>
     * </p>
     *
     * @param message pełna wiadomość tekstowa otrzymana z serwera
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
            showError(message);
        }
    }

    /**
     * Aktualizuje listę graczy w lobby na podstawie danych z serwera.
     *
     * <p>Oczekiwany format danych: "gracz1:READY,gracz2:NOT_READY,..."</p>
     *
     * @param usersStr łańcuch znaków zawierający listę graczy i ich statusy
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
     *
     * @param user  nazwa gracza do zaktualizowania
     * @param ready nowy status gotowości (true = gotowy)
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
     * Aktualizuje wygląd i tekst przycisku gotowości.
     *
     * <p>Metoda zmienia kolor przycisku na zielony, gdy gracz jest gotowy,
     * oraz przywraca domyślny styl, gdy nie jest gotowy. Dodatkowo oblicza
     * liczbę gotowych graczy (możliwość rozszerzenia o wyświetlanie statystyk).</p>
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

        // Możesz dodać logikę, która pokazuje informację o liczbie gotowych graczy
        // np. instrukcja.setText("Gotowych: " + readyCount + "/" + totalPlayers);
    }

    /**
     * Obsługuje kliknięcie przycisku gotowości.
     *
     * <p>Metoda zmienia status gotowości lokalnego gracza i wysyła odpowiedni
     * komunikat do serwera. W przypadku problemów z wysłaniem komunikatu,
     * wyświetlany jest błąd.</p>
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

    /**
     * Obsługuje kliknięcie przycisku wyjścia z lobby.
     *
     * <p>Metoda wysyła do serwera informacje o zmianie statusu na niegotowy
     * (jeśli był gotowy) oraz o opuszczeniu lobby, a następnie zamyka
     * połączenie i wraca do menu głównego.</p>
     *
     * @param event zdarzenie akcji przycisku (niewykorzystywane bezpośrednio)
     * @throws IOException jeśli wystąpi błąd podczas ładowania menu głównego
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
     * Powraca do głównego menu gry.
     *
     * <p>Ładuje widok menu głównego z pliku FXML i zastępuje nim
     * aktualną scenę. Przechodzi w tryb pełnoekranowy.</p>
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
     * Przełącza do głównego okna gry.
     *
     * <p>Metoda wywoływana automatycznie po otrzymaniu komunikatu START_GAME
     * z serwera. Ładuje widok gry, konfiguruje kontroler i przekazuje
     * mu połączenie sieciowe.</p>
     *
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
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

    /**
     * Wyświetla okno dialogowe z komunikatem błędu.
     *
     * @param message treść komunikatu błędu do wyświetlenia
     */
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