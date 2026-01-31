package org.example;

import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String serverAddress = "localhost";
    private int serverPort = 2137;
    private boolean authenticated = false;

    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Połączono z serwerem");
            return true;
        } catch (IOException e) {
            System.out.println("Błąd połączenia z serwerem: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String receiveMessage() throws IOException {
        if (in != null) {
            return in.readLine();
        }
        return null;
    }

    // Metoda do logowania z oczekiwaniem na odpowiedź
    public boolean login(String username, String password) throws IOException {
        if (!isConnected()) {
            return false;
        }

        // Wyślij dane logowania
        sendMessage("LOGIN " + username + ":" + password);

        // Czekaj na odpowiedź od serwera
        String response = receiveMessage();

        if (response != null && response.equals("LOGIN_SUCCESS")) {
            authenticated = true;
            return true;
        }

        return false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            authenticated = false;
        } catch (IOException e) {
            System.out.println("Błąd przy zamykaniu połączenia: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}