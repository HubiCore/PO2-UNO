package org.example;

import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String serverAddress = "localhost";
    private int serverPort = 2137;

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

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Błąd przy zamykaniu połączenia: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}