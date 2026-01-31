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
    private ClientConnection clientConnection;

    @FXML
    public void initialize() {
        playerColumn.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
        scoreTableView.setItems(scoreData);

        connectAndLoadData();
    }

    private void connectAndLoadData() {
        clientConnection = new ClientConnection();
        if (clientConnection.connect()) {
            try {
                clientConnection.sendMessage("TOP5");
                String response = clientConnection.receiveMessage();
                if (response != null && !response.isEmpty()) {
                    processServerResponse(response);
                } else {
                    loadSampleData();
                }
            } catch (IOException e) {
                System.err.println("Błąd podczas komunikacji z serwerem: " + e.getMessage());
                loadSampleData();
            }
        } else {
            System.err.println("Nie można połączyć z serwerem. Ładuję przykładowe dane.");
            loadSampleData();
        }
    }

    private void processServerResponse(String response) {
        scoreData.clear();

        if (response.startsWith("TOP5 ")) {
            response = response.substring(5).trim();
        }
        System.out.println("Server response: " + response);

        String[] records = response.split("/");

        for (String record : records) {
            record = record.trim();
            if (record.isEmpty()) continue;

            System.out.println("Processing record: " + record);

            try {
                String withoutPosition = record.replaceFirst("^\\d+\\.\\s*", "");

                String[] parts = withoutPosition.split(" - ");
                if (parts.length >= 2) {
                    String playerName = parts[0].trim();
                    String winsPart = parts[1].trim();
                    String winsStr = winsPart.split("\\s+")[0];
                    int wins = Integer.parseInt(winsStr);

                    scoreData.add(new PlayerScore(playerName, wins));
                }
            } catch (Exception e) {
                System.err.println("Błąd przetwarzania rekordu: " + record);
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
        if (clientConnection != null && clientConnection.isConnected()) {
            clientConnection.disconnect();
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
        scoreData.clear();
        if (clientConnection != null && clientConnection.isConnected()) {
            clientConnection.disconnect();
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