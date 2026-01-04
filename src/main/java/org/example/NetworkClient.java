package org.example;

import java.io.*;
import java.net.Socket;

public class NetworkClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 2137;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public boolean connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Połączono z serwerem: " + SERVER_ADDRESS + ":" + SERVER_PORT);
            return true;
        } catch (IOException e) {
            System.err.println("Błąd połączenia z serwerem: " + e.getMessage());
            return false;
        }
    }

    public String sendMessage(String message) {
        if (out == null || in == null) {
            return "Brak połączenia z serwerem";
        }

        try {
            out.println(message);

            // Odczytaj odpowiedź serwera (do znaku nowej linii)
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                if (!in.ready()) { // Jeśli nie ma więcej danych do odczytu
                    break;
                }
            }

            return response.toString().trim();
        } catch (IOException e) {
            System.err.println("Błąd komunikacji z serwerem: " + e.getMessage());
            return "Błąd: " + e.getMessage();
        }
    }

    public void disconnect() {
        try {
            if (out != null) {
                out.println("quit");
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Rozłączono z serwerem");
        } catch (IOException e) {
            System.err.println("Błąd przy rozłączaniu: " + e.getMessage());
        }
    }
}