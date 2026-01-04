package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class DbController {

    @FXML
    private TableView<PlayerScore> scoreTableView;

    @FXML
    private TableColumn<PlayerScore, String> playerColumn;

    @FXML
    private TableColumn<PlayerScore, Integer> winsColumn;

    private ObservableList<PlayerScore> scoreData = FXCollections.observableArrayList();
    private NetworkClient networkClient;

    @FXML
    public void initialize() {
        playerColumn.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
        scoreTableView.setItems(scoreData);

        // Połączenie z serwerem i pobranie danych
        connectAndLoadData();
    }

    private void connectAndLoadData() {
        networkClient = new NetworkClient();
        if (networkClient.connectToServer()) {
            String response = networkClient.sendMessage("TOP5");
            if (response != null && !response.isEmpty()) {
                processServerResponse(response);
            } else {
                loadSampleData();
            }
        } else {
            System.err.println("Nie można połączyć z serwerem. Ładuję przykładowe dane.");
            loadSampleData();
        }
    }

    private void processServerResponse(String response) {
        scoreData.clear();
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                String withoutPosition = line.replaceFirst("^\\d+\\.\\s*", "");

                String[] parts = withoutPosition.split(" - ");
                if (parts.length >= 2) {
                    String playerName = parts[0].trim();
                    String winsPart = parts[1].trim();
                    int wins = 0;
                    if (winsPart.contains("wygranych")) {
                        String winsStr = winsPart.split("\\s+")[0];
                        wins = Integer.parseInt(winsStr);
                    }
                    scoreData.add(new PlayerScore(playerName, wins));
                }
            } catch (Exception e) {
                System.err.println("Błąd przetwarzania linii: " + line);
                e.printStackTrace();
            }
        }

    }

    public void addPlayerScore(String playerName, int wins) {
        scoreData.add(new PlayerScore(playerName, wins));
    }
    private void loadSampleData() {
        addPlayerScore("Jan Kowalski", 15);
        addPlayerScore("Anna Nowak", 12);
        addPlayerScore("Piotr Wiśniewski", 8);
        addPlayerScore("Maria Zielińska", 20);
        addPlayerScore("Krzysztof Nowak", 10);
    }

    @FXML
    private void switch_to_main_menu(ActionEvent event) throws IOException {
        // Rozłącz się z serwerem przed zamknięciem
        if (networkClient != null) {
            networkClient.disconnect();
        }

        Stage stage;
        Scene scene;
        Parent root;
        System.out.println("switch_to_main_menu");
        root = FXMLLoader.load(getClass().getResource("/main_menu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    @FXML
    private void refreshData(ActionEvent event) {
        // Dodaj przycisk odświeżania w FXML jeśli chcesz
        scoreData.clear();
        if (networkClient != null) {
            networkClient.disconnect();
        }
        connectAndLoadData();
    }
    public static class PlayerScore {
        private String playerName;
        private Integer wins;

        public PlayerScore(String playerName, Integer wins) {
            this.playerName = playerName;
            this.wins = wins;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public Integer getWins() {
            return wins;
        }

        public void setWins(Integer wins) {
            this.wins = wins;
        }
    }
}