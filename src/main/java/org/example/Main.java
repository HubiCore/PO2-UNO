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
    private static final Logger logger = Logger.getInstance();

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
        logger.info("=== URUCHAMIANIE APLIKACJI UNO ===");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
            logger.debug("Ładowanie pliku FXML: /main_menu.fxml");

            Parent root = loader.load();
            SceneController controller = loader.getController();
            logger.debug("Kontroler SceneController załadowany");

            Scene scene = new Scene(root, 1920, 1080);
            logger.debug("Scena utworzona (1920x1080)");

            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            logger.debug("Style CSS załadowane");

            primaryStage.setTitle("FXML Example");
            primaryStage.setScene(scene);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setFullScreen(true);
            primaryStage.show();

            logger.info("Aplikacja uruchomiona pomyślnie");
            logger.info("Tryb pełnoekranowy włączony");

        } catch (Exception e) {
            logger.error(e, "Błąd podczas uruchamiania aplikacji");
            throw e;
        }
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
        logger.info("=== START APLIKACJI UNO CLIENT ===");
        logger.info("Wersja: 1.0");
        logger.info("Data uruchomienia: " + new java.util.Date());

        try {
            logger.info("Uruchamianie JavaFX Application...");
            launch(args);
        } catch (Exception e) {
            logger.error(e, "Krytyczny błąd podczas uruchamiania aplikacji");
            System.err.println("Krytyczny błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Metoda wywoływana przy zamykaniu aplikacji.
     * Zamyka logger przed wyjściem.
     */
    @Override
    public void stop() throws Exception {
        logger.info("=== ZAMYKANIE APLIKACJI ===");
        logger.info("Zamykanie zasobów...");

        // Zamknij logger
        Logger.getInstance().shutdown();

        super.stop();
        logger.info("Aplikacja zamknięta");
    }
}