package org.example;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

public class UnoServer extends Thread {
    private Connection conn;
    private DataBase db;

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
    //obsługa klientów
    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private Connection conn;
        private DataBase db;

        public ClientHandler(Socket socket, Connection conn, DataBase db) {
            this.clientSocket = socket;
            this.conn = conn;
            this.db = db;
        }

        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client: " + inputLine);

                    if ("TOP5".equalsIgnoreCase(inputLine)) {
                        String top5 = db.Top5_Best(conn);
                        out.println(top5);
                    }
                    else if ("quit".equalsIgnoreCase(inputLine)) {
                        out.println("Goodbye!");
                        break;
                    }
                    else {
                        out.println("Server received: " + inputLine);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Connection with client closed");
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}