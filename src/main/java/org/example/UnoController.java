/**
 * Główny kontroler gry UNO obsługujący interfejs użytkownika i komunikację z serwerem.
 * Klasa zarządza logiką klienta gry UNO, w tym wyświetlaniem kart, obsługą tur graczy,
 * przetwarzaniem komunikatów serwera i interakcją użytkownika poprzez interfejs JavaFX.
 *
 * <p>Kontroler implementuje interfejs {@link Initializable}, co umożliwia inicjalizację
 * komponentów JavaFX po załadowaniu pliku FXML.</p>
 *
 * @see javafx.fxml.Initializable
 * @see ClientConnection
 * @see Card
 */
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UnoController implements Initializable {

    /** Kontener dla wierzchniej karty na stole. */
    @FXML private StackPane stol;

    /** Kontener wyświetlający karty w ręce głównego gracza. */
    @FXML private HBox rekaGracza;

    /** Kontener wyświetlający karty przeciwnika (górny gracz). */
    @FXML private HBox rekaPrzeciwnika;

    /** Kontener wyświetlający karty lewego przeciwnika. */
    @FXML private HBox rekaLewego;

    /** Kontener wyświetlający karty prawego przeciwnika. */
    @FXML private HBox rekaPrawego;

    /** Etykieta wyświetlająca instrukcje dla gracza. */
    @FXML private Label instrukcja;

    /** Etykieta z nazwą i liczbą kart głównego gracza. */
    @FXML private Label labelGracz;

    /** Etykieta z nazwą i liczbą kart przeciwnika (górny gracz). */
    @FXML private Label labelPrzeciwnik;

    /** Etykieta z nazwą i liczbą kart lewego przeciwnika. */
    @FXML private Label labelLewy;

    /** Etykieta z nazwą i liczbą kart prawego przeciwnika. */
    @FXML private Label labelPrawy;

    /** Etykieta informująca o aktualnej turze. */
    @FXML private Label labelTura;

    /** Przycisk umożliwiający dobieranie karty. */
    @FXML private Button przyciskDobierania;

    /** Aktualna wierzchnia karta na stole. */
    private Card wierzchniaKarta;

    /** Lista kart w ręce głównego gracza. */
    private List<Card> kartyGracza;

    /** Mapa przechowująca liczbę kart każdego przeciwnika (klucz: nazwa gracza, wartość: liczba kart). */
    private Map<String, Integer> przeciwnicyKarty;

    /** Połączenie klienta z serwerem. */
    private ClientConnection clientConnection;

    /** Nickname głównego gracza. */
    private String nickname;

    /** Nazwa gracza aktualnie wykonującego turę. */
    private String currentPlayer;

    /** Flaga wskazująca, czy tura należy do głównego gracza. */
    private boolean myTurn = false;

    /** Flaga wskazująca, czy oczekiwany jest wybór koloru po zagraniu karty WILD. */
    private boolean waitingForColorChoice = false;

    /** AtomicBoolean zarządzający stanem aktywności gry. */
    private AtomicBoolean gameActive = new AtomicBoolean(true);

    /** Wątek odbierający wiadomości z serwera. */
    private Thread messageReceiver;

    /** Flaga wskazująca, czy interfejs użytkownika jest gotowy do aktualizacji. */
    private volatile boolean uiReady = false;

    /** Kolejka wiadomości oczekujących na przetworzenie po gotowości UI. */
    private Queue<String> pendingMessages = new ConcurrentLinkedQueue<>();

    /**
     * Inicjalizuje kontroler po załadowaniu pliku FXML.
     * Metoda wywoływana automatycznie przez JavaFX.
     *
     * @param location lokalizacja używana do rozwiązywania ścieżek względnych dla obiektu root
     * @param resources zasoby używane do lokalizacji obiektu root
     */
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

    /**
     * Konfiguruje połączenie z serwerem i ustawia nickname gracza.
     *
     * @param connection obiekt ClientConnection do komunikacji z serwerem
     * @param nickname nazwa gracza
     */
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

    /**
     * Uruchamia wątek odbierający wiadomości z serwera.
     * Wiadomości są dodawane do kolejki pendingMessages i przetwarzane po gotowości UI.
     */
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

    /**
     * Usuwa kartę z ręki gracza na podstawie jej reprezentacji tekstowej.
     *
     * @param cardStr reprezentacja tekstowa karty do usunięcia
     */
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

    /**
     * Przetwarza wiadomości oczekujące w kolejce po gotowości UI.
     */
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

    /**
     * Główna metoda obsługi komunikatów serwera.
     * Rozpoznaje typ komunikatu i wywołuje odpowiednią metodę obsługi.
     *
     * @param message pełny komunikat otrzymany z serwera
     */
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
        else if (trimmed.startsWith("WINNER ")) {
            System.out.println("Otrzymano informację o zwycięzcy");
            handleWinner(trimmed.substring(7));
        }
        else if (trimmed.startsWith("GAME_ENDED")) {
            System.out.println("Gra zakończona przez serwer");
            gameEnded();
        }
        else {
            System.out.println("Nieznana komenda: " + trimmed);
        }

        System.out.println("=== ZAKOŃCZENIE handleServerMessage ===\n");
    }

    /**
     * Obsługuje wynik zagrania karty otrzymany z serwera.
     * Aktualizuje wierzchnią kartę, turę, stan przeciwników i rękę gracza.
     *
     * @param playResultData dane wyniku zagrania w formacie:
     *                      "gracz karta wierzchnia_karta aktualny_gracz przeciwnicy ręka"
     */
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

    /**
     * Obsługuje inicjalizację gry na podstawie danych otrzymanych z serwera.
     *
     * @param initData dane inicjalizacyjne w formacie:
     *                "wierzchnia_karta aktualny_gracz przeciwnicy ręka"
     */
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

    /**
     * Aktualizuje wyświetlanie kart w ręce głównego gracza.
     *
     * @param handStr ciąg znaków reprezentujący karty w ręce,
     *               oddzielone przecinkami (np. "RED:5,BLUE:SKIP")
     */
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

    /**
     * Aktualizuje wierzchnią kartę na stole.
     *
     * @param cardStr reprezentacja karty w formacie "kolor:wartość"
     */
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

    /**
     * Aktualizuje informacje o przeciwnikach na podstawie danych z serwera.
     *
     * @param playersStr ciąg z informacjami o graczach w formacie:
     *                  "gracz1:liczba_kart,gracz2:liczba_kart,..."
     */
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

    /**
     * Aktualizuje wyświetlanie informacji o przeciwnikach w interfejsie.
     * Rozmieszcza etykiety i karty przeciwników w odpowiednich kontenerach.
     */
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

    /**
     * Aktualizuje wyświetlanie kart przeciwnika (tylko rewersy kart).
     *
     * @param handBox kontener HBox dla kart przeciwnika
     * @param cardCount liczba kart do wyświetlenia
     */
    private void updateHandDisplay(HBox handBox, int cardCount) {
        handBox.getChildren().clear();
        for (int i = 0; i < cardCount; i++) {
            Card dummyCard = new Card("RED", "0");
            handBox.getChildren().add(dummyCard.getBackView());
        }
    }

    /**
     * Obsługuje akcję dobierania karty przez gracza.
     * Wysyła żądanie do serwera i aktualizuje interfejs.
     */
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

    /**
     * Aktualizuje informację o aktualnej turze.
     * Włącza/wyłącza interaktywność kart i przycisków w zależności od tego, czy to tura gracza.
     *
     * @param player nazwa gracza, który ma aktualną turę
     */
    private void updateTurn(String player) {
        System.out.println("=== updateTurn ===");
        System.out.println("Nowy gracz: " + player);
        System.out.println("Ja: " + nickname);
        System.out.println("Czy moja tura? " + player.equals(nickname));

        Platform.runLater(() -> {
            currentPlayer = player;
            myTurn = player.equals(nickname);

            System.out.println("myTurn ustawione na: " + myTurn);

            if (myTurn) {
                labelTura.setText("Twoja tura!");
                instrukcja.setText("Wybierz kartę do zagrania lub kliknij talię, aby dobrać kartę");

                // Włącz przycisk dobierania
                if (przyciskDobierania != null) {
                    przyciskDobierania.setDisable(false);
                    przyciskDobierania.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");
                    System.out.println("Przycisk dobierania włączony");
                }

                // Odblokuj wszystkie karty w ręce
                for (var child : rekaGracza.getChildren()) {
                    child.setDisable(false);
                    child.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");

                    // Znajdź odpowiadającą kartę i ustaw handler
                    int index = rekaGracza.getChildren().indexOf(child);
                    if (index >= 0 && index < kartyGracza.size()) {
                        Card card = kartyGracza.get(index);
                        child.setOnMouseClicked(e -> playCard(card));
                    }
                }
                System.out.println("Karty odblokowane");

            } else {
                labelTura.setText("Tura gracza: " + player);
                instrukcja.setText("Oczekiwanie na ruch gracza " + player);

                // Wyłącz przycisk dobierania
                if (przyciskDobierania != null) {
                    przyciskDobierania.setDisable(true);
                    przyciskDobierania.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
                    System.out.println("Przycisk dobierania wyłączony");
                }

                // Zablokuj wszystkie karty w ręce
                for (var child : rekaGracza.getChildren()) {
                    child.setDisable(true);
                    child.setStyle("-fx-opacity: 0.7; -fx-cursor: default;");
                    child.setOnMouseClicked(null);
                }
                System.out.println("Karty zablokowane");
            }

            System.out.println("=== koniec updateTurn ===\n");
        });
    }

    /**
     * Obsługuje informację o zagranej karcie przez innego gracza.
     *
     * @param playInfo informacja o zagranej karcie w formacie "gracz karta"
     */
    private void handleCardPlayed(String playInfo) {
        System.out.println("Otrzymano PLAYED: " + playInfo);

        // Usuń ewentualne dodatkowe białe znaki
        playInfo = playInfo.trim();

        String[] parts = playInfo.split(" ");
        if (parts.length >= 2) {
            String player = parts[0];
            String cardStr = parts[1];

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

    /**
     * Obsługuje informację o dobranej karcie przez gracza.
     * Dodaje nową kartę do ręki gracza.
     *
     * @param cardStr reprezentacja dobranej karty
     */
    private void handleCardDrawn(String cardStr) {
        System.out.println("handleCardDrawn: Otrzymano DREW - " + cardStr);

        Platform.runLater(() -> {
            instrukcja.setText("Dobrałeś kartę: " + cardStr);

            try {
                // Dodaj nową kartę do ręki gracza
                Card newCard = Card.fromString(cardStr);
                kartyGracza.add(newCard);

                // Dodaj widok karty do ręki
                StackPane kartaView = newCard.getView();

                // Początkowo zablokuj kartę (tura się zmieni)
                kartaView.setDisable(true);
                kartaView.setStyle("-fx-opacity: 0.7; -fx-cursor: default;");
                kartaView.setOnMouseClicked(null);

                rekaGracza.getChildren().add(kartaView);
                labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");

                System.out.println("handleCardDrawn: Karta dodana do ręki, oczekiwanie na TURN...");

            } catch (Exception e) {
                System.err.println("Błąd podczas dodawania dobranej karty: " + e.getMessage());
            }
        });
    }

    /**
     * Obsługuje informację o zwycięzcy gry.
     * Wyświetla odpowiedni komunikat i po 3 sekundach wraca do menu głównego.
     *
     * @param winner nazwa zwycięzcy
     */
    private void handleWinner(String winner) {
        Platform.runLater(() -> {
            // Wyświetl komunikat o zwycięzcy
            if (winner.equals(nickname)) {
                instrukcja.setText("WYGRAŁEŚ!");
                instrukcja.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
            } else {
                instrukcja.setText("Wygrał gracz " + winner);
                instrukcja.setStyle("-fx-text-fill: silver; -fx-font-size: 36px; -fx-font-weight: bold;");
            }

            zablokujKarty();

            // Wyłącz przycisk dobierania
            if (przyciskDobierania != null) {
                przyciskDobierania.setDisable(true);
                przyciskDobierania.setStyle("-fx-opacity: 0.5;");
            }

            // Pokaż alert z informacją o wygranej
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Koniec gry!");
            alert.setHeaderText(null);
            if (winner.equals(nickname)) {
                alert.setContentText("Gratulacje! Wygrałeś grę!\nPowrót do menu głównego za 3 sekundy...");
            } else {
                alert.setContentText("Wygrał gracz " + winner + "!\nPowrót do menu głównego za 3 sekundy...");
            }

            alert.show();

            // Automatyczne przejście do menu głównego po 3 sekundach
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                try {
                                    // Zamknij alert jeśli jeszcze jest otwarty
                                    alert.close();

                                    // Załaduj menu główne z zasobów
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
                                    Parent root = loader.load();

                                    // Pobierz aktualne okno
                                    Stage stage = (Stage) instrukcja.getScene().getWindow();

                                    // Ustaw nową scenę
                                    Scene scene = new Scene(root);
                                    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                                    stage.setScene(scene);
                                    stage.setFullScreen(true);
                                    stage.setFullScreenExitHint("");
                                    stage.show();

                                    // Zamknij połączenie z serwerem
                                    if (clientConnection != null) {
                                        clientConnection.disconnect();
                                    }

                                } catch (Exception e) {
                                    System.err.println("Błąd podczas przełączania do menu: " + e.getMessage());
                                    e.printStackTrace();
                                    // W razie błędu spróbuj bezpośrednio zamknąć okno
                                    Platform.exit();
                                }
                            });
                        }
                    },
                    3000  // 3 sekundy opóźnienia
            );
        });
    }

    /**
     * Obsługuje zakończenie gry przez serwer.
     * Wyświetla komunikat i wraca do menu głównego.
     */
    private void gameEnded() {
        gameActive.set(false);

        Platform.runLater(() -> {
            instrukcja.setText("Gra zakończona");
            instrukcja.setStyle("-fx-text-fill: red; -fx-font-size: 36px; -fx-font-weight: bold;");

            // Pokaż alert informacyjny
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gra zakończona");
            alert.setHeaderText(null);
            alert.setContentText("Gra została zakończona przez serwer.\nPowrót do menu głównego za 3 sekundy...");
            alert.show();

            // Automatyczne przejście do menu głównego
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                try {
                                    alert.close();
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/MainMenu.fxml"));
                                    Parent root = loader.load();

                                    Stage stage = (Stage) instrukcja.getScene().getWindow();
                                    Scene scene = new Scene(root);
                                    stage.setScene(scene);
                                    stage.setTitle("UNO - Menu Główne");
                                    stage.show();

                                    if (clientConnection != null) {
                                        clientConnection.disconnect();
                                    }

                                } catch (Exception e) {
                                    System.err.println("Błąd podczas przełączania do menu: " + e.getMessage());
                                }
                            });
                        }
                    },
                    3000
            );
        });
    }

    /**
     * Wyświetla okno dialogowe z wyborem koloru po zagraniu karty WILD.
     */
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

    /**
     * Aktualizuje informację o wybranym kolorze po zagraniu karty WILD.
     *
     * @param color wybrany kolor
     */
    private void updateWildColor(String color) {
        instrukcja.setText("Kolor zmieniony na: " + color);
    }

    /**
     * Obsługuje żądanie dobrania karty (alternatywna metoda dla przycisku).
     */
    @FXML
    private void handleDrawCard() {
        if (myTurn && !waitingForColorChoice) {
            clientConnection.sendMessage("DRAW");
        }
    }

    /**
     * Wysyła do serwera informację o zagraniu karty.
     *
     * @param card karta do zagrania
     */
    private void playCard(Card card) {
        if (myTurn && !waitingForColorChoice) {
            String cardStr = card.getColor() + ":" + card.getValue();
            clientConnection.sendMessage("PLAY " + cardStr);
            System.out.println("Wysłano kartę do serwera: " + cardStr);

            instrukcja.setText("Wysyłanie karty...");
        }
    }

    /**
     * Wyświetla okno dialogowe z błędem.
     *
     * @param message komunikat błędu do wyświetlenia
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Blokuje wszystkie karty w ręce gracza (np. po zakończeniu gry).
     */
    private void zablokujKarty() {
        for (var child : rekaGracza.getChildren()) {
            child.setDisable(true);
            child.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
        }
    }

    /**
     * Obsługuje wyjście z gry.
     * Wysyła komunikat o wyjściu do serwera i zamyka połączenie.
     */
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