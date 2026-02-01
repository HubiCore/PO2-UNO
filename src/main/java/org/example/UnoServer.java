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
    private static Game currentGame;
    private static boolean gameInProgress = false;

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

            gameInProgress = true;
        }

        private void initDeck() {
            String[] colors = {"RED", "GREEN", "BLUE", "YELLOW"};
            // Zmieniamy wartości na skrócone
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
                case "W4":
                    nextPlayer();
                    String nextPlayerWild = getCurrentPlayer();
                    for (int i = 0; i < 4; i++) {
                        Cart drawnCard = drawFromDeck();
                        if (drawnCard != null) {
                            hands.get(nextPlayerWild).add(drawnCard);
                        }
                    }
                    nextPlayer();
                    waitingForWildColor = true;
                    break;
                case "W":
                    waitingForWildColor = true;
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
                // Pozwalamy na TOP5 bez logowania
                String top5 = db.Top5_Best(conn);
                System.out.println(top5);
                out.println("TOP5 " + top5);
            } else if ("LIST".equalsIgnoreCase(inputLine)) {
                if (nickname == null) {
                    out.println("ERROR_NOT_LOGGED_IN");
                    return;
                }
                broadcastUserList();
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

            if (gameInProgress) {
                System.out.println("Serwer: Gra w trakcie, odmowa logowania");
                out.println("LOGIN_ERROR Game in progress");
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

            // WAŻNE: Czekamy chwilę przed wysłaniem broadcastu
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            broadcastUserList();
            broadcastMessage("USER_JOINED " + username);

            System.out.println("Serwer: Login zakończony pomyślnie dla: " + username);
        }

        private void handleReady(String nick) {
            if (!nick.equals(nickname)) {
                out.println("ERROR Invalid nickname");
                return;
            }

            if (gameInProgress) {
                out.println("ERROR_GAME_IN_PROGRESS");
                return;
            }

            clientStatus.put(nick, "READY");
            broadcastMessage("READY " + nick);

            boolean allReady = true;
            int readyCount = 0;
            for (String status : clientStatus.values()) {
                if ("READY".equals(status)) {
                    readyCount++;
                } else {
                    allReady = false;
                }
            }

            if (allReady && readyCount >= 2 && readyCount <= 4) {
                broadcastMessage("START_GAME");
            }
        }

        private void handleUnready(String nick) {
            if (!nick.equals(nickname)) {
                out.println("ERROR Invalid nickname");
                return;
            }

            clientStatus.put(nick, "NOT_READY");
            broadcastMessage("UNREADY " + nick);
        }

        private void handleExit(String nick) {
            if (!nick.equals(nickname)) {
                out.println("ERROR Invalid nickname");
                return;
            }

            connectedClients.remove(nick);
            clientStatus.remove(nick);
            broadcastUserList();
            broadcastMessage("USER_LEFT " + nick);
        }

        private void initialize_game() {
            List<String> players = new ArrayList<>(connectedClients.keySet());
            currentGame = new Game(players);
            for (String player : players) {
                ClientHandler client = connectedClients.get(player);
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

        private void handlePlay(String cardStr) {
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

                        // Wyślij informację o zwycięzcy do wszystkich graczy
                        broadcastMessage("WINNER " + nickname);

                        // Zakończ grę
                        endGame();
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

                // Powiadom wszystkich graczy o zmianie tury
                broadcastMessage("TURN " + currentGame.getCurrentPlayer());

                // Wyślij zaktualizowane informacje o rękach wszystkich graczy
                updateAllPlayerHands();
            } else {
                out.println("ERROR No cards to draw");
            }
        }

        private void handleWildColor(String color) {
            if (currentGame == null || nickname == null) {
                out.println("ERROR No game in progress");
                return;
            }

            currentGame.setWildColor(color.toUpperCase());
            broadcastMessage("WILD_COLOR " + color.toUpperCase());
            broadcastMessage("TURN " + currentGame.getCurrentPlayer());
            updateAllPlayerHands();
        }

        private void sendGameState() {
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

        private void updateAllPlayerHands() {
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

        private void endGame() {
            currentGame = null;
            gameInProgress = false;
            for (String player : clientStatus.keySet()) {
                clientStatus.put(player, "NOT_READY");
            }
            broadcastMessage("GAME_ENDED");
        }

        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("USERLIST ");
            for (String user : connectedClients.keySet()) {
                userList.append(user).append(":").append(clientStatus.get(user)).append(",");
            }
            broadcastMessage(userList.toString());
        }

        private void broadcastMessage(String message) {
            for (ClientHandler client : connectedClients.values()) {
                if (client != this && client.out != null) {
                    client.out.println(message);
                }
            }
            if (out != null) {
                out.println(message);
            }
        }

        private void cleanup() {
            if (nickname != null) {
                connectedClients.remove(nickname);
                clientStatus.remove(nickname);
                broadcastUserList();
                broadcastMessage("USER_LEFT " + nickname);
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