package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
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
    @FXML
    private TextField loginTextField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private String savedLoginText;
    private String savedPassword;
    private ClientConnection clientConnection;

    @FXML
    public void initialize() {
        // Max 20 znaków dla loginu - tylko litery
        UnaryOperator<TextFormatter.Change> loginFilter = change -> {
            String newText = change.getControlNewText();

            // Sprawdź długość
            if (newText.length() > 20) {
                return null;
            }

            // Sprawdź czy tylko litery (włącznie z polskimi znakami)
            if (!newText.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]*")) {
                return null;
            }

            return change;
        };
        loginTextField.setTextFormatter(new TextFormatter<>(loginFilter));

        // Max 30 znaków dla hasła
        UnaryOperator<TextFormatter.Change> passwordFilter = change -> {
            String newText = change.getControlNewText();

            if (newText.length() > 30) {
                return null;
            }

            return change;
        };
        passwordField.setTextFormatter(new TextFormatter<>(passwordFilter));

        // Ukryj etykietę błędu na starcie
        errorLabel.setVisible(false);
    }

    @FXML
    private void handlePlayButton(ActionEvent event) throws IOException {
        savedLoginText = loginTextField.getText().trim();
        savedPassword = passwordField.getText();

        // Resetuj błędy
        errorLabel.setVisible(false);

        // Walidacja loginu
        if (savedLoginText.isEmpty()) {
            showError("Wprowadź nick!");
            return;
        }

        // Sprawdź czy login zawiera tylko litery
        if (!savedLoginText.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+")) {
            showError("Login może zawierać tylko litery (bez cyfr i znaków specjalnych)!");
            return;
        }

        // Walidacja hasła
        if (savedPassword.isEmpty()) {
            showError("Wprowadź hasło!");
            return;
        }

        if (savedPassword.length() < 6) {
            showError("Hasło musi mieć co najmniej 6 znaków!");
            return;
        }

        System.out.println("Zapisany login: " + savedLoginText);
        System.out.println("Hasło: " + savedPassword);

        // Utwórz połączenie z serwerem
        clientConnection = new ClientConnection();
        boolean connected = clientConnection.connect();

        if (connected) {
            // Wysyłamy dane logowania
            String loginData = "LOGIN " + savedLoginText + ":" + savedPassword;
            clientConnection.sendMessage(loginData);

            // Tutaj możesz dodać oczekiwanie na odpowiedź od serwera
            // Na razie przechodzimy od razu do lobby
            switch_to_lobby(event);
        } else {
            showError("Nie udało się połączyć z serwerem");
        }
    }

    @FXML
    private void switch_to_main_menu(ActionEvent event) throws IOException {
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
    private void switch_to_lobby(ActionEvent event) throws IOException {
        Stage stage;
        Scene scene;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobby.fxml"));
        Parent root = loader.load();
        LobbyController lobbyController = loader.getController();

        // Przekaż połączenie i nick do LobbyController
        lobbyController.setupConnection(clientConnection, getSavedLoginText());

        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style_log_join.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    public String getSavedLoginText() {
        return savedLoginText;
    }

    public String getSavedPassword() {
        return savedPassword;
    }
}