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
        playerColumn.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
        scoreTableView.setItems(scoreData);

        connectAndLoadData();
    }

    /**
     * Nawiązuje połączenie z serwerem i ładuje dane rankingu.
     * W przypadku błędu połączenia lub przetwarzania danych,
     * ładowane są przykładowe dane lokalne.
     */
    private void connectAndLoadData() {
        try {
            clientConnection = new ClientConnection();

            // Próba połączenia z serwerem
            System.out.println("Próba połączenia z serwerem...");
            if (clientConnection.connect()) {
                System.out.println("Połączono z serwerem. Wysyłam TOP5...");

                // Wysyłamy żądanie TOP5
                if (clientConnection.sendMessage("TOP5")) {
                    System.out.println("TOP5 wysłano. Oczekuję odpowiedzi...");

                    // Odbieramy odpowiedź z serwera
                    String response = clientConnection.receiveMessage();
                    System.out.println("Otrzymana odpowiedź: " + response);

                    if (response != null && !response.isEmpty()) {
                        if (response.startsWith("TOP5")) {
                            processServerResponse(response);
                            System.out.println("Dane z serwera załadowane pomyślnie.");
                        } else if (response.startsWith("ERROR")) {
                            System.err.println("Serwer zwrócił błąd: " + response);
                            showAlert("Błąd serwera", "Serwer zwrócił błąd: " + response);
                            loadSampleData();
                        } else {
                            System.err.println("Nieznana odpowiedź serwera: " + response);
                            loadSampleData();
                        }
                    } else {
                        System.err.println("Pusta odpowiedź serwera");
                        loadSampleData();
                    }
                } else {
                    System.err.println("Nie udało się wysłać żądania TOP5");
                    showAlert("Błąd komunikacji", "Nie udało się wysłać żądania do serwera");
                    loadSampleData();
                }
            } else {
                System.err.println("Nie udało się połączyć z serwerem");
                showAlert("Brak połączenia", "Nie można połączyć się z serwerem rankingu. Sprawdź, czy serwer jest uruchomiony.");
                loadSampleData();
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas ładowania danych: " + e.getMessage());
            e.printStackTrace();
            loadSampleData();
        } finally {
            // Zawsze zamykamy połączenie
            if (clientConnection != null && clientConnection.isConnected()) {
                clientConnection.disconnect();
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
        scoreData.clear();

        // Usuwamy prefiks "TOP5 "
        if (response.startsWith("TOP5 ")) {
            response = response.substring(5).trim();
        }

        System.out.println("Przetwarzam odpowiedź: " + response);

        // Sprawdzamy czy odpowiedź zawiera błąd
        if (response.contains("Błąd") || response.contains("ERROR")) {
            System.err.println("Błąd w odpowiedzi serwera: " + response);
            loadSampleData();
            return;
        }

        // Sprawdzamy czy są dane
        if (response.trim().isEmpty() || response.equals("Brak danych o graczach.")) {
            System.out.println("Brak danych w rankingu");
            scoreData.clear();
            return;
        }

        // Dzielimy rekordy
        String[] records = response.split("/");
        System.out.println("Znaleziono " + records.length + " rekordów");

        for (String record : records) {
            record = record.trim();
            if (record.isEmpty()) continue;

            System.out.println("Przetwarzam rekord: '" + record + "'");

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
                        System.out.println("Dodano: " + playerName + " - " + wins);
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd przetwarzania rekordu: " + record);
                System.err.println("Błąd: " + e.getMessage());
            }
        }

        // Jeśli po przetworzeniu lista jest pusta, ładujemy dane przykładowe
        if (scoreData.isEmpty()) {
            System.err.println("Nie udało się przetworzyć żadnych rekordów. Ładuję przykładowe dane.");
            loadSampleData();
        }
    }

    /**
     * Wyświetla okno dialogowe z ostrzeżeniem.
     *
     * @param title Tytuł okna dialogowego
     * @param content Treść komunikatu
     */
    private void showAlert(String title, String content) {
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
    }

    /**
     * Ładuje przykładowe dane do tabeli.
     * Używane w przypadku braku połączenia z serwerem lub błędów w danych.
     */
    private void loadSampleData() {
        System.out.println("Ładuję przykładowe dane...");
        scoreData.clear();
        addPlayerScore("Jan Kowalski", 15);
        addPlayerScore("Anna Nowak", 12);
        addPlayerScore("Piotr Wiśniewski", 8);
        addPlayerScore("Maria Zielińska", 20);
        addPlayerScore("Krzysztof Nowak", 10);
        System.out.println("Przykładowe dane załadowane");
    }

    /**
     * Przełącza widok na główne menu aplikacji.
     *
     * @param event Zdarzenie akcji przycisku
     * @throws IOException W przypadku błędu ładowania pliku FXML
     */
    @FXML
    private void switch_to_main_menu(ActionEvent event) throws IOException {
        if (clientConnection != null && clientConnection.isConnected()) {
            clientConnection.disconnect();
        }

        Stage stage;
        Scene scene;
        Parent root;
        System.out.println("Przełączam do głównego menu");
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
     * Odświeża dane w tabeli poprzez ponowne połączenie z serwerem.
     *
     * @param event Zdarzenie akcji przycisku
     */
    @FXML
    private void refreshData(ActionEvent event) {
        System.out.println("Odświeżam dane...");
        connectAndLoadData();
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