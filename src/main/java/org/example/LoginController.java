/**
 * Kontroler obsługujący logowanie użytkownika do aplikacji.
 * Zarządza interfejsem logowania, walidacją danych, komunikacją z serwerem
 * oraz przejściem do głównego menu lub lobby po pomyślnym uwierzytelnieniu.
 *
 */
package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.UnaryOperator;

public class LoginController {
    /**
     * Pole tekstowe do wprowadzenia loginu użytkownika.
     * Akceptuje tylko litery (w tym polskie znaki diakrytyczne) do 20 znaków.
     */
    @FXML
    private TextField loginTextField;

    /**
     * Pole do wprowadzenia hasła użytkownika.
     * Akceptuje do 30 znaków dowolnego typu.
     */
    @FXML
    private PasswordField passwordField;

    /**
     * Etykieta wyświetlająca komunikaty o błędach logowania.
     */
    @FXML
    private Label errorLabel;

    /**
     * Przechowuje login użytkownika po pomyślnym zatwierdzeniu formularza.
     */
    private String savedLoginText;

    /**
     * Przechowuje hasło użytkownika po pomyślnym zatwierdzeniu formularza.
     */
    private String savedPassword;

    /**
     * Połączenie klienta z serwerem gry.
     */
    private ClientConnection clientConnection;

    /**
     * Serwis odpowiedzialny za uwierzytelnianie użytkowników.
     */
    private AuthenticationService authService;

    /**
     * Inicjalizuje kontroler po załadowaniu pliku FXML.
     * Konfiguruje filtry walidacyjne dla pól tekstowych, ukrywa etykietę błędów
     * i tworzy instancję serwisu uwierzytelniania.
     * Zawiera także zakomentowany kod do automatycznego wypełnienia pól dla celów testowych.
     */
    @FXML
    public void initialize() {
        authService = new AuthenticationService();

        UnaryOperator<TextFormatter.Change> loginFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.length() > 20) return null;
            if (!newText.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]*")) return null;
            return change;
        };
        loginTextField.setTextFormatter(new TextFormatter<>(loginFilter));

        UnaryOperator<TextFormatter.Change> passwordFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.length() > 30) return null;
            return change;
        };
        passwordField.setTextFormatter(new TextFormatter<>(passwordFilter));

        errorLabel.setVisible(false);

        // Debug: Automatyczne wypełnienie pól dla testów
        // loginTextField.setText("test");
        // passwordField.setText("test123");
    }

    /**
     * Obsługuje kliknięcie przycisku "Graj".
     * Wykonuje walidację danych logowania, nawiązuje połączenie z serwerem,
     * wysyła dane uwierzytelniające i oczekuje na odpowiedź serwera.
     *
     * @param event zdarzenie akcji przycisku
     * @throws IOException w przypadku błędu ładowania widoku lobby
     */
    @FXML
    private void handlePlayButton(ActionEvent event) throws IOException {
        savedLoginText = loginTextField.getText().trim();
        savedPassword = passwordField.getText();

        errorLabel.setVisible(false);

        System.out.println("=== ROZPOCZĘCIE LOGOWANIA ===");
        System.out.println("Login: " + savedLoginText);

        if (savedLoginText.isEmpty()) {
            showError("Wprowadź nick!");
            return;
        }

        if (!savedLoginText.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+")) {
            showError("Login może zawierać tylko litery!");
            return;
        }

        if (savedPassword.isEmpty()) {
            showError("Wprowadź hasło!");
            return;
        }

        if (savedPassword.length() < 6) {
            showError("Hasło musi mieć co najmniej 6 znaków!");
            return;
        }

        String hashedPassword = hashPasswordUsingAuthService(savedPassword);
        System.out.println("Hash hasła: " + hashedPassword);

        if (clientConnection != null && clientConnection.isConnected()) {
            System.out.println("Zamykam istniejące połączenie...");
            clientConnection.disconnect();
        }

        clientConnection = new ClientConnection();
        System.out.println("Łączę z serwerem (localhost:2137)...");

        boolean connected = clientConnection.connect();
        System.out.println("Status połączenia: " + connected);

        if (!connected) {
            showError("Nie udało się połączyć z serwerem");
            clientConnection = null;
            return;
        }

        String loginData = "LOGIN " + savedLoginText + ":" + hashedPassword;
        System.out.println("Wysyłam do serwera: " + loginData);

        boolean sent = clientConnection.sendMessage(loginData);
        System.out.println("Status wysyłania: " + sent);

        if (!sent) {
            showError("Nie udało się wysłać danych logowania");
            clientConnection.disconnect();
            clientConnection = null;
            return;
        }

        // Odbieraj wiadomości w pętli aż do otrzymania LOGIN_SUCCESS
        long startTime = System.currentTimeMillis();
        long timeout = 100000;
        boolean loginProcessed = false;

        while (!loginProcessed && (System.currentTimeMillis() - startTime) < timeout) {
            String serverResponse = clientConnection.receiveMessageWithTimeout(1000);

            if (serverResponse == null) {
                continue; // Kontynuuj oczekiwanie
            }

            System.out.println("Otrzymana odpowiedź: " + serverResponse);

            if (serverResponse.startsWith("LOGIN_SUCCESS")) {
                System.out.println("Logowanie pomyślne!");
                loginProcessed = true;
                // Przechodzimy do lobby
                Platform.runLater(() -> {
                    try {
                        switch_to_lobby(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                        showError("Błąd przejścia do lobby: " + e.getMessage());
                    }
                });
                break;
            } else if (serverResponse.startsWith("LOGIN_ERROR")) {
                String errorMessage = serverResponse.substring(11);
                showError("Błąd logowania: " + errorMessage);
                clientConnection.disconnect();
                clientConnection = null;
                loginProcessed = true;
                break;
            } else {
                // Ignoruj inne wiadomości (USERLIST, USER_JOINED itp.)
                System.out.println("Ignoruję wiadomość podczas logowania: " + serverResponse);
                continue;
            }
        }

        if (!loginProcessed) {
            showError("Brak odpowiedzi od serwera (timeout)");
            if (clientConnection != null) {
                clientConnection.disconnect();
                clientConnection = null;
            }
        }
    }

    /**
     * Hashuje hasło używając serwisu AuthenticationService.
     * Wykorzystuje refleksję do wywołania prywatnej metody hashPassword.
     * W przypadku niepowodzenia używa zapasowej metody hashowania MD5.
     *
     * @param password hasło do zahashowania
     * @return zahashowane hasło w formacie tekstowym
     */
    private String hashPasswordUsingAuthService(String password) {
        try {
            java.lang.reflect.Method method = AuthenticationService.class.getDeclaredMethod("hashPassword", String.class);
            method.setAccessible(true);
            return (String) method.invoke(authService, password);
        } catch (Exception e) {
            System.err.println("AuthenticationService nie ma metody hashPassword: " + e.getMessage());
            return fallbackHashPassword(password);
        }
    }

    /**
     * Zapasowa metoda hashowania hasła używająca algorytmu MD5.
     * Stosowana gdy serwis AuthenticationService nie jest dostępny.
     *
     * @param password hasło do zahashowania
     * @return zahashowane hasło w formacie szesnastkowym
     */
    private String fallbackHashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("Błąd hashowania: " + e.getMessage());
            return password;
        }
    }

    /**
     * Przełącza widok na główne menu aplikacji.
     * Zamyka istniejące połączenie z serwerem przed przejściem.
     *
     * @param event zdarzenie akcji przycisku
     * @throws IOException w przypadku błędu ładowania widoku głównego menu
     */
    @FXML
    private void switch_to_main_menu(ActionEvent event) throws IOException {
        if (clientConnection != null && clientConnection.isConnected()) {
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
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    /**
     * Przełącza widok na lobby gry po pomyślnym logowaniu.
     * Przekazuje połączenie z serwerem i dane użytkownika do kontrolera lobby.
     *
     * @param event zdarzenie akcji przycisku
     * @throws IOException w przypadku błędu ładowania widoku lobby
     */
    @FXML
    private void switch_to_lobby(ActionEvent event) throws IOException {
        System.out.println("Przechodzę do lobby...");
        System.out.println("Połączenie aktywne: " + (clientConnection != null && clientConnection.isConnected()));
        System.out.println("Nickname: " + savedLoginText);

        if (clientConnection == null || !clientConnection.isConnected()) {
            showError("Brak połączenia z serwerem");
            return;
        }

        Stage stage;
        Scene scene;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobby.fxml"));
        Parent root = loader.load();
        LobbyController lobbyController = loader.getController();

        lobbyController.setupConnection(clientConnection, savedLoginText);

        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style_log_join.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        System.out.println("Przejście do lobby zakończone sukcesem");
    }

    /**
     * Wyświetla komunikat o błędzie w interfejsie użytkownika.
     *
     * @param message treść komunikatu błędu
     */
    private void showError(String message) {
        System.err.println("BŁĄD: " + message);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Zwraca zapisany login użytkownika.
     *
     * @return login użytkownika
     */
    public String getSavedLoginText() {
        return savedLoginText;
    }

    /**
     * Zwraca zapisane hasło użytkownika.
     *
     * @return hasło użytkownika
     */
    public String getSavedPassword() {
        return savedPassword;
    }
}