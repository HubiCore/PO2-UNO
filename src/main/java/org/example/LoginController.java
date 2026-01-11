package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.UnaryOperator;

public class LoginController {
    @FXML
    private TextField loginTextField;
    private String savedLoginText;
    private ClientConnection clientConnection;


    @FXML
    public void initialize() {
        //Max 20 znaków
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (change.isContentChange()) {
                int newLength = change.getControlNewText().length();
                if (newLength > 20) {
                    return null;
                }
            }
            return change;
        };
        loginTextField.setTextFormatter(new TextFormatter<>(filter));
    }
    @FXML
    private void handlePlayButton(ActionEvent event) throws IOException {
        savedLoginText = loginTextField.getText();
        if (savedLoginText == null || savedLoginText.trim().isEmpty()) {
            System.out.println("Wprowadź nick!");
            return;
        }

        System.out.println("Zapisany login: " + savedLoginText);

        // Utwórz połączenie z serwerem
        clientConnection = new ClientConnection();
        boolean connected = clientConnection.connect();

        if (connected) {
            switch_to_lobby(event);
        } else {
            System.out.println("Nie udało się połączyć z serwerem");
            // Możesz dodać komunikat dla użytkownika
        }
    }

    @FXML
    private void switch_to_main_menu(ActionEvent event) throws IOException {
        System.out.println(getSavedLoginText());
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
        System.out.println(getSavedLoginText());
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

    public String getSavedLoginText() {
        return savedLoginText;
    }
}