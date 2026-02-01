package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Główna klasa aplikacji JavaFX rozszerzająca klasę {@link javafx.application.Application}.
 * <p>
 * Klasa odpowiada za inicjalizację i uruchomienie interfejsu użytkownika,
 * wczytując plik FXML ({@code main_menu.fxml}) i konfigurując główne okno aplikacji.
 * Aplikacja jest uruchamiana w trybie pełnoekranowym z domyślną rozdzielczością 1920x1080 pikseli.
 * </p>
 * <p>
 * Przykład użycia:
 * </p>
 * <pre>
 * public static void main(String[] args) {
 *     Main.main(args);
 * }
 * </pre>
 */
public class Main extends Application {

    /**
     * Główna metoda startowa JavaFX, inicjująca interfejs użytkownika.
     * <p>
     * Ładuje plik FXML z zasobów, tworzy scenę o rozmiarze 1920x1080 pikseli,
     * dodaje arkusze stylów CSS, konfiguruje tytuł okna i ustawia tryb pełnoekranowy.
     * </p>
     *
     * @param primaryStage główne okno (scena) aplikacji JavaFX.
     * @throws Exception jeśli wystąpi błąd podczas ładowania pliku FXML, CSS
     *                   lub inny błąd związany z inicjalizacją interfejsu.
     * @see javafx.stage.Stage
     * @see javafx.scene.Scene
     * @see javafx.fxml.FXMLLoader
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
        Parent root = loader.load();
        SceneController controller = loader.getController();
        Scene scene = new Scene(root, 1920, 1080);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setTitle("FXML Example");
        primaryStage.setScene(scene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    /**
     * Główna metoda uruchomieniowa aplikacji.
     * <p>
     * Uruchamia aplikację JavaFX, wywołując metodę {@link Application#launch(String...)}.
     * </p>
     *
     * @param args argumenty wiersza poleceń przekazywane do aplikacji.
     * @see Application#launch(String...)
     */
    public static void main(String[] args) {
        launch(args);
    }
}