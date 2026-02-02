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
    private static final Logger logger = Logger.getInstance();

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
        logger.info("Przełączam do głównego menu");
        try {
            root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();
            logger.info("Główne menu załadowane pomyślnie");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas przełączania do głównego menu");
            throw e;
        }
    }

    /**
     * Przełącza scenę na widok ustawień aplikacji.
     * Ładuje plik FXML {@code settings.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_settings(ActionEvent event) throws IOException {
        logger.info("Przechodzenie do ustawień");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/settings.fxml"));
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();
            logger.info("Ustawienia załadowane pomyślnie");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas przełączania do ustawień");
            throw e;
        }
    }

    /**
     * Przełącza scenę na widok logowania.
     * Ładuje plik FXML {@code login.fxml} i stosuje dedykowany arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_login(ActionEvent event) throws IOException {
        logger.info("Przechodzenie do logowania");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style_log_join.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();
            logger.info("Ekran logowania załadowany pomyślnie");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas przełączania do logowania");
            throw e;
        }
    }

    /**
     * Przełącza scenę na widok tabeli wyników.
     * Ładuje plik FXML {@code tab_wyn.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_tab_wynik(ActionEvent event) throws IOException {
        logger.info("Przechodzenie do tabeli wyników");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tab_wyn.fxml"));
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();
            logger.info("Tabela wyników załadowana pomyślnie");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas przełączania do tabeli wyników");
            throw e;
        }
    }

    /**
     * Przełącza scenę na widok informacji o autorach.
     * Ładuje plik FXML {@code autorzy.fxml} i stosuje domyślny arkusz stylów.
     *
     * @param event zdarzenie akcji wywołujące przełączenie sceny
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML
     */
    public void switch_to_autorzy(ActionEvent event) throws IOException {
        logger.info("Przechodzenie do autorów");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/autorzy.fxml"));
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();
            logger.info("Ekran autorów załadowany pomyślnie");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas przełączania do autorów");
            throw e;
        }
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
        logger.info("Przechodzenie do gry");

        try {
            // POPRAWKA: Użyj FXMLLoader z kontrolerem
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/uno_game.fxml"));
            Parent root = loader.load(); // To automatycznie wywołuje initialize() w kontrolerze
            logger.debug("Kontroler UnoController załadowany");

            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();

            logger.info("Gra załadowana pomyślnie!");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas przełączania do gry");
            throw e;
        }
    }

    /**
     * Zamyka aplikację, wywołując Platform.exit().
     *
     * @param event zdarzenie akcji wywołujące zamknięcie aplikacji
     * @throws IOException (nie jest faktycznie używane, ale pozostawione dla spójności sygnatur metod)
     */
    public void turn_off(ActionEvent event) throws IOException {
        logger.info("Zamykanie aplikacji...");
        try {
            Platform.exit();
            logger.info("Aplikacja zamknięta");
        } catch (Exception e) {
            logger.error(e, "Błąd podczas zamykania aplikacji");
            throw e;
        }
    }
}