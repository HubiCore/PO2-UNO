package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UnoServer extends Thread {
    private Connection conn;
    private DataBase db;
    private static Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private static Map<String, String> clientStatus = new ConcurrentHashMap<>(); // "READY", "NOT_READY"
    private static Map<Integer, GameRoom> gameRooms = new ConcurrentHashMap<>(); // Nowa mapa pokoi
    private static int nextRoomId = 1;

    public void run() {
        db = new DataBase();
        conn = db.connect("src/main/resources/baza.sql");

        if (conn == null) {
            System.out.println("Nie udało się połączyć z bazą danych");
            return;
        }

        int portNumber = 2137;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server is listening on port " + portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                new ClientHandler(clientSocket, conn, db).start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + portNumber);
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    System.out.println("Połączenie z bazą danych zamknięte");
                }
            } catch (SQLException e) {
                System.out.println("Błąd przy zamykaniu połączenia z bazą: " + e.getMessage());
            }
        }
    }

    // Klasa reprezentująca pokój gry
    static class GameRoom {
        private int roomId;
        private List<String> players;
        private Game currentGame;
        private boolean gameInProgress;
        private String roomStatus; // "WAITING", "FULL", "IN_PROGRESS"

        public GameRoom(int roomId) {
            this.roomId = roomId;
            this.players = new ArrayList<>();
            this.gameInProgress = false;
            this.roomStatus = "WAITING";
        }

        public boolean addPlayer(String player) {
            if (players.size() >= 4 || gameInProgress) {
                return false;
            }
            players.add(player);
            if (players.size() == 4) {
                roomStatus = "FULL";
            }
            return true;
        }

        public boolean removePlayer(String player) {
            boolean removed = players.remove(player);
            if (removed) {
                if (players.size() < 4 && !gameInProgress) {
                    roomStatus = "WAITING";
                }
            }
            return removed;
        }

        public void startGame() {
            if (players.size() >= 2 && players.size() <= 4 && !gameInProgress) {
                currentGame = new Game(new ArrayList<>(players));
                gameInProgress = true;
                roomStatus = "IN_PROGRESS";
            }
        }

        public void endGame() {
            currentGame = null;
            gameInProgress = false;
            roomStatus = "WAITING";
        }

        // Gettery i inne metody
        public int getRoomId() { return roomId; }
        public List<String> getPlayers() { return players; }
        public Game getGame() { return currentGame; }
        public boolean isGameInProgress() { return gameInProgress; }
        public String getRoomStatus() { return roomStatus; }
        public boolean isFull() { return players.size() >= 4; }
        public boolean isEmpty() { return players.isEmpty(); }
    }

    // Metody do zarządzania pokojami
    private static synchronized GameRoom createNewRoom() {
        int roomId = nextRoomId++;
        GameRoom room = new GameRoom(roomId);
        gameRooms.put(roomId, room);
        System.out.println("Utworzono nowy pokój: " + roomId);
        return room;
    }

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

    // Pełna klasa Game z wszystkimi metodami
    static class Game {
        private List<String> players;
        private Map<String, List<Cart>> hands;
        private List<Cart> deck;
        private List<Cart> discardPile;
        private int currentPlayerIndex;
        private boolean direction; // true = clockwise
        private String currentColor;
        private String currentValue;
        private boolean waitingForWildColor = false;

        public Game(List<String> players) {
            this.players = new ArrayList<>(players);
            this.hands = new HashMap<>();
            this.deck = new ArrayList<>();
            this.discardPile = new ArrayList<>();
            this.currentPlayerIndex = 0;
            this.direction = true;
            this.waitingForWildColor = false;

            initDeck();
            shuffleDeck();
            dealCards();

            Cart firstCard = drawFromDeck();
            discardPile.add(firstCard);
            currentColor = firstCard.getKolor();
            currentValue = firstCard.getWartosc();

            while (firstCard.getWartosc().equals("W") ||
                    firstCard.getWartosc().equals("W4") ||
                    firstCard.getWartosc().equals("+2") ||
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

            // Dodaj karty Wild
            for (int i = 0; i < 4; i++) {
                deck.add(new Cart("WILD", "W"));
                deck.add(new Cart("WILD", "W4"));
            }
        }

        private void shuffleDeck() {
            Collections.shuffle(deck);
        }

        private void dealCards() {
            for (String player : players) {
                List<Cart> hand = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    hand.add(drawFromDeck());
                }
                hands.put(player, hand);
            }
        }

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

        public List<Cart> getHandForPlayer(String player) {
            return hands.get(player);
        }

        public Cart getTopCard() {
            return discardPile.isEmpty() ? null : discardPile.get(discardPile.size() - 1);
        }

        public String getCurrentPlayer() {
            return players.get(currentPlayerIndex);
        }

        public boolean playCard(String player, Cart card) {
            System.out.println("Game.playCard: gracz " + player + " próbuje zagrać " + card);

            if (!player.equals(players.get(currentPlayerIndex))) {
                System.out.println("To nie jest tura gracza " + player);
                return false;
            }

            Cart topCard = getTopCard();
            if (canPlayOn(card, topCard)) {
                // Znajdź i usuń konkretną kartę z ręki gracza
                List<Cart> playerHand = hands.get(player);
                boolean removed = false;

                // Debug: wypisz zawartość ręki przed usunięciem
                System.out.println("Ręka przed usunięciem (" + player + "):");
                for (Cart c : playerHand) {
                    System.out.println("  " + c.toString());
                }

                // Znajdź kartę do usunięcia (porównując stringi, bo mogą być różne obiekty)
                for (int i = 0; i < playerHand.size(); i++) {
                    Cart c = playerHand.get(i);
                    if (c.toString().equals(card.toString())) {
                        playerHand.remove(i);
                        removed = true;
                        System.out.println("Usunięto kartę: " + card.toString() + " z pozycji " + i);
                        break;
                    }
                }

                if (!removed) {
                    System.out.println("UWAGA: Nie znaleziono karty do usunięcia!");
                    return false;
                }

                // Debug: wypisz zawartość ręki po usunięciu
                System.out.println("Ręka po usunięciu (" + player + "):");
                for (Cart c : playerHand) {
                    System.out.println("  " + c.toString());
                }

                discardPile.add(card);
                currentColor = card.getKolor();
                currentValue = card.getWartosc();

                handleSpecialCard(card);

                nextPlayer();
                return true;
            }
            return false;
        }

        private boolean canPlayOn(Cart card, Cart topCard) {
            if (card.getKolor().equals("WILD")) {
                return true;
            }

            return card.getKolor().equals(currentColor) ||
                    card.getWartosc().equals(currentValue);
        }

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
                case "W":
                    waitingForWildColor = true;
                    break;
                case "W4":
                    waitingForWildColor = true;
                    nextPlayer();
                    String nextPlayer2 = getCurrentPlayer();
                    for (int i = 0; i < 4; i++) {
                        Cart drawnCard = drawFromDeck();
                        if (drawnCard != null) {
                            hands.get(nextPlayer2).add(drawnCard);
                        }
                    }
                    nextPlayer();
                    break;
            }
        }

        private void nextPlayer() {
            if (direction) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } else {
                currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
            }
        }

        public Cart drawCardForPlayer(String player) {
            Cart card = drawFromDeck();
            if (card != null) {
                hands.get(player).add(card);
            }
            return card;
        }

        public boolean hasPlayerWon(String player) {
            return hands.get(player).isEmpty();
        }

        public void setWildColor(String color) {
            currentColor = color;
            waitingForWildColor = false;
        }

        public boolean isWaitingForWildColor() {
            return waitingForWildColor;
        }

        public List<String> getPlayers() {
            return players;
        }

        public Map<String, List<Cart>> getHands() {
            return hands;
        }
    }

    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private Connection conn;
        private DataBase db;
        private PrintWriter out;
        private BufferedReader in;
        private String nickname;
        private int currentRoomId = -1; // ID pokoju, w którym znajduje się gracz

        public ClientHandler(Socket socket, Connection conn, DataBase db) {
            this.clientSocket = socket;
            this.conn = conn;
            this.db = db;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client " + (nickname != null ? nickname : "unknown") + ": " + inputLine);
                    handleInput(inputLine);
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleInput(String inputLine) {
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
                System.out.println("Otrzymano inicjalizację gry");
                initialize_game();
            } else if ("TOP5".equalsIgnoreCase(inputLine)) {
                String top5 = db.Top5_Best(conn);
                System.out.println(top5);
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
            } else if (inputLine.startsWith("WILD_COLOR ")) {
                if (nickname == null) {
                    out.println("ERROR_NOT_LOGGED_IN");
                    return;
                }
                handleWildColor(inputLine.substring(11));
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
        }

        private void handleLogin(String loginData) {
            System.out.println("=== SERWER: ROZPOCZĘCIE LOGOWANIA ===");
            String[] parts = loginData.split(":");
            if (parts.length != 2) {
                System.out.println("Serwer: Nieprawidłowy format logowania: " + loginData);
                out.println("LOGIN_ERROR Invalid format. Use: LOGIN username:password_hash");
                return;
            }

            String username = parts[0];
            String passwordHash = parts[1];

            System.out.println("Serwer: Próba logowania dla użytkownika: " + username);

            if (connectedClients.containsKey(username)) {
                System.out.println("Serwer: Użytkownik już połączony: " + username);
                out.println("LOGIN_ERROR User already connected");
                return;
            }

            boolean userExists = db.is_player(conn, username);
            System.out.println("Serwer: Użytkownik istnieje w bazie: " + userExists);

            if (!userExists) {
                System.out.println("Serwer: Tworzenie nowego użytkownika: " + username);
                boolean created = db.createUserIfNotExists(conn, username, passwordHash);
                if (!created) {
                    System.out.println("Serwer: Nie udało się utworzyć użytkownika");
                    out.println("LOGIN_ERROR Failed to create user");
                    return;
                }
                System.out.println("Serwer: Utworzono nowego użytkownika: " + username);
                loginUser(username);
                return;
            }

            String storedPasswordHash = db.getPasswordHash(conn, username);

            if (storedPasswordHash == null) {
                System.out.println("Serwer: Nie znaleziono użytkownika w bazie");
                out.println("LOGIN_ERROR User not found in database");
                return;
            }

            if (!storedPasswordHash.equals(passwordHash)) {
                System.out.println("Serwer: Błędne hasło dla użytkownika: " + username);
                out.println("LOGIN_ERROR Invalid password");
                return;
            }

            System.out.println("Serwer: Hasło poprawne, logowanie użytkownika: " + username);
            loginUser(username);
        }

        private void loginUser(String username) {
            System.out.println("Serwer: Logowanie użytkownika: " + username);
            this.nickname = username;
            connectedClients.put(username, this);
            clientStatus.put(username, "NOT_READY");

            System.out.println("Serwer: Wysyłam LOGIN_SUCCESS do: " + username);
            out.println("LOGIN_SUCCESS " + username);

            // Automatyczne przypisanie do dostępnego pokoju
            GameRoom room = findAvailableRoomForPlayer(username);
            currentRoomId = room.getRoomId();
            if (room.addPlayer(username)) {
                out.println("ROOM_ASSIGNED " + currentRoomId);
                broadcastToRoom(currentRoomId, "USER_JOINED " + username);
                broadcastUserListToRoom(currentRoomId);
            }

            System.out.println("Serwer: Login zakończony pomyślnie dla: " + username);
        }

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

        private void createRoom() {
            GameRoom newRoom = createNewRoom();
            currentRoomId = newRoom.getRoomId();
            newRoom.addPlayer(nickname);
            out.println("ROOM_CREATED " + currentRoomId);
            broadcastUserListToRoom(currentRoomId);
        }

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

            // Sprawdź czy wszyscy w pokoju są gotowi
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
                broadcastToRoom(currentRoomId, "START_GAME");
                room.startGame();
            }
        }

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
        }

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
                    } else {
                        broadcastUserListToRoom(currentRoomId);
                    }
                }
            }

            connectedClients.remove(nick);
            clientStatus.remove(nick);
        }

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
                System.out.println("Próba zagrania karty: " + cardStr + " przez gracza: " + nickname);
                Cart card = Cart.fromString(cardStr);

                boolean success = currentGame.playCard(nickname, card);

                if (success) {
                    System.out.println("Karta zagrana pomyślnie przez: " + nickname);

                    Cart topCard = currentGame.getTopCard();
                    String currentPlayer = currentGame.getCurrentPlayer();

                    // Sprawdź czy ktoś wygrał
                    if (currentGame.hasPlayerWon(nickname)) {
                        System.out.println("Gracz " + nickname + " wygrał grę!");
                        db.increaseWins(conn, nickname);

                        // Wyślij informację o zwycięzcy do wszystkich graczy w pokoju
                        broadcastToRoom(currentRoomId, "WINNER " + nickname);

                        // Zakończ grę w pokoju
                        room.endGame();
                        return;
                    }

                    // Budujemy wiadomość dla KAŻDEGO gracza INDYWIDUALNIE
                    for (String player : currentGame.getPlayers()) {
                        ClientHandler client = connectedClients.get(player);
                        if (client != null) {
                            // Lista przeciwników z liczbą kart
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

                            // Ręka BIEŻĄCEGO gracza
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

                            System.out.println("Wysyłam do gracza " + player + ": " + message);
                            client.out.println(message);
                        }
                    }

                    // Jeśli trzeba wybrać kolor dla WILD
                    if (currentGame.isWaitingForWildColor() && nickname.equals(currentGame.getCurrentPlayer())) {
                        out.println("CHOOSE_COLOR");
                    }

                } else {
                    out.println("ERROR Nie można zagrać tej karty");
                    out.println("TURN " + currentGame.getCurrentPlayer());
                }
            } catch (Exception e) {
                System.out.println("Błąd w handlePlay: " + e.getMessage());
                out.println("ERROR Nieprawidłowy format karty");
                out.println("TURN " + currentGame.getCurrentPlayer());
            }
        }

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
                out.println("ERROR Not your turn");
                return;
            }

            Cart drawnCard = currentGame.drawCardForPlayer(nickname);
            if (drawnCard != null) {
                out.println("DREW " + drawnCard.toString());

                // Wyślij zaktualizowaną rękę do gracza, który dobrał kartę
                List<Cart> hand = currentGame.getHandForPlayer(nickname);
                StringBuilder handStr = new StringBuilder("HAND ");
                for (int i = 0; i < hand.size(); i++) {
                    handStr.append(hand.get(i).toString());
                    if (i < hand.size() - 1) handStr.append(",");
                }
                out.println(handStr.toString());

                // Przejdź do następnego gracza
                currentGame.nextPlayer();

                // Powiadom wszystkich graczy w pokoju o zmianie tury
                broadcastToRoom(currentRoomId, "TURN " + currentGame.getCurrentPlayer());

                // Wyślij zaktualizowane informacje o rękach wszystkich graczy w pokoju
                updateAllPlayerHandsInRoom();
            } else {
                out.println("ERROR No cards to draw");
            }
        }

        private void handleWildColor(String color) {
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

            currentGame.setWildColor(color.toUpperCase());
            broadcastToRoom(currentRoomId, "WILD_COLOR " + color.toUpperCase());
            broadcastToRoom(currentRoomId, "TURN " + currentGame.getCurrentPlayer());
            updateAllPlayerHandsInRoom();
        }

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

        private void broadcastUserList() {
            if (currentRoomId == -1) {
                out.println("ERROR Not in a room");
                return;
            }
            broadcastUserListToRoom(currentRoomId);
        }

        private void broadcastUserListToRoom(int roomId) {
            GameRoom room = gameRooms.get(roomId);
            if (room == null) return;

            StringBuilder userList = new StringBuilder("USERLIST ");
            for (String user : room.getPlayers()) {
                userList.append(user).append(":").append(clientStatus.getOrDefault(user, "NOT_READY")).append(",");
            }
            broadcastToRoom(roomId, userList.toString());
        }

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

        private void cleanup() {
            if (nickname != null) {
                // Usuń gracza z pokoju
                if (currentRoomId != -1) {
                    GameRoom room = gameRooms.get(currentRoomId);
                    if (room != null) {
                        room.removePlayer(nickname);
                        broadcastToRoom(currentRoomId, "USER_LEFT " + nickname);
                        if (room.isEmpty()) {
                            gameRooms.remove(currentRoomId);
                        } else {
                            broadcastUserListToRoom(currentRoomId);
                        }
                    }
                }

                connectedClients.remove(nickname);
                clientStatus.remove(nickname);
            }
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("Connection with client closed");
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}