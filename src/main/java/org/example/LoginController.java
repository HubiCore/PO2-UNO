package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML
    private TextField loginTextField;
    private String savedLoginText;
    @FXML
    private void handlePlayButton(ActionEvent event) throws IOException {
        savedLoginText = loginTextField.getText();
        System.out.println("Zapisany login: " + savedLoginText);
        switch_to_lobby(event);
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
        lobbyController.addUser(getSavedLoginText());
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }
    public String getSavedLoginText() {
        return savedLoginText;
    }
}