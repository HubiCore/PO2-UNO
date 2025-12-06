package org.example;
//Funkcje te niżej odpowiadają za zmianę scen. Łączymy z przeciskami w menu i jest ezzzz
import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static java.lang.System.exit;

public class SceneController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    //fullscreen nie działa
    public void switch_to_main_menu(ActionEvent event) throws IOException {
        System.out.println("switch_to_main_menu");
        root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setFullScreen(true);
        stage.setScene(scene);
        stage.show();
    }
    //fullscreen nie działa D:

    public void switch_to_settings(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do ustawien");
        Parent root = FXMLLoader.load(getClass().getResource("/settings.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setFullScreen(true); //będziesz dotykany :3
        stage.setScene(scene);
        stage.show();
    }
    public void turn_off(ActionEvent event) throws IOException {
        Platform.exit();
    }
}
