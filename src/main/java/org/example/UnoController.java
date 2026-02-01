package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnoController implements Initializable {
    @FXML private StackPane stol;
    @FXML private HBox rekaGracza;
    @FXML private HBox rekaPrzeciwnika;
    @FXML private HBox rekaLewego;
    @FXML private HBox rekaPrawego;
    @FXML private Label instrukcja;
    @FXML private Label labelGracz;
    @FXML private Label labelPrzeciwnik;
    @FXML private Label labelLewy;
    @FXML private Label labelPrawy;
    @FXML private Label labelTura;
    @FXML private Button przyciskDobierania;
    private Card wierzchniaKarta;
    private List<Card> kartyGracza;
    private Map<String, Integer> przeciwnicyKarty;
    private ClientConnection clientConnection;
    private String nickname;
    private String currentPlayer;
    private boolean myTurn = false;
    private boolean waitingForColorChoice = false;
    private AtomicBoolean gameActive = new AtomicBoolean(true);
    private Thread messageReceiver;
    private volatile boolean uiReady = false;
    private Queue<String> pendingMessages = new ConcurrentLinkedQueue<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        przeciwnicyKarty = new HashMap<>();
        kartyGracza = new ArrayList<>();
        System.out.println("Kontroler zainicjalizowany");

        Platform.runLater(() -> {
            if (przyciskDobierania != null) {
                przyciskDobierania.setOnAction(e -> dobierzKarte());
                przyciskDobierania.setDisable(true);  // Początkowo wyłączony
                przyciskDobierania.setStyle("-fx-opacity: 0.5;");
            }
            uiReady = true;
            System.out.println("UI gotowe! Przetwarzam oczekujące wiadomości: " + pendingMessages.size());

            processPendingMessages();
        });
    }

    public void setupConnection(ClientConnection connection, String nickname) {
        this.clientConnection = connection;
        this.nickname = nickname;
        System.out.println("Ustawiono połączenie dla: " + nickname);

        startMessageReceiver();
        Platform.runLater(() -> {
            clientConnection.sendMessage("INIT_GAME ");
            System.out.println("Wysłano INIT_GAME");
        });
    }

    private void startMessageReceiver() {
        messageReceiver = new Thread(() -> {
            try {
                System.out.println("Rozpoczęto wątek odbierania wiadomości");
                while (gameActive.get() && clientConnection != null && clientConnection.isConnected()) {
                    try {
                        String message = clientConnection.receiveMessage();
                        if (message != null && !message.trim().isEmpty()) {
                            System.out.println("Odebrano w wątku sieciowym: [" + message + "]");

                            // Rozdziel po znakach nowej linii ORAZ po średnikach
                            String[] lines = message.split("\n");
                            for (String line : lines) {
                                if (!line.trim().isEmpty()) {
                                    // Teraz rozdziel po średnikach
                                    String[] parts = line.split(";");
                                    for (String part : parts) {
                                        String trimmedPart = part.trim();
                                        if (!trimmedPart.isEmpty()) {
                                            System.out.println("Dodaję do kolejki: " + trimmedPart);
                                            pendingMessages.offer(trimmedPart);
                                        }
                                    }
                                }
                            }

                            if (uiReady) {
                                Platform.runLater(() -> processPendingMessages());
                            }
                        }
                        // Krótka pauza, aby nie obciążać CPU
                        Thread.sleep(50);
                    } catch (Exception e) {
                        System.err.println("Błąd w odbiorze wiadomości: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd w wątku odbierania: " + e.getMessage());
                Platform.runLater(() -> showError("Utracono połączenie z serwerem"));
            }
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }
    private void removeCardFromHand(String cardStr) {
        Platform.runLater(() -> {
            for (int i = 0; i < kartyGracza.size(); i++) {
                Card card = kartyGracza.get(i);
                if (card.toString().equals(cardStr)) {
                    kartyGracza.remove(i);
                    if (i < rekaGracza.getChildren().size()) {
                        rekaGracza.getChildren().remove(i);
                    }
                    labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
                    break;
                }
            }
        });
    }
    private void processPendingMessages() {
        if (!uiReady) {
            System.out.println("UI niegotowe, pomijam przetwarzanie");
            return;
        }

        while (!pendingMessages.isEmpty()) {
            String message = pendingMessages.poll();
            System.out.println("Przetwarzam wiadomość z kolejki: " + message);
            handleServerMessage(message);
        }
    }

    private void handleServerMessage(String message) {
        System.out.println("=== ROZPOCZĘCIE handleServerMessage ===");
        System.out.println("Oryginalna wiadomość: [" + message + "]");

        String trimmed = message.trim();
        System.out.println("Przetwarzam komendę: [" + trimmed + "]");

        if (trimmed.startsWith("INIT_GAME ")) {
            handleGameInitialization(trimmed.substring(10));
        }
        else if (trimmed.startsWith("PLAY_RESULT ")) {
            System.out.println("Otrzymano wynik zagrania karty");
            handlePlayResult(trimmed.substring(12));
        }
        else if (trimmed.startsWith("HAND ")) {
            String handData = trimmed.substring(5);
            System.out.println("Dane ręki: " + handData);
            updateHand(handData);
        }
        else if (trimmed.startsWith("TOP_CARD ")) {
            System.out.println("Aktualizacja wierzchniej karty");
            updateTopCard(trimmed.substring(9));
        }
        else if (trimmed.startsWith("PLAYERS ")) {
            System.out.println("Aktualizacja graczy");
            updateOpponents(trimmed.substring(8));
        }
        else if (trimmed.startsWith("TURN ")) {
            System.out.println("Aktualizacja tury");
            updateTurn(trimmed.substring(5));
        }
        else if (trimmed.startsWith("PLAYED ")) {
            handleCardPlayed(trimmed.substring(7));
        }
        else if (trimmed.startsWith("DREW ")) {
            handleCardDrawn(trimmed.substring(5));
        }
        else if (trimmed.startsWith("WINNER ")) {
            handleWinner(trimmed.substring(7));
        }
        else if (trimmed.startsWith("CHOOSE_COLOR")) {
            promptColorChoice();
        }
        else if (trimmed.startsWith("WILD_COLOR ")) {
            updateWildColor(trimmed.substring(11));
        }
        else if (trimmed.startsWith("ERROR")) {
            showError(trimmed);
        }
        else if (trimmed.startsWith("GAME_ENDED")) {
            gameEnded();
        }
        else {
            System.out.println("Nieznana komenda: " + trimmed);
        }

        System.out.println("=== ZAKOŃCZENIE handleServerMessage ===\n");
    }
    private void handlePlayResult(String playResultData) {
        System.out.println("Przetwarzanie PLAY_RESULT: " + playResultData);

        String[] parts = playResultData.split(" ", 6);
        if (parts.length != 6) {
            System.err.println("Błędny format PLAY_RESULT: " + playResultData);
            return;
        }

        String playerWhoPlayed = parts[0];
        String cardPlayed = parts[1];
        String topCard = parts[2];
        String currentPlayer = parts[3];
        String opponents = parts[4];
        String hand = parts[5];  // To jest MOJA ręka, a nie ręka gracza który zagrał!

        Platform.runLater(() -> {
            // Aktualizuj wierzchnią kartę
            updateTopCard(topCard);

            // Aktualizuj turę
            updateTurn(currentPlayer);

            // Aktualizuj przeciwników
            updateOpponents(opponents);

            // ZAWSZE aktualizuj rękę (bo to MOJA ręka)
            updateHand(hand);

            // Wyświetl komunikat
            if (playerWhoPlayed.equals(nickname)) {
                instrukcja.setText("Twoja karta została zagrana");
            } else {
                instrukcja.setText("Gracz " + playerWhoPlayed + " zagrał kartę");
            }
        });
    }

    private void handleGameInitialization(String initData) {
        System.out.println("Inicjalizacja gry: " + initData);

        String[] parts = initData.split(" ", 4);
        if (parts.length != 4) {
            System.err.println("Błędny format INIT_GAME: " + initData);
            return;
        }

        String topCard = parts[0];
        String currentPlayer = parts[1];
        String opponents = parts[2];
        String hand = parts[3];

        updateTopCard(topCard);
        updateTurn(currentPlayer);
        updateOpponents(opponents);
        updateHand(hand);

    }

    private void updateHand(String handStr) {
        System.out.println("updateHand wywołane z: " + handStr);

        Platform.runLater(() -> {
            try {
                if (rekaGracza == null) {
                    System.err.println("ERROR: rekaGracza is null!");
                    return;
                }

                kartyGracza.clear();
                rekaGracza.getChildren().clear();

                String[] cards = handStr.split(",");
                System.out.println("Liczba kart: " + cards.length);

                for (String cardStr : cards) {
                    cardStr = cardStr.trim();
                    if (!cardStr.isEmpty()) {
                        try {
                            Card card = Card.fromString(cardStr);
                            kartyGracza.add(card);

                            StackPane kartaView = card.getView();
                            kartaView.setDisable(!myTurn || waitingForColorChoice);
                            kartaView.setStyle("-fx-cursor: " + (myTurn && !waitingForColorChoice ? "hand" : "default") + ";");

                            if (myTurn && !waitingForColorChoice) {
                                kartaView.setOnMouseClicked(e -> playCard(card));
                            } else {
                                kartaView.setOnMouseClicked(null);
                            }

                            rekaGracza.getChildren().add(kartaView);
                        } catch (Exception e) {
                            System.err.println("Błąd parsowania karty: " + cardStr);
                        }
                    }
                }

                labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
                System.out.println("Zaktualizowano rękę, liczba kart: " + kartyGracza.size());
            } catch (Exception e) {
                System.err.println("Błąd w updateHand: " + e.getMessage());
            }
        });
    }

    private void updateTopCard(String cardStr) {
        try {
            stol.getChildren().clear();
            String[] parts = cardStr.split(":");
            if (parts.length == 2) {
                wierzchniaKarta = new Card(parts[0], parts[1]);
                stol.getChildren().add(wierzchniaKarta.getView());
                System.out.println("Wierzchnia karta ustawiona: " + parts[0] + " " + parts[1]);
            }
        } catch (Exception e) {
            System.err.println("Błąd parsowania top card: " + cardStr);
        }
    }

    private void updateOpponents(String playersStr) {
        przeciwnicyKarty.clear();
        String[] players = playersStr.split(",");

        for (String player : players) {
            if (!player.isEmpty()) {
                String[] parts = player.split(":");
                if (parts.length == 2) {
                    przeciwnicyKarty.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        }

        updateOpponentDisplays();
    }

    private void updateOpponentDisplays() {
        Platform.runLater(() -> {
            List<String> opponents = new ArrayList<>(przeciwnicyKarty.keySet());

            if (opponents.size() >= 3) {
                labelPrzeciwnik.setText(opponents.get(0) + " (" + przeciwnicyKarty.get(opponents.get(0)) + ")");
                labelLewy.setText(opponents.get(1) + " (" + przeciwnicyKarty.get(opponents.get(1)) + ")");
                labelPrawy.setText(opponents.get(2) + " (" + przeciwnicyKarty.get(opponents.get(2)) + ")");

                updateHandDisplay(rekaPrzeciwnika, przeciwnicyKarty.get(opponents.get(0)));
                updateHandDisplay(rekaLewego, przeciwnicyKarty.get(opponents.get(1)));
                updateHandDisplay(rekaPrawego, przeciwnicyKarty.get(opponents.get(2)));
            } else if (opponents.size() == 2) {
                labelPrzeciwnik.setText(opponents.get(0) + " (" + przeciwnicyKarty.get(opponents.get(0)) + ")");
                labelLewy.setText(opponents.get(1) + " (" + przeciwnicyKarty.get(opponents.get(1)) + ")");
                labelPrawy.setText("");

                updateHandDisplay(rekaPrzeciwnika, przeciwnicyKarty.get(opponents.get(0)));
                updateHandDisplay(rekaLewego, przeciwnicyKarty.get(opponents.get(1)));
                rekaPrawego.getChildren().clear();
            } else if (opponents.size() == 1) {
                labelPrzeciwnik.setText(opponents.get(0) + " (" + przeciwnicyKarty.get(opponents.get(0)) + ")");
                labelLewy.setText("");
                labelPrawy.setText("");

                updateHandDisplay(rekaPrzeciwnika, przeciwnicyKarty.get(opponents.get(0)));
                rekaLewego.getChildren().clear();
                rekaPrawego.getChildren().clear();
            }
        });
    }
    private void updateHandDisplay(HBox handBox, int cardCount) {
        handBox.getChildren().clear();
        for (int i = 0; i < cardCount; i++) {
            Card dummyCard = new Card("RED", "0");
            handBox.getChildren().add(dummyCard.getBackView());
        }
    }
    @FXML
    private void dobierzKarte() {
        if (myTurn && !waitingForColorChoice && clientConnection != null && clientConnection.isConnected()) {
            System.out.println("Dobieranie karty...");

            // Wyłącz przycisk na chwilę, aby uniknąć wielokrotnych kliknięć
            if (przyciskDobierania != null) {
                przyciskDobierania.setDisable(true);
            }

            // Wyślij komendę do serwera
            clientConnection.sendMessage("DRAW");
            instrukcja.setText("Dobieranie karty...");

            // Po krótkim czasie przywróć stan przycisku (jeśli nadal jest tura)
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                if (myTurn && przyciskDobierania != null) {
                                    przyciskDobierania.setDisable(false);
                                }
                            });
                        }
                    },
                    1000  // 1 sekunda
            );
        }
    }
    private void updateTurn(String player) {
        System.out.println("updateTurn wywołane dla gracza: " + player + " (ja: " + nickname + ")");

        Platform.runLater(() -> {
            currentPlayer = player;
            myTurn = player.equals(nickname);
            System.out.println("Czy moja tura? " + myTurn);

            if (myTurn) {
                labelTura.setText("Twoja tura!");
                instrukcja.setText("Wybierz kartę do zagrania lub kliknij talię, aby dobrać kartę");

                if (przyciskDobierania != null) {
                    przyciskDobierania.setDisable(false);
                    przyciskDobierania.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");
                }
                // Odblokuj karty
                for (var child : rekaGracza.getChildren()) {
                    child.setDisable(false);
                    child.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");
                }
            } else {
                labelTura.setText("Tura gracza: " + player);
                instrukcja.setText("Oczekiwanie na ruch gracza " + player);

                // Zablokuj karty
                for (var child : rekaGracza.getChildren()) {
                    child.setDisable(true);
                    child.setStyle("-fx-opacity: 0.7; -fx-cursor: default;");
                }
            }
        });
    }

    private void handleCardPlayed(String playInfo) {
        System.out.println("Otrzymano PLAYED: " + playInfo);

        // Usuń ewentualne dodatkowe białe znaki
        playInfo = playInfo.trim();

        String[] parts = playInfo.split(" ");
        if (parts.length >= 2) {
            String player = parts[0];
            String cardStr = parts[1];

            if (player.equals(nickname)) {
                // Usuń kartę z ręki klienta
                Platform.runLater(() -> {
                    for (int i = kartyGracza.size() - 1; i >= 0; i--) {
                        Card c = kartyGracza.get(i);
                        if (c.toString().equals(cardStr)) {
                            kartyGracza.remove(i);
                            if (i < rekaGracza.getChildren().size()) {
                                rekaGracza.getChildren().remove(i);
                            }
                            labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
                            break;
                        }
                    }
                });
            }

            // Aktualizuj komunikat
            Platform.runLater(() -> {
                if (!player.equals(nickname)) {
                    instrukcja.setText("Gracz " + player + " zagrał kartę");
                } else {
                    instrukcja.setText("Twoja karta została zagrana");
                }
            });
        }
    }

    private void handleCardDrawn(String cardStr) {
        Platform.runLater(() -> {
            instrukcja.setText("Dobrałeś kartę: " + cardStr);

            try {
                // Dodaj nową kartę do ręki gracza
                Card newCard = Card.fromString(cardStr);
                kartyGracza.add(newCard);

                // Dodaj widok karty do ręki
                StackPane kartaView = newCard.getView();
                kartaView.setDisable(!myTurn || waitingForColorChoice);
                kartaView.setStyle("-fx-cursor: " + (myTurn && !waitingForColorChoice ? "hand" : "default") + ";");

                if (myTurn && !waitingForColorChoice) {
                    kartaView.setOnMouseClicked(e -> playCard(newCard));
                }

                rekaGracza.getChildren().add(kartaView);
                labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");

                // Aktualizuj widok przeciwników (ich liczba kart się nie zmieniła, ale może być potrzebne odświeżenie)
                updateOpponentDisplays();

            } catch (Exception e) {
                System.err.println("Błąd podczas dodawania dobranej karty: " + e.getMessage());
            }
        });
    }

    private void handleWinner(String winner) {
        if (winner.equals(nickname)) {
            instrukcja.setText("WYGRAŁEŚ!");
            instrukcja.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
        } else {
            instrukcja.setText("Wygrał gracz " + winner);
            instrukcja.setStyle("-fx-text-fill: silver; -fx-font-size: 36px; -fx-font-weight: bold;");
        }

        zablokujKarty();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Koniec gry!");
            alert.setHeaderText(null);
            alert.setContentText(winner.equals(nickname) ?
                    "Gratulacje! Wygrałeś grę!" :
                    "Wygrał gracz " + winner);
            alert.showAndWait();
        });
    }

    private void gameEnded() {
        gameActive.set(false);
        instrukcja.setText("Gra zakończona");
    }

    private void promptColorChoice() {
        waitingForColorChoice = true;
        instrukcja.setText("Wybierz kolor: [R]ed, [G]reen, [B]lue, [Y]ellow");

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Wybierz kolor");
            alert.setHeaderText("Zagrałeś kartę WILD");
            alert.setContentText("Wybierz kolor:");

            ButtonType redButton = new ButtonType("Czerwony");
            ButtonType greenButton = new ButtonType("Zielony");
            ButtonType blueButton = new ButtonType("Niebieski");
            ButtonType yellowButton = new ButtonType("Żółty");

            alert.getButtonTypes().setAll(redButton, greenButton, blueButton, yellowButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                String color = "";
                if (result.get() == redButton) color = "RED";
                else if (result.get() == greenButton) color = "GREEN";
                else if (result.get() == blueButton) color = "BLUE";
                else if (result.get() == yellowButton) color = "YELLOW";

                if (!color.isEmpty()) {
                    clientConnection.sendMessage("WILD_COLOR " + color);
                    waitingForColorChoice = false;
                }
            }
        });
    }

    private void updateWildColor(String color) {
        instrukcja.setText("Kolor zmieniony na: " + color);
    }

    @FXML
    private void handleDrawCard() {
        if (myTurn && !waitingForColorChoice) {
            clientConnection.sendMessage("DRAW");
        }
    }

    private void playCard(Card card) {
        if (myTurn && !waitingForColorChoice) {
            String cardStr = card.getColor() + ":" + card.getValue();
            clientConnection.sendMessage("PLAY " + cardStr);
            System.out.println("Wysłano kartę do serwera: " + cardStr);

            // Tymczasowe usunięcie karty z ręki (do czasu otrzymania potwierdzenia od serwera)
            removeCardFromHand(cardStr);
            instrukcja.setText("Wysyłanie karty...");
        }
    }
    //Debugging
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void zablokujKarty() {
        for (var child : rekaGracza.getChildren()) {
            child.setDisable(true);
            child.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
        }
    }

    @FXML
    private void handleQuit() {
        gameActive.set(false);
        if (messageReceiver != null) {
            messageReceiver.interrupt();
        }
        if (clientConnection != null) {
            clientConnection.sendMessage("EXIT " + nickname);
            clientConnection.disconnect();
        }
    }
}