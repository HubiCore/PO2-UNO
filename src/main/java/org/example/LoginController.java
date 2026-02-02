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
    private static final Logger logger = Logger.getInstance();

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
        logger.info("Inicjalizacja LoginController");

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
        // logger.debug("Pola testowe wypełnione");

        logger.info("LoginController zainicjalizowany");
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
        logger.info("=== ROZPOCZĘCIE LOGOWANIA ===");

        savedLoginText = loginTextField.getText().trim();
        savedPassword = passwordField.getText();

        logger.info("Próba logowania użytkownika: " + savedLoginText);
        errorLabel.setVisible(false);

        if (savedLoginText.isEmpty()) {
            logger.warning("Nie podano nicku");
            showError("Wprowadź nick!");
            return;
        }

        if (!savedLoginText.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+")) {
            logger.warning("Nieprawidłowy nick (tylko litery): " + savedLoginText);
            showError("Login może zawierać tylko litery!");
            return;
        }

        if (savedPassword.isEmpty()) {
            logger.warning("Nie podano hasła dla użytkownika: " + savedLoginText);
            showError("Wprowadź hasło!");
            return;
        }

        if (savedPassword.length() < 6) {
            logger.warning("Hasło za krótkie dla użytkownika: " + savedLoginText);
            showError("Hasło musi mieć co najmniej 6 znaków!");
            return;
        }

        String hashedPassword = hashPasswordUsingAuthService(savedPassword);
        logger.debug("Hash hasła wygenerowany");

        if (clientConnection != null && clientConnection.isConnected()) {
            logger.info("Zamykam istniejące połączenie...");
            clientConnection.disconnect();
        }

        clientConnection = new ClientConnection();
        logger.info("Łączę z serwerem (localhost:2137)...");

        boolean connected = clientConnection.connect();
        logger.info("Status połączenia: " + connected);

        if (!connected) {
            logger.error("Nie udało się połączyć z serwerem");
            showError("Nie udało się połączyć z serwerem");
            clientConnection = null;
            return;
        }

        String loginData = "LOGIN " + savedLoginText + ":" + hashedPassword;
        logger.info("Wysyłam do serwera: " + savedLoginText);

        boolean sent = clientConnection.sendMessage(loginData);
        logger.info("Status wysyłania: " + sent);

        if (!sent) {
            logger.error("Nie udało się wysłać danych logowania");
            showError("Nie udało się wysłać danych logowania");
            clientConnection.disconnect();
            clientConnection = null;
            return;
        }

        // Odbieraj wiadomości w pętli aż do otrzymania LOGIN_SUCCESS
        long startTime = System.currentTimeMillis();
        long timeout = 100000;
        boolean loginProcessed = false;
        logger.info("Oczekiwanie na odpowiedź serwera (timeout: " + timeout + "ms)");

        while (!loginProcessed && (System.currentTimeMillis() - startTime) < timeout) {
            String serverResponse = clientConnection.receiveMessageWithTimeout(1000);

            if (serverResponse == null) {
                continue; // Kontynuuj oczekiwanie
            }

            logger.debug("Otrzymana odpowiedź: " + serverResponse);

            if (serverResponse.startsWith("LOGIN_SUCCESS")) {
                logger.info("Logowanie pomyślne dla użytkownika: " + savedLoginText);
                loginProcessed = true;
                // Przechodzimy do lobby
                Platform.runLater(() -> {
                    try {
                        switch_to_lobby(event);
                    } catch (IOException e) {
                        logger.error(e, "Błąd przejścia do lobby");
                        showError("Błąd przejścia do lobby: " + e.getMessage());
                    }
                });
                break;
            } else if (serverResponse.startsWith("LOGIN_ERROR")) {
                String errorMessage = serverResponse.substring(11);
                logger.error("Błąd logowania: " + errorMessage);
                showError("Błąd logowania: " + errorMessage);
                clientConnection.disconnect();
                clientConnection = null;
                loginProcessed = true;
                break;
            } else {
                // Ignoruj inne wiadomości (USERLIST, USER_JOINED itp.)
                logger.debug("Ignoruję wiadomość podczas logowania: " + serverResponse);
                continue;
            }
        }

        if (!loginProcessed) {
            logger.error("Brak odpowiedzi od serwera (timeout)");
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
            logger.debug("Haszowanie hasła przy użyciu AuthenticationService");
            java.lang.reflect.Method method = AuthenticationService.class.getDeclaredMethod("hashPassword", String.class);
            method.setAccessible(true);
            return (String) method.invoke(authService, password);
        } catch (Exception e) {
            logger.error("AuthenticationService nie ma metody hashPassword: " + e.getMessage());
            logger.error(e, "Szczegóły błędu");
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
        logger.debug("Używanie zapasowej metody haszowania (MD5)");
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String result = hexString.toString();
            logger.debug("Hash MD5 wygenerowany");
            return result;
        } catch (java.security.NoSuchAlgorithmException e) {
            logger.error("Błąd hashowania: " + e.getMessage());
            logger.error(e, "Szczegóły błędu");
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
        logger.info("Przełączam do głównego menu z logowania");

        if (clientConnection != null && clientConnection.isConnected()) {
            clientConnection.disconnect();
            logger.debug("Połączenie z serwerem zamknięte");
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

        logger.info("Przełączono do głównego menu");
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
        logger.info("Przechodzę do lobby dla użytkownika: " + savedLoginText);
        logger.debug("Połączenie aktywne: " + (clientConnection != null && clientConnection.isConnected()));

        if (clientConnection == null || !clientConnection.isConnected()) {
            logger.error("Brak połączenia z serwerem");
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

        logger.info("Przejście do lobby zakończone sukcesem dla użytkownika: " + savedLoginText);
    }

    /**
     * Wyświetla komunikat o błędzie w interfejsie użytkownika.
     *
     * @param message treść komunikatu błędu
     */
    private void showError(String message) {
        logger.error("BŁĄD LOGOWANIA: " + message);
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