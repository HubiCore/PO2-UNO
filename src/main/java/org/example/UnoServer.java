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
    private static List<String> readyUsers = Collections.synchronizedList(new ArrayList<>());

    public void run() {
        //łączenie z bazą danych
        db = new DataBase();
        conn = db.connect("/home/Hubi_Core/IdeaProjects/proba2_backend/src/main/resources/baza.sql");

        if (conn == null) {
            System.out.println("Nie udało się połączyć z bazą danych");
            System.out.println("Sprawdź uprawnienia do zapisu w katalogu: " + System.getProperty("user.dir"));
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

    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private Connection conn;
        private DataBase db;
        private PrintWriter out;
        private String nickname;

        public ClientHandler(Socket socket, Connection conn, DataBase db) {
            this.clientSocket = socket;
            this.conn = conn;
            this.db = db;
        }
        public void initialize_game(){

        }
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client: " + inputLine);
                    if (inputLine.startsWith("JOIN ")) {
                        handleJoin(inputLine.substring(5));
                    } else if (inputLine.startsWith("READY ")) {
                        handleReady(inputLine.substring(6));
                    } else if (inputLine.startsWith("UNREADY ")) {
                        handleUnready(inputLine.substring(8));
                    } else if (inputLine.startsWith("EXIT ")) {
                        handleExit(inputLine.substring(5));
                        break;
                    } else if ("TOP5".equalsIgnoreCase(inputLine)) {
                        String top5 = db.Top5_Best(conn);
                        out.println("TOP5 " + top5);
                    } else if ("LIST".equalsIgnoreCase(inputLine)) {
                        broadcastUserList();
                    } else if ("quit".equalsIgnoreCase(inputLine)) {
                        out.println("Bye bye (mogging)!");
                        break;
                    } else {
                        out.println("Server received: " + inputLine);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleJoin(String nick) {
            if (connectedClients.containsKey(nick)) {
                out.println("ERROR_TAKEN");
                return;
            }
            db.Insert_User(conn, nick);
            this.nickname = nick;
            connectedClients.put(nick, this);
            broadcastUserList();
            broadcastMessage("USER_JOINED " + nick);
        }

        private void handleReady(String nick) {
            if (!readyUsers.contains(nick)) {
                readyUsers.add(nick);
                broadcastMessage("READY " + nick);

                if (readyUsers.size() == connectedClients.size() && connectedClients.size() >= 2) {
                    broadcastMessage("START_GAME");
                    System.out.println("Gra się zaczyna");
                    initialize_game();
                }
            }
        }

        private void handleUnready(String nick) {
            readyUsers.remove(nick);
            broadcastMessage("UNREADY " + nick);
        }

        private void handleExit(String nick) {
            connectedClients.remove(nick);
            readyUsers.remove(nick);
            broadcastUserList();
            broadcastMessage("USER_LEFT " + nick);
        }

        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("USERLIST ");
            for (String user : connectedClients.keySet()) {
                userList.append(user).append(",");
            }
            broadcastMessage(userList.toString());
        }

        private void broadcastMessage(String message) {
            for (ClientHandler client : connectedClients.values()) {
                if (client != this) {
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
                readyUsers.remove(nickname);
                broadcastUserList();
                broadcastMessage("USER_LEFT " + nickname);
            }
            try {
                clientSocket.close();
                System.out.println("Connection with client closed");
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}