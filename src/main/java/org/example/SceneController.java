package org.example;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Klasa kontrolera scen zarządzająca przełączaniem między widokami aplikacji.
 * Zapewnia metody do zmiany aktualnie wyświetlanej sceny w głównym oknie aplikacji.
 * Wszystkie metody ustawiają tryb pełnoekranowy z ukrytą podpowiedzią wyjścia.
 */
public class SceneController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    /**
     * Przełącza scenę na główne menu aplikacji.
     * Ładuje plik FXML {@code main_menu.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_main_menu(ActionEvent event) throws IOException {
        System.out.println("switch_to_main_menu");
        root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    /**
     * Przełącza scenę na widok ustawień aplikacji.
     * Ładuje plik FXML {@code settings.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_settings(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do ustawien");
        Parent root = FXMLLoader.load(getClass().getResource("/settings.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    /**
     * Przełącza scenę na widok logowania.
     * Ładuje plik FXML {@code login.fxml} i stosuje dedykowany arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_login(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do logowania");
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style_log_join.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    /**
     * Przełącza scenę na widok tabeli wyników.
     * Ładuje plik FXML {@code tab_wyn.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_tab_wynik(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do tabeli wyników");
        Parent root = FXMLLoader.load(getClass().getResource("/tab_wyn.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    /**
     * Przełącza scenę na widok informacji o autorach.
     * Ładuje plik FXML {@code autorzy.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_autorzy(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do autorów");
        Parent root = FXMLLoader.load(getClass().getResource("/autorzy.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    /**
     * Przełącza scenę na widok głównej gry UNO.
     * Używa FXMLLoader z kontrolerem, co automatycznie wywołuje metodę initialize() w kontrolerze gry.
     * Ładuje plik FXML {@code uno_game.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_game(ActionEvent event) throws IOException {
        System.out.println("Przechodzenie do gry");

        // POPRAWKA: Użyj FXMLLoader z kontrolerem
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/uno_game.fxml"));
        Parent root = loader.load(); // To automatycznie wywołuje initialize() w kontrolerze

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();

        System.out.println("Gra załadowana!");
    }

    /**
     * Zamyka aplikację, wywołując Platform.exit().
     *
     * @param event zdarzenie akcji wywołujące zamknięcie aplikacji
     * @throws IOException (nie jest faktycznie używane, ale pozostawione dla spójności sygnatur metod)
     */
    public void turn_off(ActionEvent event) throws IOException {
        Platform.exit();
    }
}