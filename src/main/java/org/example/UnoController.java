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
    private static final Logger logger = Logger.getInstance();

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
        logger.info("Inicjalizacja UnoController");

        przeciwnicyKarty = new HashMap<>();
        kartyGracza = new ArrayList<>();
        logger.debug("Struktury danych zainicjalizowane");

        Platform.runLater(() -> {
            if (przyciskDobierania != null) {
                przyciskDobierania.setOnAction(e -> dobierzKarte());
                przyciskDobierania.setDisable(true);  // Początkowo wyłączony
                przyciskDobierania.setStyle("-fx-opacity: 0.5;");
                logger.debug("Przycisk dobierania skonfigurowany");
            }
            uiReady = true;
            logger.info("UI gotowe! Oczekujące wiadomości: " + pendingMessages.size());

            processPendingMessages();
        });

        logger.info("UnoController zainicjalizowany");
    }

    /**
     * Konfiguruje połączenie z serwerem i ustawia nickname gracza.
     *
     * @param connection obiekt ClientConnection do komunikacji z serwerem
     * @param nickname nazwa gracza
     */
    public void setupConnection(ClientConnection connection, String nickname) {
        logger.info("Konfiguracja połączenia dla gracza: " + nickname);
        this.clientConnection = connection;
        this.nickname = nickname;
        logger.debug("Połączenie ustawione dla: " + nickname);

        startMessageReceiver();
        Platform.runLater(() -> {
            clientConnection.sendMessage("INIT_GAME ");
            logger.debug("Wysłano INIT_GAME");
        });
    }

    /**
     * Uruchamia wątek odbierający wiadomości z serwera.
     * Wiadomości są dodawane do kolejki pendingMessages i przetwarzane po gotowości UI.
     */
    private void startMessageReceiver() {
        messageReceiver = new Thread(() -> {
            try {
                logger.info("Rozpoczęto wątek odbierania wiadomości");
                while (gameActive.get() && clientConnection != null && clientConnection.isConnected()) {
                    try {
                        String message = clientConnection.receiveMessage();
                        if (message != null && !message.trim().isEmpty()) {
                            logger.debug("Odebrano w wątku sieciowym: [" + message + "]");

                            // Rozdziel po znakach nowej linii ORAZ po średnikach
                            String[] lines = message.split("\n");
                            for (String line : lines) {
                                if (!line.trim().isEmpty()) {
                                    // Teraz rozdziel po średnikach
                                    String[] parts = line.split(";");
                                    for (String part : parts) {
                                        String trimmedPart = part.trim();
                                        if (!trimmedPart.isEmpty()) {
                                            logger.debug("Dodaję do kolejki: " + trimmedPart);
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
                        logger.error("Błąd w odbiorze wiadomości: " + e.getMessage());
                        logger.error(e, "Szczegóły błędu");
                    }
                }
                logger.info("Wątek odbierania wiadomości zakończony");
            } catch (Exception e) {
                logger.error("Błąd w wątku odbierania: " + e.getMessage());
                logger.error(e, "Szczegóły błędu");
                Platform.runLater(() -> showError("Utracono połączenie z serwerem"));
            }
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();
        logger.debug("Wątek odbierania wiadomości uruchomiony");
    }

    /**
     * Usuwa kartę z ręki gracza na podstawie jej reprezentacji tekstowej.
     *
     * @param cardStr reprezentacja tekstowa karty do usunięcia
     */
    private void removeCardFromHand(String cardStr) {
        Platform.runLater(() -> {
            logger.debug("Usuwanie karty z ręki: " + cardStr);
            for (int i = 0; i < kartyGracza.size(); i++) {
                Card card = kartyGracza.get(i);
                if (card.toString().equals(cardStr)) {
                    kartyGracza.remove(i);
                    if (i < rekaGracza.getChildren().size()) {
                        rekaGracza.getChildren().remove(i);
                    }
                    labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
                    logger.debug("Karta usunięta. Pozostałe karty: " + kartyGracza.size());
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
            logger.warning("UI niegotowe, pomijam przetwarzanie");
            return;
        }

        logger.debug("Przetwarzanie oczekujących wiadomości: " + pendingMessages.size());
        while (!pendingMessages.isEmpty()) {
            String message = pendingMessages.poll();
            logger.debug("Przetwarzam wiadomość z kolejki: " + message);
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
        logger.debug("=== ROZPOCZĘCIE handleServerMessage ===");
        logger.debug("Oryginalna wiadomość: [" + message + "]");

        String trimmed = message.trim();
        logger.debug("Przetwarzam komendę: [" + trimmed + "]");

        if (trimmed.startsWith("INIT_GAME ")) {
            logger.debug("Inicjalizacja gry");
            handleGameInitialization(trimmed.substring(10));
        }
        else if (trimmed.startsWith("PLAY_RESULT ")) {
            logger.debug("Otrzymano wynik zagrania karty");
            handlePlayResult(trimmed.substring(12));
        }
        else if (trimmed.startsWith("HAND ")) {
            String handData = trimmed.substring(5);
            logger.debug("Dane ręki: " + handData);
            updateHand(handData);
        }
        else if (trimmed.startsWith("TOP_CARD ")) {
            logger.debug("Aktualizacja wierzchniej karty");
            updateTopCard(trimmed.substring(9));
        }
        else if (trimmed.startsWith("PLAYERS ")) {
            logger.debug("Aktualizacja graczy");
            updateOpponents(trimmed.substring(8));
        }
        else if (trimmed.startsWith("TURN ")) {
            logger.debug("Aktualizacja tury");
            updateTurn(trimmed.substring(5));
        }
        else if (trimmed.startsWith("PLAYED ")) {
            logger.debug("Karta zagrana");
            handleCardPlayed(trimmed.substring(7));
        }
        else if (trimmed.startsWith("DREW ")) {
            logger.debug("Karta dobrana");
            handleCardDrawn(trimmed.substring(5));
        }
        else if (trimmed.startsWith("WINNER ")) {
            logger.debug("Informacja o zwycięzcy");
            handleWinner(trimmed.substring(7));
        }
        else if (trimmed.startsWith("CHOOSE_COLOR")) {
            logger.debug("Wybór koloru wymagany");
            promptColorChoice();
        }
        else if (trimmed.startsWith("WILD_COLOR ")) {
            logger.debug("Kolor dzikiej karty zaktualizowany");
            updateWildColor(trimmed.substring(11));
        }
        else if (trimmed.startsWith("ERROR")) {
            logger.error("Błąd serwera: " + trimmed);
            showError(trimmed);
        }
        else if (trimmed.startsWith("GAME_ENDED")) {
            logger.info("Gra zakończona przez serwer");
            gameEnded();
        }
        else if (trimmed.startsWith("WINNER ")) {
            logger.info("Otrzymano informację o zwycięzcy");
            handleWinner(trimmed.substring(7));
        }
        else if (trimmed.startsWith("GAME_ENDED")) {
            logger.info("Gra zakończona przez serwer");
            gameEnded();
        }
        else {
            logger.warning("Nieznana komenda: " + trimmed);
        }

        logger.debug("=== ZAKOŃCZENIE handleServerMessage ===\n");
    }

    /**
     * Obsługuje wynik zagrania karty otrzymany z serwera.
     * Aktualizuje wierzchnią kartę, turę, stan przeciwników i rękę gracza.
     *
     * @param playResultData dane wyniku zagrania w formacie:
     *                      "gracz karta wierzchnia_karta aktualny_gracz przeciwnicy ręka"
     */
    private void handlePlayResult(String playResultData) {
        logger.debug("Przetwarzanie PLAY_RESULT: " + playResultData);

        String[] parts = playResultData.split(" ", 6);
        if (parts.length != 6) {
            logger.error("Błędny format PLAY_RESULT: " + playResultData);
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
                logger.debug("Twoja karta została zagrana: " + cardPlayed);
            } else {
                instrukcja.setText("Gracz " + playerWhoPlayed + " zagrał kartę");
                logger.debug("Gracz " + playerWhoPlayed + " zagrał kartę: " + cardPlayed);
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
        logger.info("Inicjalizacja gry: " + initData);

        String[] parts = initData.split(" ", 4);
        if (parts.length != 4) {
            logger.error("Błędny format INIT_GAME: " + initData);
            return;
        }

        String topCard = parts[0];
        String currentPlayer = parts[1];
        String opponents = parts[2];
        String hand = parts[3];

        logger.debug("Top card: " + topCard + ", Current player: " + currentPlayer +
                ", Opponents: " + opponents + ", Hand size: " + hand.split(",").length);

        updateTopCard(topCard);
        updateTurn(currentPlayer);
        updateOpponents(opponents);
        updateHand(hand);

        logger.info("Gra zainicjalizowana");
    }

    /**
     * Aktualizuje wyświetlanie kart w ręce głównego gracza.
     *
     * @param handStr ciąg znaków reprezentujący karty w ręce,
     *               oddzielone przecinkami (np. "RED:5,BLUE:SKIP")
     */
    private void updateHand(String handStr) {
        logger.debug("updateHand wywołane z danymi o długości: " + handStr.length());

        Platform.runLater(() -> {
            try {
                if (rekaGracza == null) {
                    logger.error("ERROR: rekaGracza is null!");
                    return;
                }

                kartyGracza.clear();
                rekaGracza.getChildren().clear();

                String[] cards = handStr.split(",");
                logger.debug("Liczba kart do wyświetlenia: " + cards.length);

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
                            logger.error("Błąd parsowania karty: " + cardStr);
                            logger.error(e, "Szczegóły błędu");
                        }
                    }
                }

                labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
                logger.debug("Ręka zaktualizowana, liczba kart: " + kartyGracza.size());
            } catch (Exception e) {
                logger.error("Błąd w updateHand: " + e.getMessage());
                logger.error(e, "Szczegóły błędu");
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
                logger.debug("Wierzchnia karta ustawiona: " + parts[0] + " " + parts[1]);
            }
        } catch (Exception e) {
            logger.error("Błąd parsowania top card: " + cardStr);
            logger.error(e, "Szczegóły błędu");
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

        logger.debug("Aktualizacja przeciwników: " + playersStr);

        for (String player : players) {
            if (!player.isEmpty()) {
                String[] parts = player.split(":");
                if (parts.length == 2) {
                    przeciwnicyKarty.put(parts[0], Integer.parseInt(parts[1]));
                    logger.debug("Przeciwnik: " + parts[0] + " ma " + parts[1] + " kart");
                }
            }
        }

        updateOpponentDisplays();
        logger.debug("Przeciwnicy zaktualizowani: " + przeciwnicyKarty.size() + " graczy");
    }

    /**
     * Aktualizuje wyświetlanie informacji o przeciwnikach w interfejsie.
     * Rozmieszcza etykiety i karty przeciwników w odpowiednich kontenerach.
     */
    private void updateOpponentDisplays() {
        Platform.runLater(() -> {
            List<String> opponents = new ArrayList<>(przeciwnicyKarty.keySet());

            logger.debug("Aktualizacja wyświetlania przeciwników: " + opponents.size() + " przeciwników");

            if (opponents.size() >= 3) {
                labelPrzeciwnik.setText(opponents.get(0) + " (" + przeciwnicyKarty.get(opponents.get(0)) + ")");
                labelLewy.setText(opponents.get(1) + " (" + przeciwnicyKarty.get(opponents.get(1)) + ")");
                labelPrawy.setText(opponents.get(2) + " (" + przeciwnicyKarty.get(opponents.get(2)) + ")");

                updateHandDisplay(rekaPrzeciwnika, przeciwnicyKarty.get(opponents.get(0)));
                updateHandDisplay(rekaLewego, przeciwnicyKarty.get(opponents.get(1)));
                updateHandDisplay(rekaPrawego, przeciwnicyKarty.get(opponents.get(2)));
                logger.debug("Wyświetlanie 3 przeciwników");
            } else if (opponents.size() == 2) {
                labelPrzeciwnik.setText(opponents.get(0) + " (" + przeciwnicyKarty.get(opponents.get(0)) + ")");
                labelLewy.setText(opponents.get(1) + " (" + przeciwnicyKarty.get(opponents.get(1)) + ")");
                labelPrawy.setText("");

                updateHandDisplay(rekaPrzeciwnika, przeciwnicyKarty.get(opponents.get(0)));
                updateHandDisplay(rekaLewego, przeciwnicyKarty.get(opponents.get(1)));
                rekaPrawego.getChildren().clear();
                logger.debug("Wyświetlanie 2 przeciwników");
            } else if (opponents.size() == 1) {
                labelPrzeciwnik.setText(opponents.get(0) + " (" + przeciwnicyKarty.get(opponents.get(0)) + ")");
                labelLewy.setText("");
                labelPrawy.setText("");

                updateHandDisplay(rekaPrzeciwnika, przeciwnicyKarty.get(opponents.get(0)));
                rekaLewego.getChildren().clear();
                rekaPrawego.getChildren().clear();
                logger.debug("Wyświetlanie 1 przeciwnika");
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
        logger.debug("Wyświetlono " + cardCount + " kart przeciwnika");
    }

    /**
     * Obsługuje akcję dobierania karty przez gracza.
     * Wysyła żądanie do serwera i aktualizuje interfejs.
     */
    @FXML
    private void dobierzKarte() {
        if (myTurn && !waitingForColorChoice && clientConnection != null && clientConnection.isConnected()) {
            logger.info("Dobieranie karty...");

            // Wyłącz przycisk na chwilę, aby uniknąć wielokrotnych kliknięć
            if (przyciskDobierania != null) {
                przyciskDobierania.setDisable(true);
                logger.debug("Przycisk dobierania wyłączony");
            }

            // Wyślij komendę do serwera
            clientConnection.sendMessage("DRAW");
            instrukcja.setText("Dobieranie karty...");
            logger.debug("Wysłano komendę DRAW do serwera");

            // Po krótkim czasie przywróć stan przycisku (jeśli nadal jest tura)
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                if (myTurn && przyciskDobierania != null) {
                                    przyciskDobierania.setDisable(false);
                                    logger.debug("Przycisk dobierania włączony ponownie");
                                }
                            });
                        }
                    },
                    1000  // 1 sekunda
            );
        } else {
            logger.warning("Nie można dobrać karty: tura=" + myTurn +
                    ", waitingForColorChoice=" + waitingForColorChoice +
                    ", connected=" + (clientConnection != null && clientConnection.isConnected()));
        }
    }

    /**
     * Aktualizuje informację o aktualnej turze.
     * Włącza/wyłącza interaktywność kart i przycisków w zależności od tego, czy to tura gracza.
     *
     * @param player nazwa gracza, który ma aktualną turę
     */
    private void updateTurn(String player) {
        logger.debug("=== updateTurn ===");
        logger.debug("Nowy gracz: " + player);
        logger.debug("Ja: " + nickname);
        logger.debug("Czy moja tura? " + player.equals(nickname));

        Platform.runLater(() -> {
            currentPlayer = player;
            myTurn = player.equals(nickname);

            logger.debug("myTurn ustawione na: " + myTurn);

            if (myTurn) {
                labelTura.setText("Twoja tura!");
                instrukcja.setText("Wybierz kartę do zagrania lub kliknij talię, aby dobrać kartę");

                // Włącz przycisk dobierania
                if (przyciskDobierania != null) {
                    przyciskDobierania.setDisable(false);
                    przyciskDobierania.setStyle("-fx-opacity: 1.0; -fx-cursor: hand;");
                    logger.debug("Przycisk dobierania włączony");
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
                logger.debug("Karty odblokowane, liczba kart: " + kartyGracza.size());

            } else {
                labelTura.setText("Tura gracza: " + player);
                instrukcja.setText("Oczekiwanie na ruch gracza " + player);

                // Wyłącz przycisk dobierania
                if (przyciskDobierania != null) {
                    przyciskDobierania.setDisable(true);
                    przyciskDobierania.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
                    logger.debug("Przycisk dobierania wyłączony");
                }

                // Zablokuj wszystkie karty w ręce
                for (var child : rekaGracza.getChildren()) {
                    child.setDisable(true);
                    child.setStyle("-fx-opacity: 0.7; -fx-cursor: default;");
                    child.setOnMouseClicked(null);
                }
                logger.debug("Karty zablokowane, liczba kart: " + kartyGracza.size());
            }

            logger.debug("=== koniec updateTurn ===\n");
        });
    }

    /**
     * Obsługuje informację o zagranej karcie przez innego gracza.
     *
     * @param playInfo informacja o zagranej karcie w formacie "gracz karta"
     */
    private void handleCardPlayed(String playInfo) {
        logger.debug("Otrzymano PLAYED: " + playInfo);

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
                    logger.debug("Gracz " + player + " zagrał kartę: " + cardStr);
                } else {
                    instrukcja.setText("Twoja karta została zagrana");
                    logger.debug("Twoja karta została zagrana: " + cardStr);
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
        logger.info("handleCardDrawn: Otrzymano DREW - " + cardStr);

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

                logger.info("Karta dodana do ręki, oczekiwanie na TURN...");
                logger.debug("Liczba kart po dobraniu: " + kartyGracza.size());

            } catch (Exception e) {
                logger.error("Błąd podczas dodawania dobranej karty: " + e.getMessage());
                logger.error(e, "Szczegóły błędu");
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
        logger.info("Zwycięzca gry: " + winner);
        Platform.runLater(() -> {
            // Wyświetl komunikat o zwycięzcy
            if (winner.equals(nickname)) {
                instrukcja.setText("WYGRAŁEŚ!");
                instrukcja.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
                logger.info("Gratulacje! Wygrałeś grę!");
            } else {
                instrukcja.setText("Wygrał gracz " + winner);
                instrukcja.setStyle("-fx-text-fill: silver; -fx-font-size: 36px; -fx-font-weight: bold;");
                logger.info("Wygrał gracz " + winner);
            }

            zablokujKarty();

            // Wyłącz przycisk dobierania
            if (przyciskDobierania != null) {
                przyciskDobierania.setDisable(true);
                przyciskDobierania.setStyle("-fx-opacity: 0.5;");
                logger.debug("Przycisk dobierania wyłączony");
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
            logger.debug("Alert końca gry wyświetlony");

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
                                        logger.debug("Połączenie z serwerem zamknięte");
                                    }

                                    logger.info("Powrót do menu głównego po zakończeniu gry");

                                } catch (Exception e) {
                                    logger.error("Błąd podczas przełączania do menu: " + e.getMessage());
                                    logger.error(e, "Szczegóły błędu");
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
        logger.info("Gra zakończona przez serwer");

        Platform.runLater(() -> {
            instrukcja.setText("Gra zakończona");
            instrukcja.setStyle("-fx-text-fill: red; -fx-font-size: 36px; -fx-font-weight: bold;");

            // Pokaż alert informacyjny
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gra zakończona");
            alert.setHeaderText(null);
            alert.setContentText("Gra została zakończona przez serwer.\nPowrót do menu głównego za 3 sekundy...");
            alert.show();
            logger.debug("Alert zakończenia gry wyświetlony");

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
                                        logger.debug("Połączenie z serwerem zamknięte");
                                    }

                                    logger.info("Powrót do menu głównego po zakończeniu gry przez serwer");

                                } catch (Exception e) {
                                    logger.error("Błąd podczas przełączania do menu: " + e.getMessage());
                                    logger.error(e, "Szczegóły błędu");
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
        logger.info("Wymagany wybór koloru po zagraniu karty WILD");

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
                    logger.info("Wybrano kolor: " + color);
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
        logger.debug("Kolor dzikiej karty zmieniony na: " + color);
    }

    /**
     * Obsługuje żądanie dobrania karty (alternatywna metoda dla przycisku).
     */
    @FXML
    private void handleDrawCard() {
        logger.debug("handleDrawCard wywołany");
        if (myTurn && !waitingForColorChoice) {
            clientConnection.sendMessage("DRAW");
            logger.debug("Wysłano komendę DRAW (alternatywna metoda)");
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
            logger.info("Wysłano kartę do serwera: " + cardStr);

            instrukcja.setText("Wysyłanie karty...");
        } else {
            logger.warning("Nie można zagrać karty: tura=" + myTurn +
                    ", waitingForColorChoice=" + waitingForColorChoice);
        }
    }

    /**
     * Wyświetla okno dialogowe z błędem.
     *
     * @param message komunikat błędu do wyświetlenia
     */
    private void showError(String message) {
        logger.error("Wyświetlam błąd: " + message);
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
        logger.debug("Blokowanie kart w ręce");
        for (var child : rekaGracza.getChildren()) {
            child.setDisable(true);
            child.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
        }
        logger.debug("Karty zablokowane");
    }

    /**
     * Obsługuje wyjście z gry.
     * Wysyła komunikat o wyjściu do serwera i zamyka połączenie.
     */
    @FXML
    private void handleQuit() {
        logger.info("Wychodzę z gry dla gracza: " + nickname);
        gameActive.set(false);
        if (messageReceiver != null) {
            messageReceiver.interrupt();
            logger.debug("Wątek odbierania wiadomości przerwany");
        }
        if (clientConnection != null) {
            clientConnection.sendMessage("EXIT " + nickname);
            clientConnection.disconnect();
            logger.info("Połączenie z serwerem zamknięte");
        }
    }
}