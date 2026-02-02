package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Kontroler obsługujący widok bazy danych/rankingu graczy.
 * Odpowiada za pobieranie danych z serwera, wyświetlanie ich w tabeli
 * oraz zarządzanie połączeniem sieciowym z serwerem rankingu.
 *
 */
public class DbController {
    private static final Logger logger = Logger.getInstance();

    @FXML
    private TableView<PlayerScore> scoreTableView;

    @FXML
    private TableColumn<PlayerScore, String> playerColumn;

    @FXML
    private TableColumn<PlayerScore, Integer> winsColumn;

    private ObservableList<PlayerScore> scoreData = FXCollections.observableArrayList();
    private ClientConnection clientConnection;

    /**
     * Inicjalizuje kontroler po załadowaniu widoku FXML.
     * Konfiguruje wiązania kolumn tabeli i rozpoczyna proces ładowania danych.
     * Automatycznie wywoływany przez JavaFX.
     */
    @FXML
    public void initialize() {
        logger.info("Inicjalizacja DbController");

        playerColumn.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
        scoreTableView.setItems(scoreData);

        connectAndLoadData();

        logger.info("DbController zainicjalizowany");
    }

    /**
     * Nawiązuje połączenie z serwerem i ładuje dane rankingu.
     * W przypadku błędu połączenia lub przetwarzania danych,
     * ładowane są przykładowe dane lokalne.
     */
    private void connectAndLoadData() {
        try {
            logger.info("Ładowanie danych rankingu...");
            clientConnection = new ClientConnection();

            // Próba połączenia z serwerem
            logger.info("Próba połączenia z serwerem...");
            if (clientConnection.connect()) {
                logger.info("Połączono z serwerem. Wysyłam TOP5...");

                // Wysyłamy żądanie TOP5
                if (clientConnection.sendMessage("TOP5")) {
                    logger.info("TOP5 wysłano. Oczekuję odpowiedzi...");

                    // Odbieramy odpowiedź z serwera
                    String response = clientConnection.receiveMessage();
                    logger.info("Otrzymana odpowiedź: " + response);

                    if (response != null && !response.isEmpty()) {
                        if (response.startsWith("TOP5")) {
                            processServerResponse(response);
                            logger.info("Dane z serwera załadowane pomyślnie.");
                        } else if (response.startsWith("ERROR")) {
                            logger.error("Serwer zwrócił błąd: " + response);
                            showAlert("Błąd serwera", "Serwer zwrócił błąd: " + response);
                            loadSampleData();
                        } else {
                            logger.warning("Nieznana odpowiedź serwera: " + response);
                            loadSampleData();
                        }
                    } else {
                        logger.error("Pusta odpowiedź serwera");
                        loadSampleData();
                    }
                } else {
                    logger.error("Nie udało się wysłać żądania TOP5");
                    showAlert("Błąd komunikacji", "Nie udało się wysłać żądania do serwera");
                    loadSampleData();
                }
            } else {
                logger.error("Nie udało się połączyć z serwerem");
                showAlert("Brak połączenia", "Nie można połączyć się z serwerem rankingu. Sprawdź, czy serwer jest uruchomiony.");
                loadSampleData();
            }
        } catch (Exception e) {
            logger.error(e, "Błąd podczas ładowania danych");
            loadSampleData();
        } finally {
            // Zawsze zamykamy połączenie
            if (clientConnection != null && clientConnection.isConnected()) {
                clientConnection.disconnect();
                logger.info("Połączenie z serwerem rankingu zamknięte");
            }
        }
    }

    /**
     * Przetwarza odpowiedź serwera zawierającą dane rankingu.
     * Parsuje odpowiedź w formacie tekstowym na obiekty PlayerScore.
     *
     * @param response Odpowiedź serwera w formacie: "TOP5 1. Jan Kowalski - 15 wygranych/2. ..."
     */
    private void processServerResponse(String response) {
        logger.debug("Przetwarzanie odpowiedzi serwera");
        scoreData.clear();

        // Usuwamy prefiks "TOP5 "
        if (response.startsWith("TOP5 ")) {
            response = response.substring(5).trim();
        }

        logger.debug("Przetwarzam odpowiedź: " + response);

        // Sprawdzamy czy odpowiedź zawiera błąd
        if (response.contains("Błąd") || response.contains("ERROR")) {
            logger.error("Błąd w odpowiedzi serwera: " + response);
            loadSampleData();
            return;
        }

        // Sprawdzamy czy są dane
        if (response.trim().isEmpty() || response.equals("Brak danych o graczach.")) {
            logger.info("Brak danych w rankingu");
            scoreData.clear();
            return;
        }

        // Dzielimy rekordy
        String[] records = response.split("/");
        logger.debug("Znaleziono " + records.length + " rekordów");

        for (String record : records) {
            record = record.trim();
            if (record.isEmpty()) continue;

            logger.debug("Przetwarzam rekord: '" + record + "'");

            try {
                // Format: "1. Jan Kowalski - 15 wygranych"
                // Usuwamy numerację
                String withoutPosition = record.replaceFirst("^\\d+\\.\\s*", "").trim();

                // Dzielimy na nazwę i liczbę wygranych
                String[] parts = withoutPosition.split(" - ");
                if (parts.length >= 2) {
                    String playerName = parts[0].trim();
                    String winsPart = parts[1].trim();

                    // Wyodrębniamy tylko liczbę (usuwamy "wygranych")
                    String winsStr = winsPart.replaceAll("[^0-9]", "").trim();

                    if (!winsStr.isEmpty()) {
                        int wins = Integer.parseInt(winsStr);
                        scoreData.add(new PlayerScore(playerName, wins));
                        logger.debug("Dodano: " + playerName + " - " + wins);
                    }
                }
            } catch (Exception e) {
                logger.error("Błąd przetwarzania rekordu: " + record);
                logger.error(e, "Szczegóły błędu");
            }
        }

        // Jeśli po przetworzeniu lista jest pusta, ładujemy dane przykładowe
        if (scoreData.isEmpty()) {
            logger.error("Nie udało się przetworzyć żadnych rekordów. Ładuję przykładowe dane.");
            loadSampleData();
        } else {
            logger.info("Załadowano " + scoreData.size() + " rekordów do rankingu");
        }
    }

    /**
     * Wyświetla okno dialogowe z ostrzeżeniem.
     *
     * @param title Tytuł okna dialogowego
     * @param content Treść komunikatu
     */
    private void showAlert(String title, String content) {
        logger.warning("Wyświetlam alert: " + title + " - " + content);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Dodaje nowy wynik gracza do listy danych.
     *
     * @param playerName Nazwa gracza
     * @param wins Liczba wygranych
     */
    public void addPlayerScore(String playerName, int wins) {
        scoreData.add(new PlayerScore(playerName, wins));
        logger.debug("Dodano wynik gracza: " + playerName + " - " + wins);
    }

    /**
     * Ładuje przykładowe dane do tabeli.
     * Używane w przypadku braku połączenia z serwerem lub błędów w danych.
     */
    private void loadSampleData() {
        logger.warning("Ładuję przykładowe dane...");
        scoreData.clear();
        addPlayerScore("Jan Kowalski", 15);
        addPlayerScore("Anna Nowak", 12);
        addPlayerScore("Piotr Wiśniewski", 8);
        addPlayerScore("Maria Zielińska", 20);
        addPlayerScore("Krzysztof Nowak", 10);
        logger.info("Przykładowe dane załadowane (5 rekordów)");
    }

    /**
     * Przełącza widok na główne menu aplikacji.
     *
     * @param event Zdarzenie akcji przycisku
     * @throws IOException W przypadku błędu ładowania pliku FXML
     */
    @FXML
    private void switch_to_main_menu(ActionEvent event) throws IOException {
        logger.info("Przełączam do głównego menu z rankingu");

        if (clientConnection != null && clientConnection.isConnected()) {
            clientConnection.disconnect();
            logger.debug("Połączenie z serwerem rankingu zamknięte");
        }

        Stage stage;
        Scene scene;
        Parent root;
        root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();

        logger.info("Przełączono do głównego menu");
    }

    /**
     * Odświeża dane w tabeli poprzez ponowne połączenie z serwerem.
     *
     * @param event Zdarzenie akcji przycisku
     */
    @FXML
    private void refreshData(ActionEvent event) {
        logger.info("Odświeżam dane...");
        connectAndLoadData();
        logger.info("Dane odświeżone");
    }

    /**
     * Klasa reprezentująca wynik gracza w rankingu.
     * Przechowuje nazwę gracza i liczbę wygranych.
     */
    public static class PlayerScore {
        private String playerName;
        private Integer wins;

        /**
         * Tworzy nowy obiekt wyniku gracza.
         *
         * @param playerName Nazwa gracza
         * @param wins Liczba wygranych
         */
        public PlayerScore(String playerName, Integer wins) {
            this.playerName = playerName;
            this.wins = wins;
        }

        /**
         * Zwraca nazwę gracza.
         *
         * @return Nazwa gracza
         */
        public String getPlayerName() {
            return playerName;
        }

        /**
         * Ustawia nazwę gracza.
         *
         * @param playerName Nowa nazwa gracza
         */
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        /**
         * Zwraca liczbę wygranych gracza.
         *
         * @return Liczba wygranych
         */
        public Integer getWins() {
            return wins;
        }

        /**
         * Ustawia liczbę wygranych gracza.
         *
         * @param wins Nowa liczba wygranych
         */
        public void setWins(Integer wins) {
            this.wins = wins;
        }
    }
}