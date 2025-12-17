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



public class SceneController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    public void switch_to_main_menu(ActionEvent event) throws IOException {
        System.out.println("switch_to_main_menu");
        root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public void switch_to_settings(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do ustawien");
        Parent root = FXMLLoader.load(getClass().getResource("/settings.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public void switch_to_login(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do logowania");
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style_log_join.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public void switch_to_tab_wynik(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do tabeli wyników");
        Parent root = FXMLLoader.load(getClass().getResource("/tab_wyn.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public void switch_to_autorzy(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do autorów");
        Parent root = FXMLLoader.load(getClass().getResource("/autorzy.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public void switch_to_game(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do gry");

        // POPRAWKA: Użyj FXMLLoader z kontrolerem
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/uno_game.fxml"));
        Parent root = loader.load(); // To automatycznie wywołuje initialize() w kontrolerze

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        System.out.println("Gra załadowana!");
    }

    public void turn_off(ActionEvent event) throws IOException {
        Platform.exit();
    }
}