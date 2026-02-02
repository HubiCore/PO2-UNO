package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Główna klasa serwera gry UNO.
 * Serwer obsługuje połączenia klientów, zarządza pokojami gry, logiką gry
 * oraz komunikacją z bazą danych użytkowników.
 * Uruchamia się na porcie 2137 i tworzy osobny wątek dla każdego klienta.
 *
 */
public class UnoServer extends Thread {


    private final Logger logger = Logger.getInstance();

    /** Połączenie z bazą danych */
    private Connection conn;

    /** Instancja bazy danych do zarządzania użytkownikami i wynikami */
    private DataBase db;

    /** Mapa przechowująca połączonych klientów (nickname -> ClientHandler) */
    private static Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    /** Mapa przechowująca status gotowości klientów (nickname -> status) */
    private static Map<String, String> clientStatus = new ConcurrentHashMap<>(); // "READY", "NOT_READY"

    /** Mapa przechowująca pokoje gry (roomId -> GameRoom) */
    private static Map<Integer, GameRoom> gameRooms = new ConcurrentHashMap<>(); // Nowa mapa pokoi

    /** Licznik do generowania kolejnych identyfikatorów pokojów */
    private static int nextRoomId = 1;

    /**
     * Główna metoda uruchamiająca serwer.
     * Nasłuchuje na porcie 2137, akceptuje połączenia klientów
     * i uruchamia dla każdego osobny wątek ClientHandler.
     */
    public void run() {
        db = new DataBase();
        conn = db.connect("src/main/resources/baza.sql");

        if (conn == null) {
            logger.error("Failed to connect to database");
            return;
        }

        int portNumber = 2137;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            logger.info("Server is listening on port " + portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected from: " + clientSocket.getInetAddress());

                new ClientHandler(clientSocket, conn, db).start();
            }
        } catch (IOException e) {
            logger.error(e, "Exception caught when trying to listen on port " + portNumber);
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    logger.info("Database connection closed");
                }
            } catch (SQLException e) {
                logger.error(e, "Error closing database connection");
            }

            // Zamknij logger przy wyłączaniu serwera
            Logger.getInstance().shutdown();
        }
    }

    /**
     * Klasa reprezentująca pokój gry UNO.
     * Zarządza graczami w pokoju, stanem gry i logiką związaną z pokojem.
     */
    static class GameRoom {
        /** Identyfikator pokoju */
        private int roomId;

        /** Lista graczy w pokoju */
        private List<String> players;

        /** Bieżąca gra tocząca się w pokoju */
        private Game currentGame;

        /** Flaga wskazująca czy gra jest w trakcie */
        private boolean gameInProgress;

        /** Status pokoju: "WAITING", "FULL", "IN_PROGRESS" */
        private String roomStatus; // "WAITING", "FULL", "IN_PROGRESS"

        private final Logger logger = Logger.getInstance();

        /**
         * Tworzy nowy pokój gry z określonym identyfikatorem.
         *
         * @param roomId identyfikator pokoju
         */
        public GameRoom(int roomId) {
            this.roomId = roomId;
            this.players = new ArrayList<>();
            this.gameInProgress = false;
            this.roomStatus = "WAITING";
            logger.debug("GameRoom " + roomId + " created");
        }

        /**
         * Dodaje gracza do pokoju, jeśli pokój nie jest pełny
         * i gra nie jest w trakcie.
         *
         * @param player nick gracza
         * @return true jeśli gracz został dodany, false w przeciwnym razie
         */
        public boolean addPlayer(String player) {
            if (players.size() >= 4 || gameInProgress) {
                logger.debug("Cannot add player " + player + " to room " + roomId + " - room full or game in progress");
                return false;
            }
            players.add(player);
            logger.info("Player " + player + " joined room " + roomId);

            if (players.size() == 4) {
                roomStatus = "FULL";
                logger.info("Room " + roomId + " is now full");
            }
            return true;
        }

        /**
         * Usuwa gracza z pokoju.
         *
         * @param player nick gracza do usunięcia
         * @return true jeśli gracz został usunięty, false w przeciwnym razie
         */
        public boolean removePlayer(String player) {
            boolean removed = players.remove(player);
            if (removed) {
                logger.info("Player " + player + " left room " + roomId);
                if (players.size() < 4 && !gameInProgress) {
                    roomStatus = "WAITING";
                }
            }
            return removed;
        }

        /**
         * Rozpoczyna nową grę w pokoju, jeśli jest wystarczająco graczy
         * (2-4) i żadna gra nie jest w trakcie.
         */
        public void startGame() {
            if (players.size() >= 2 && players.size() <= 4 && !gameInProgress) {
                currentGame = new Game(new ArrayList<>(players));
                gameInProgress = true;
                roomStatus = "IN_PROGRESS";
                logger.info("Game started in room " + roomId + " with players: " + players);
            }
        }

        /**
         * Kończy bieżącą grę i resetuje stan pokoju.
         */
        public void endGame() {
            logger.info("Game ended in room " + roomId);
            currentGame = null;
            gameInProgress = false;
            roomStatus = "WAITING";
        }

        /**
         * Pobiera identyfikator pokoju.
         *
         * @return identyfikator pokoju
         */
        public int getRoomId() { return roomId; }

        /**
         * Pobiera listę graczy w pokoju.
         *
         * @return lista nicków graczy
         */
        public List<String> getPlayers() { return players; }

        /**
         * Pobiera bieżącą grę toczącą się w pokoju.
         *
         * @return instancja gry lub null jeśli gra nie jest aktywna
         */
        public Game getGame() { return currentGame; }

        /**
         * Sprawdza czy gra jest w trakcie.
         *
         * @return true jeśli gra jest aktywna, false w przeciwnym razie
         */
        public boolean isGameInProgress() { return gameInProgress; }

        /**
         * Pobiera status pokoju.
         *
         * @return status pokoju: "WAITING", "FULL", "IN_PROGRESS"
         */
        public String getRoomStatus() { return roomStatus; }

        /**
         * Sprawdza czy pokój jest pełny (4 graczy).
         *
         * @return true jeśli pokój jest pełny, false w przeciwnym razie
         */
        public boolean isFull() { return players.size() >= 4; }

        /**
         * Sprawdza czy pokój jest pusty.
         *
         * @return true jeśli pokój nie ma graczy, false w przeciwnym razie
         */
        public boolean isEmpty() { return players.isEmpty(); }
    }

    /**
     * Tworzy nowy pokój gry z kolejnym dostępnym ID.
     *
     * @return nowo utworzony pokój gry
     */
    private static synchronized GameRoom createNewRoom() {
        int roomId = nextRoomId++;
        GameRoom room = new GameRoom(roomId);
        gameRooms.put(roomId, room);
        Logger.getInstance().info("Created new room: " + roomId);
        return room;
    }

    /**
     * Znajduje dostępny pokój dla gracza.
     * Priorytety:
     * 1. Pokój, w którym gracz już jest
     * 2. Pokój z miejscem, który nie jest w trakcie gry
     * 3. Nowy pokój jeśli nie ma dostępnych
     *
     * @param player nick gracza
     * @return dostępny pokój gry
     */
    private static GameRoom findAvailableRoomForPlayer(String player) {
        // 1. Spróbuj znaleźć pokój, w którym gracz już jest
        for (GameRoom room : gameRooms.values()) {
            if (room.getPlayers().contains(player)) {
                return room;
            }
        }

        // 2. Spróbuj znaleźć pokój z miejscem, który nie jest w trakcie gry
        for (GameRoom room : gameRooms.values()) {
            if (!room.isGameInProgress() && !room.isFull()) {
                return room;
            }
        }

        // 3. Jeśli nie ma dostępnego pokoju, utwórz nowy
        return createNewRoom();
    }

    /**
     * Klasa zarządzająca logiką gry UNO.
     * Obsługuje talię, rozdawanie kart, turę graczy i specjalne karty.
     */
    static class Game {
        /** Lista graczy w grze */
        private List<String> players;

        /** Mapa przechowująca karty w rękach graczy (gracz -> lista kart) */
        private Map<String, List<Cart>> hands;

        /** Talia kart do dobierania */
        private List<Cart> deck;

        /** Stos kart odrzuconych */
        private List<Cart> discardPile;

        /** Indeks aktualnego gracza */
        private int currentPlayerIndex;

        /** Kierunek gry: true = zgodnie z ruchem wskazówek zegara */
        private boolean direction; // true = clockwise

        /** Aktualny kolor na stole */
        private String currentColor;

        /** Aktualna wartość na stole */
        private String currentValue;

        private final Logger logger = Logger.getInstance();

        /**
         * Tworzy nową grę dla podanej listy graczy.
         * Inicjalizuje talię, rozdaje karty i ustala pierwszą kartę na stosie.
         *
         * @param players lista graczy biorących udział w grze
         */
        public Game(List<String> players) {
            this.players = new ArrayList<>(players);
            this.hands = new HashMap<>();
            this.deck = new ArrayList<>();
            this.discardPile = new ArrayList<>();
            this.currentPlayerIndex = 0;
            this.direction = true;

            initDeck();
            shuffleDeck();
            dealCards();

            Cart firstCard = drawFromDeck();
            discardPile.add(firstCard);
            currentColor = firstCard.getKolor();
            currentValue = firstCard.getWartosc();

            logger.info("Game created with players: " + players);

            while (firstCard.getWartosc().equals("+2") ||
                    firstCard.getWartosc().equals("⏸") ||
                    firstCard.getWartosc().equals("↺")) {
                deck.add(discardPile.remove(0));
                shuffleDeck();
                firstCard = drawFromDeck();
                discardPile.add(firstCard);
                currentColor = firstCard.getKolor();
                currentValue = firstCard.getWartosc();
            }
        }

        /**
         * Inicjalizuje talię kart UNO.
         */
        private void initDeck() {
            String[] colors = {"RED", "GREEN", "BLUE", "YELLOW"};
            String[] values = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                    "+2", "⏸", "↺"};

            for (String color : colors) {
                deck.add(new Cart(color, "0"));
                for (String value : values) {
                    if (!value.equals("0")) {
                        deck.add(new Cart(color, value));
                        deck.add(new Cart(color, value));
                    }
                }
            }
        }

        /**
         * Tasuje talię kart.
         */
        private void shuffleDeck() {
            Collections.shuffle(deck);
        }

        /**
         * Rozdaje początkowe karty graczom (7 kart każdemu).
         */
        private void dealCards() {
            for (String player : players) {
                List<Cart> hand = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    hand.add(drawFromDeck());
                }
                hands.put(player, hand);
            }
        }

        /**
         * Dobiera kartę z talii.
         * Jeśli talia jest pusta, miesza stos kart odrzuconych (oprócz górnej karty).
         *
         * @return dobrana karta lub null jeśli nie ma kart
         */
        private Cart drawFromDeck() {
            if (deck.isEmpty()) {
                if (discardPile.size() > 1) {
                    Cart topCard = discardPile.remove(discardPile.size() - 1);
                    deck.addAll(discardPile);
                    discardPile.clear();
                    discardPile.add(topCard);
                    shuffleDeck();
                }
            }
            return deck.isEmpty() ? null : deck.remove(deck.size() - 1);
        }

        /**
         * Pobiera rękę (listę kart) dla określonego gracza.
         *
         * @param player nick gracza
         * @return lista kart gracza lub null jeśli gracz nie istnieje
         */
        public List<Cart> getHandForPlayer(String player) {
            return hands.get(player);
        }

        /**
         * Pobiera górną kartę ze stosu kart odrzuconych.
         *
         * @return górna karta lub null jeśli stos jest pusty
         */
        public Cart getTopCard() {
            return discardPile.isEmpty() ? null : discardPile.get(discardPile.size() - 1);
        }

        /**
         * Pobiera nick aktualnego gracza (który ma turę).
         *
         * @return nick aktualnego gracza
         */
        public String getCurrentPlayer() {
            return players.get(currentPlayerIndex);
        }

        /**
         * Próbuje zagrać kartę przez gracza.
         * Sprawdza czy to tura gracza i czy karta może być zagrana.
         *
         * @param player nick gracza próbującego zagrać kartę
         * @param card karta do zagrania
         * @return true jeśli karta została zagrana pomyślnie, false w przeciwnym razie
         */
        public boolean playCard(String player, Cart card) {
            logger.debug("Game.playCard: player " + player + " trying to play " + card);

            if (!player.equals(players.get(currentPlayerIndex))) {
                logger.debug("Not player " + player + "'s turn");
                return false;
            }

            Cart topCard = getTopCard();
            if (canPlayOn(card, topCard)) {
                List<Cart> playerHand = hands.get(player);
                boolean removed = false;

                for (int i = 0; i < playerHand.size(); i++) {
                    Cart c = playerHand.get(i);
                    if (c.toString().equals(card.toString())) {
                        playerHand.remove(i);
                        removed = true;
                        logger.debug("Removed card: " + card.toString() + " from player " + player);
                        break;
                    }
                }

                if (!removed) {
                    logger.warning("Card not found in player's hand: " + player);
                    return false;
                }

                discardPile.add(card);
                currentColor = card.getKolor();
                currentValue = card.getWartosc();

                handleSpecialCard(card);
                logger.info("Player " + player + " played card: " + card);

                nextPlayer();
                return true;
            }
            logger.debug("Card cannot be played: " + card);
            return false;
        }

        /**
         * Sprawdza czy karta może być zagrana na aktualną kartę na stole.
         *
         * @param card karta do zagrania
         * @param topCard aktualna górna karta na stosie
         * @return true jeśli karta może być zagrana, false w przeciwnym razie
         */
        private boolean canPlayOn(Cart card, Cart topCard) {
            return card.getKolor().equals(currentColor) ||
                    card.getWartosc().equals(currentValue);
        }

        /**
         * Obsługuje efekty specjalnych kart.
         *
         * @param card zagrana karta specjalna
         */
        private void handleSpecialCard(Cart card) {
            String value = card.getWartosc();
            switch (value) {
                case "⏸":
                    nextPlayer();
                    break;
                case "↺":
                    direction = !direction;
                    Collections.reverse(players);
                    currentPlayerIndex = players.indexOf(getCurrentPlayer());
                    break;
                case "+2":
                    nextPlayer();
                    String nextPlayer = getCurrentPlayer();
                    for (int i = 0; i < 2; i++) {
                        Cart drawnCard = drawFromDeck();
                        if (drawnCard != null) {
                            hands.get(nextPlayer).add(drawnCard);
                        }
                    }
                    nextPlayer();
                    break;
            }
        }

        /**
         * Przechodzi do następnego gracza zgodnie z kierunkiem gry.
         */
        private void nextPlayer() {
            if (direction) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } else {
                currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
            }
        }

        /**
         * Pozwala graczowi dobrać kartę z talii.
         *
         * @param player nick gracza
         * @return dobrana karta lub null jeśli talia jest pusta
         */
        public Cart drawCardForPlayer(String player) {
            Cart card = drawFromDeck();
            if (card != null) {
                hands.get(player).add(card);
            }
            return card;
        }

        /**
         * Sprawdza czy gracz wygrał grę (nie ma kart w ręce).
         *
         * @param player nick gracza
         * @return true jeśli gracz wygrał, false w przeciwnym razie
         */
        public boolean hasPlayerWon(String player) {
            boolean won = hands.get(player).isEmpty();
            if (won) {
                logger.info("Player " + player + " has won the game!");
            }
            return won;
        }

        /**
         * Pobiera listę graczy w grze.
         *
         * @return lista graczy
         */
        public List<String> getPlayers() {
            return players;
        }

        /**
         * Pobiera mapę rąk wszystkich graczy.
         *
         * @return mapa (gracz -> lista kart)
         */
        public Map<String, List<Cart>> getHands() {
            return hands;
        }
    }

    /**
     * Obsługuje połączenie z pojedynczym klientem.
     * Zarządza komunikacją, autoryzacją i stanem gry dla klienta.
     */
    static class ClientHandler extends Thread {
        /** Gniazdo klienta */
        private Socket clientSocket;

        /** Połączenie z bazą danych */
        private Connection conn;

        /** Instancja bazy danych */
        private DataBase db;

        /** Strumień wyjściowy do klienta */
        private PrintWriter out;

        /** Strumień wejściowy od klienta */
        private BufferedReader in;

        /** Nickname klienta */
        private String nickname;

        /** ID pokoju, w którym znajduje się gracz */
        private int currentRoomId = -1; // ID pokoju, w którym znajduje się gracz

        private final Logger logger = Logger.getInstance();

        /**
         * Tworzy nowy handler dla klienta.
         *
         * @param socket gniazdo klienta
         * @param conn połączenie z bazą danych
         * @param db instancja bazy danych
         */
        public ClientHandler(Socket socket, Connection conn, DataBase db) {
            this.clientSocket = socket;
            this.conn = conn;
            this.db = db;
        }

        /**
         * Główna metoda obsługi klienta.
         * Nasłuchuje na wiadomości od klienta i deleguje je do handlera.
         */
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    logger.debug("Received from client " + (nickname != null ? nickname : "unknown") + ": " + inputLine);
                    handleInput(inputLine);
                }
            } catch (IOException e) {
                logger.error(e, "Error handling client " + nickname);
            } finally {
                cleanup();
            }
        }

        /**
         * Rozpoznaje i obsługuje polecenie od klienta.
         *
         * @param inputLine linia wejściowa od klienta
         */
        private void handleInput(String inputLine) {
            try {
                if (inputLine.startsWith("LOGIN ")) {
                    handleLogin(inputLine.substring(6));
                } else if (inputLine.startsWith("READY ")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    handleReady(inputLine.substring(6));
                } else if (inputLine.startsWith("UNREADY ")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    handleUnready(inputLine.substring(8));
                } else if (inputLine.startsWith("EXIT ")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    handleExit(inputLine.substring(5));
                } else if (inputLine.startsWith("INIT_GAME ")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    logger.info("Received game initialization from " + nickname);
                    initialize_game();
                } else if ("TOP5".equalsIgnoreCase(inputLine)) {
                    String top5 = db.Top5_Best(conn);
                    logger.debug("Top5 request from " + nickname);
                    out.println("TOP5 " + top5);
                } else if ("LIST".equalsIgnoreCase(inputLine)) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    broadcastUserList();
                } else if ("ROOM_LIST".equalsIgnoreCase(inputLine)) {
                    sendRoomList();
                } else if (inputLine.startsWith("JOIN_ROOM ")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    joinRoom(inputLine.substring(10));
                } else if (inputLine.startsWith("CREATE_ROOM")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    createRoom();
                } else if (inputLine.startsWith("PLAY ")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    handlePlay(inputLine.substring(5));
                } else if (inputLine.equals("DRAW")) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    handleDraw();
                } else if ("GET_GAME_STATE".equals(inputLine)) {
                    if (nickname == null) {
                        out.println("ERROR_NOT_LOGGED_IN");
                        return;
                    }
                    sendGameState();
                } else if ("quit".equalsIgnoreCase(inputLine)) {
                    out.println("Bye bye!");
                } else {
                    out.println("Server received: " + inputLine);
                }
            } catch (Exception e) {
                logger.error(e, "Error handling input from " + nickname + ": " + inputLine);
                out.println("ERROR Internal server error");
            }
        }

        /**
         * Obsługuje polecenie logowania klienta.
         *
         * @param loginData dane logowania w formacie "username:password_hash"
         */
        private void handleLogin(String loginData) {
            logger.info("=== LOGIN ATTEMPT ===");
            String[] parts = loginData.split(":");
            if (parts.length != 2) {
                logger.warning("Invalid login format: " + loginData);
                out.println("LOGIN_ERROR Invalid format. Use: LOGIN username:password_hash");
                return;
            }

            String username = parts[0];
            String passwordHash = parts[1];

            logger.debug("Login attempt for user: " + username);

            if (connectedClients.containsKey(username)) {
                logger.warning("User already connected: " + username);
                out.println("LOGIN_ERROR User already connected");
                return;
            }

            boolean userExists = db.is_player(conn, username);
            logger.debug("User exists in database: " + userExists);

            if (!userExists) {
                logger.info("Creating new user: " + username);
                boolean created = db.createUserIfNotExists(conn, username, passwordHash);
                if (!created) {
                    logger.error("Failed to create user: " + username);
                    out.println("LOGIN_ERROR Failed to create user");
                    return;
                }
                logger.info("New user created: " + username);
                loginUser(username);
                return;
            }

            String storedPasswordHash = db.getPasswordHash(conn, username);

            if (storedPasswordHash == null) {
                logger.error("User not found in database: " + username);
                out.println("LOGIN_ERROR User not found in database");
                return;
            }

            if (!storedPasswordHash.equals(passwordHash)) {
                logger.warning("Invalid password for user: " + username);
                out.println("LOGIN_ERROR Invalid password");
                return;
            }

            logger.info("Login successful for user: " + username);
            loginUser(username);
        }

        /**
         * Kończy proces logowania i przypisuje użytkownika do pokoju.
         *
         * @param username nick użytkownika
         */
        private void loginUser(String username) {
            this.nickname = username;
            connectedClients.put(username, this);
            clientStatus.put(username, "NOT_READY");

            out.println("LOGIN_SUCCESS " + username);

            GameRoom room = findAvailableRoomForPlayer(username);
            currentRoomId = room.getRoomId();
            if (room.addPlayer(username)) {
                out.println("ROOM_ASSIGNED " + currentRoomId);
                broadcastToRoom(currentRoomId, "USER_JOINED " + username);
                broadcastUserListToRoom(currentRoomId);
            }

            logger.info("Login completed for: " + username);
        }

        /**
         * Wysyła listę pokoi do klienta.
         */
        private void sendRoomList() {
            StringBuilder roomList = new StringBuilder("ROOM_LIST ");
            for (GameRoom room : gameRooms.values()) {
                roomList.append(room.getRoomId())
                        .append(":")
                        .append(room.getPlayers().size())
                        .append("/4:")
                        .append(room.getRoomStatus())
                        .append(",");
            }
            out.println(roomList.toString());
        }

        /**
         * Dołącza klienta do określonego pokoju.
         *
         * @param roomIdStr identyfikator pokoju jako ciąg znaków
         */
        private void joinRoom(String roomIdStr) {
            try {
                int roomId = Integer.parseInt(roomIdStr);
                GameRoom room = gameRooms.get(roomId);

                if (room == null) {
                    out.println("ERROR Room does not exist");
                    return;
                }

                if (room.isGameInProgress()) {
                    out.println("ERROR Game already in progress");
                    return;
                }

                if (room.isFull()) {
                    out.println("ERROR Room is full");
                    return;
                }

                // Opuść obecny pokój
                if (currentRoomId != -1) {
                    GameRoom currentRoom = gameRooms.get(currentRoomId);
                    if (currentRoom != null) {
                        currentRoom.removePlayer(nickname);
                        broadcastToRoom(currentRoomId, "USER_LEFT " + nickname);
                        if (currentRoom.isEmpty()) {
                            gameRooms.remove(currentRoomId);
                        }
                    }
                }

                // Dołącz do nowego pokoju
                currentRoomId = roomId;
                if (room.addPlayer(nickname)) {
                    out.println("ROOM_JOINED " + roomId);
                    broadcastToRoom(currentRoomId, "USER_JOINED " + nickname);
                    broadcastUserListToRoom(currentRoomId);
                } else {
                    out.println("ERROR Cannot join room");
                }
            } catch (NumberFormatException e) {
                out.println("ERROR Invalid room ID");
            }
        }

        /**
         * Tworzy nowy pokój i dołącza do niego klienta.
         */
        private void createRoom() {
            GameRoom newRoom = createNewRoom();
            currentRoomId = newRoom.getRoomId();
            newRoom.addPlayer(nickname);
            out.println("ROOM_CREATED " + currentRoomId);
            broadcastUserListToRoom(currentRoomId);
        }

        /**
         * Obsługuje polecenie gotowości klienta.
         *
         * @param nick nick gracza
         */
        private void handleReady(String nick) {
            if (!nick.equals(nickname)) {
                out.println("ERROR Invalid nickname");
                return;
            }

            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }

            GameRoom room = gameRooms.get(currentRoomId);
            if (room == null) {
                out.println("ERROR Room not found");
                return;
            }

            if (room.isGameInProgress()) {
                out.println("ERROR_GAME_IN_PROGRESS");
                return;
            }

            clientStatus.put(nick, "READY");
            broadcastToRoom(currentRoomId, "READY " + nick);
            logger.info("Player " + nick + " is ready in room " + currentRoomId);

            boolean allReady = true;
            int readyCount = 0;
            for (String player : room.getPlayers()) {
                if ("READY".equals(clientStatus.get(player))) {
                    readyCount++;
                } else {
                    allReady = false;
                }
            }

            if (allReady && readyCount >= 2 && readyCount <= 4) {
                logger.info("All players ready in room " + currentRoomId + ", starting game");
                broadcastToRoom(currentRoomId, "START_GAME");
                room.startGame();
            }
        }

        /**
         * Obsługuje polecenie "niegotowości" klienta.
         *
         * @param nick nick gracza
         */
        private void handleUnready(String nick) {
            if (!nick.equals(nickname)) {
                out.println("ERROR Invalid nickname");
                return;
            }

            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }

            clientStatus.put(nick, "NOT_READY");
            broadcastToRoom(currentRoomId, "UNREADY " + nick);
            logger.info("Player " + nick + " is not ready in room " + currentRoomId);
        }

        /**
         * Obsługuje polecenie wyjścia z gry.
         *
         * @param nick nick gracza
         */
        private void handleExit(String nick) {
            if (!nick.equals(nickname)) {
                out.println("ERROR Invalid nickname");
                return;
            }

            if (currentRoomId != -1) {
                GameRoom room = gameRooms.get(currentRoomId);
                if (room != null) {
                    room.removePlayer(nick);
                    broadcastToRoom(currentRoomId, "USER_LEFT " + nick);
                    if (room.isEmpty()) {
                        gameRooms.remove(currentRoomId);
                        logger.info("Room " + currentRoomId + " removed (empty)");
                    } else {
                        broadcastUserListToRoom(currentRoomId);
                    }
                }
            }

            connectedClients.remove(nick);
            clientStatus.remove(nick);
            logger.info("Player " + nick + " exited");
        }


        /**
         * Inicjalizuje grę i wysyła stan początkowej gry do wszystkich graczy w pokoju.
         */
        private void initialize_game() {
            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }

            GameRoom room = gameRooms.get(currentRoomId);
            if (room == null || !room.isGameInProgress()) {
                out.println("ERROR Game not started");
                return;
            }

            Game currentGame = room.getGame();
            List<String> players = currentGame.getPlayers();

            for (String player : players) {
                ClientHandler client = connectedClients.get(player);
                if (client != null) {
                    List<Cart> hand = currentGame.getHandForPlayer(player);
                    StringBuilder handStr = new StringBuilder();
                    for (int i = 0; i < hand.size(); i++) {
                        handStr.append(hand.get(i).toString());
                        if (i < hand.size() - 1) handStr.append(",");
                    }
                    Cart topCard = currentGame.getTopCard();
                    String topCardStr = topCard != null ? topCard.toString() : "";
                    String currentPlayer = currentGame.getCurrentPlayer();
                    StringBuilder opponentsStr = new StringBuilder();
                    for (String p : players) {
                        if (!p.equals(player)) {
                            int handSize = currentGame.getHandForPlayer(p).size();
                            opponentsStr.append(p).append(":").append(handSize);
                            if (!p.equals(players.get(players.size() - 1))) {
                                opponentsStr.append(",");
                            }
                        }
                    }

                    String initMessage = String.format("INIT_GAME %s %s %s %s",
                            topCardStr,
                            currentPlayer,
                            opponentsStr.toString(),
                            handStr.toString());

                    client.out.println(initMessage);
                }
            }
        }

        /**
         * Obsługuje próbę zagrania karty przez gracza.
         *
         * @param cardStr karta w formacie string
         */
        private void handlePlay(String cardStr) {
            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }

            GameRoom room = gameRooms.get(currentRoomId);
            if (room == null || !room.isGameInProgress()) {
                out.println("ERROR No game in progress");
                return;
            }

            Game currentGame = room.getGame();
            if (currentGame == null || nickname == null) {
                out.println("ERROR No game in progress");
                return;
            }

            try {
                logger.debug("Attempting to play card: " + cardStr + " by player: " + nickname);
                Cart card = Cart.fromString(cardStr);

                boolean success = currentGame.playCard(nickname, card);

                if (success) {
                    logger.info("Card played successfully by: " + nickname);

                    Cart topCard = currentGame.getTopCard();
                    String currentPlayer = currentGame.getCurrentPlayer();

                    if (currentGame.hasPlayerWon(nickname)) {
                        logger.info("Player " + nickname + " won the game!");
                        db.increaseWins(conn, nickname);

                        broadcastToRoom(currentRoomId, "WINNER " + nickname);
                        room.endGame();
                        return;
                    }

                    for (String player : currentGame.getPlayers()) {
                        ClientHandler client = connectedClients.get(player);
                        if (client != null) {
                            StringBuilder opponentsStr = new StringBuilder();
                            List<String> playersList = currentGame.getPlayers();
                            for (String p : playersList) {
                                if (!p.equals(player)) {
                                    int handSize = currentGame.getHandForPlayer(p).size();
                                    opponentsStr.append(p).append(":").append(handSize);
                                    if (!p.equals(playersList.get(playersList.size() - 1))) {
                                        opponentsStr.append(",");
                                    }
                                }
                            }

                            List<Cart> hand = currentGame.getHandForPlayer(player);
                            StringBuilder handStr = new StringBuilder();
                            for (int i = 0; i < hand.size(); i++) {
                                handStr.append(hand.get(i).toString());
                                if (i < hand.size() - 1) handStr.append(",");
                            }

                            String message = String.format("PLAY_RESULT %s %s %s %s %s %s",
                                    nickname,
                                    cardStr,
                                    topCard != null ? topCard.toString() : "",
                                    currentPlayer,
                                    opponentsStr.toString(),
                                    handStr.toString());

                            client.out.println(message);
                        }
                    }

                } else {
                    logger.debug("Cannot play card: " + cardStr);
                    out.println("ERROR Nie można zagrać tej karty");
                    out.println("TURN " + currentGame.getCurrentPlayer());
                }
            } catch (Exception e) {
                logger.error(e, "Error in handlePlay for player " + nickname);
                out.println("ERROR Nieprawidłowy format karty");
                out.println("TURN " + currentGame.getCurrentPlayer());
            }
        }

        /**
         * Obsługuje dobranie karty przez gracza.
         */
        private void handleDraw() {
            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }

            GameRoom room = gameRooms.get(currentRoomId);
            if (room == null || !room.isGameInProgress()) {
                out.println("ERROR No game in progress");
                return;
            }

            Game currentGame = room.getGame();
            if (currentGame == null || nickname == null) {
                out.println("ERROR No game in progress");
                return;
            }

            if (!nickname.equals(currentGame.getCurrentPlayer())) {
                logger.debug("Not player's turn: " + nickname);
                out.println("ERROR Not your turn");
                return;
            }

            Cart drawnCard = currentGame.drawCardForPlayer(nickname);
            if (drawnCard != null) {
                logger.debug("Player " + nickname + " drew a card");
                out.println("DREW " + drawnCard.toString());

                List<Cart> hand = currentGame.getHandForPlayer(nickname);
                StringBuilder handStr = new StringBuilder("HAND ");
                for (int i = 0; i < hand.size(); i++) {
                    handStr.append(hand.get(i).toString());
                    if (i < hand.size() - 1) handStr.append(",");
                }
                out.println(handStr.toString());

                currentGame.nextPlayer();
                broadcastToRoom(currentRoomId, "TURN " + currentGame.getCurrentPlayer());
                updateAllPlayerHandsInRoom();
            } else {
                logger.warning("No cards to draw for player " + nickname);
                out.println("ERROR No cards to draw");
            }
        }

        /**
         * Wysyła aktualny stan gry do klienta.
         */
        private void sendGameState() {
            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }

            GameRoom room = gameRooms.get(currentRoomId);
            if (room == null || !room.isGameInProgress()) {
                out.println("ERROR No game in progress");
                return;
            }

            Game currentGame = room.getGame();
            if (currentGame != null && nickname != null) {
                List<Cart> hand = currentGame.getHandForPlayer(nickname);
                StringBuilder handStr = new StringBuilder("HAND ");
                for (int i = 0; i < hand.size(); i++) {
                    handStr.append(hand.get(i).toString());
                    if (i < hand.size() - 1) handStr.append(",");
                }
                out.println(handStr.toString());

                Cart topCard = currentGame.getTopCard();
                if (topCard != null) {
                    out.println("TOP_CARD " + topCard.toString());
                }

                List<String> players = currentGame.getPlayers();
                StringBuilder playerInfo = new StringBuilder("PLAYERS ");
                for (String p : players) {
                    if (!p.equals(nickname)) {
                        int handSize = currentGame.getHandForPlayer(p).size();
                        playerInfo.append(p).append(":").append(handSize);
                        if (!p.equals(players.get(players.size() - 1))) {
                            playerInfo.append(",");
                        }
                    }
                }
                out.println(playerInfo.toString());

                out.println("TURN " + currentGame.getCurrentPlayer());
            }
        }

        /**
         * Wysyła zaktualizowane ręce wszystkich graczy w pokoju.
         */
        private void updateAllPlayerHandsInRoom() {
            GameRoom room = gameRooms.get(currentRoomId);
            if (room == null || !room.isGameInProgress()) {
                return;
            }

            Game currentGame = room.getGame();
            if (currentGame == null) return;

            for (String player : currentGame.getPlayers()) {
                ClientHandler client = connectedClients.get(player);
                if (client != null) {
                    List<Cart> hand = currentGame.getHandForPlayer(player);
                    StringBuilder handStr = new StringBuilder("HAND ");
                    for (int i = 0; i < hand.size(); i++) {
                        handStr.append(hand.get(i).toString());
                        if (i < hand.size() - 1) handStr.append(",");
                    }
                    client.out.println(handStr.toString());

                    StringBuilder playerInfo = new StringBuilder("PLAYERS ");
                    List<String> players = currentGame.getPlayers();
                    for (String p : players) {
                        if (!p.equals(player)) {
                            int handSize = currentGame.getHandForPlayer(p).size();
                            playerInfo.append(p).append(":").append(handSize);
                            if (!p.equals(players.get(players.size() - 1))) {
                                playerInfo.append(",");
                            }
                        }
                    }
                    client.out.println(playerInfo.toString());
                }
            }
        }

        /**
         * Wysyła listę użytkowników w pokoju do klienta.
         */
        private void broadcastUserList() {
            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }
            broadcastUserListToRoom(currentRoomId);
        }

        /**
         * Wysyła listę użytkowników w pokoju do wszystkich graczy w tym pokoju.
         *
         * @param roomId identyfikator pokoju
         */
        private void broadcastUserListToRoom(int roomId) {
            GameRoom room = gameRooms.get(roomId);
            if (room == null) return;

            StringBuilder userList = new StringBuilder("USERLIST ");
            for (String user : room.getPlayers()) {
                userList.append(user).append(":").append(clientStatus.getOrDefault(user, "NOT_READY")).append(",");
            }
            broadcastToRoom(roomId, userList.toString());
        }

        /**
         * Wysyła wiadomość do wszystkich graczy w pokoju.
         *
         * @param roomId identyfikator pokoju
         * @param message wiadomość do wysłania
         */
        private void broadcastToRoom(int roomId, String message) {
            GameRoom room = gameRooms.get(roomId);
            if (room == null) return;

            for (String player : room.getPlayers()) {
                ClientHandler client = connectedClients.get(player);
                if (client != null && client.out != null) {
                    client.out.println(message);
                }
            }
        }

        /**
         * Czyści zasoby po rozłączeniu klienta.
         * Usuwa gracza z pokoju, map i zamyka połączenie.
         */
        private void cleanup() {
            if (nickname != null) {
                if (currentRoomId != -1) {
                    GameRoom room = gameRooms.get(currentRoomId);
                    if (room != null) {
                        room.removePlayer(nickname);
                        broadcastToRoom(currentRoomId, "USER_LEFT " + nickname);
                        if (room.isEmpty()) {
                            gameRooms.remove(currentRoomId);
                            logger.info("Room " + currentRoomId + " removed (empty after cleanup)");
                        } else {
                            broadcastUserListToRoom(currentRoomId);
                        }
                    }
                }

                connectedClients.remove(nickname);
                clientStatus.remove(nickname);
                logger.info("Client disconnected: " + nickname);
            }
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                logger.debug("Client socket closed");
            } catch (IOException e) {
                logger.error(e, "Error closing socket");
            }
        }
    }
}