package org.example;

import java.io.*;
import java.net.*;

public class ClientConnection implements AutoCloseable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;
    private String host = "localhost";
    private int port = 2137;
    private boolean debug = true; // Włącz/Wyłącz logowanie debug

    // Podstawowy konstruktor
    public ClientConnection() {
    }

    // Konstruktor z możliwością ustawienia hosta i portu
    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Nawiązuje połączenie z serwerem
     * @return true jeśli połączenie zostało nawiązane, false w przeciwnym razie
     */
    public boolean connect() {
        try {
            if (connected) {
                disconnect();
            }

            log("Próbuję połączyć się z " + host + ":" + port);
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000); // Timeout połączenia 5 sekund
            socket.setSoTimeout(10000); // Timeout odczytu 10 sekund

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            connected = true;
            log("Połączono pomyślnie z " + host + ":" + port);
            return true;

        } catch (UnknownHostException e) {
            logError("Nieznany host: " + host);
            return false;
        } catch (SocketTimeoutException e) {
            logError("Timeout połączenia z serwerem");
            return false;
        } catch (IOException e) {
            logError("Błąd połączenia: " + e.getMessage());
            if (debug) e.printStackTrace();
            return false;
        }
    }

    /**
     * Wysyła wiadomość do serwera
     * @param message Wiadomość do wysłania
     * @return true jeśli wysłano pomyślnie, false w przeciwnym razie
     */
    public boolean sendMessage(String message) {
        if (!connected) {
            logError("Nie można wysłać - brak połączenia");
            return false;
        }

        if (writer == null) {
            logError("Writer jest null");
            return false;
        }

        try {
            writer.println(message);
            writer.flush();
            log("Wysłano wiadomość: " + message);
            return true;
        } catch (Exception e) {
            logError("Błąd wysyłania wiadomości: " + e.getMessage());
            if (debug) e.printStackTrace();
            return false;
        }
    }

    /**
     * Odbiera jedną wiadomość od serwera (blokująco)
     * @return Odebrana wiadomość lub null w przypadku błędu/timeoutu
     */
    public String receiveMessage() {
        if (!connected) {
            logError("Nie można odebrać - brak połączenia");
            return null;
        }

        if (reader == null) {
            logError("Reader jest null");
            return null;
        }

        try {
            log("Oczekuję na wiadomość...");
            String response = reader.readLine();
            log("Odebrano wiadomość: " + response);
            return response;
        } catch (SocketTimeoutException e) {
            logError("Timeout oczekiwania na odpowiedź");
            return null;
        } catch (IOException e) {
            logError("Błąd odbierania wiadomości: " + e.getMessage());
            if (debug) e.printStackTrace();
            disconnect();
            return null;
        }
    }

    /**
     * Odbiera wiadomość z określonym timeoutem
     * @param timeoutMs Timeout w milisekundach
     * @return Odebrana wiadomość lub null
     */
    public String receiveMessageWithTimeout(int timeoutMs) {
        if (!connected || socket == null) {
            logError("Nie można odebrać - brak połączenia lub socket jest null");
            return null;
        }

        try {
            log("Ustawiam timeout na " + timeoutMs + "ms");
            int originalTimeout = socket.getSoTimeout();
            socket.setSoTimeout(timeoutMs);
            String response = receiveMessage();
            socket.setSoTimeout(originalTimeout);
            return response;
        } catch (SocketException e) {
            logError("Błąd ustawiania timeoutu: " + e.getMessage());
            if (debug) e.printStackTrace();
            return null;
        }
    }

    /**
     * Wysyła wiadomość i czeka na odpowiedź
     * @param message Wiadomość do wysłania
     * @return Odpowiedź serwera lub null
     */
    public String sendAndReceive(String message) {
        log("Wysyłam i oczekuję odpowiedzi...");
        if (sendMessage(message)) {
            return receiveMessage();
        }
        return null;
    }

    /**
     * Wysyła wiadomość i czeka na odpowiedź z timeoutem
     * @param message Wiadomość do wysłania
     * @param timeoutMs Timeout w milisekundach
     * @return Odpowiedź serwera lub null
     */
    public String sendAndReceiveWithTimeout(String message, int timeoutMs) {
        log("Wysyłam i oczekuję odpowiedzi z timeoutem " + timeoutMs + "ms...");
        if (sendMessage(message)) {
            return receiveMessageWithTimeout(timeoutMs);
        }
        return null;
    }

    /**
     * Wysyła wiadomość i czeka na odpowiedź o określonym prefiksie
     * @param message Wiadomość do wysłania
     * @param expectedPrefix Prefix oczekiwanej odpowiedzi (np. "LOGIN_")
     * @param timeoutMs Timeout w milisekundach
     * @return Odpowiedź serwera lub null
     */
    public String sendAndWaitForResponse(String message, String expectedPrefix, int timeoutMs) {
        if (!sendMessage(message)) {
            return null;
        }

        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < timeoutMs) {
            int remainingTime = (int)(timeoutMs - (System.currentTimeMillis() - startTime));
            if (remainingTime <= 0) {
                logError("Timeout oczekiwania na odpowiedź z prefiksem: " + expectedPrefix);
                return null;
            }

            String response = receiveMessageWithTimeout(remainingTime);
            if (response == null) {
                continue;
            }

            if (response.startsWith(expectedPrefix)) {
                log("Znaleziono oczekiwaną odpowiedź: " + response);
                return response;
            } else {
                log("Pomijam nieoczekiwaną odpowiedź: " + response);
            }
        }

        logError("Nie znaleziono odpowiedzi z prefiksem: " + expectedPrefix);
        return null;
    }

    /**
     * Czyści bufor wejściowy (odczytuje wszystkie dostępne wiadomości)
     */
    public void clearInputBuffer() {
        if (!connected || reader == null) {
            return;
        }

        try {
            // Ustaw bardzo krótki timeout na czyszczenie bufora
            int originalTimeout = socket.getSoTimeout();
            socket.setSoTimeout(100);

            int messagesCleared = 0;
            while (true) {
                String message = reader.readLine();
                if (message == null) break;
                log("Czyszczenie bufora - pomijam: " + message);
                messagesCleared++;
            }

            // Przywróć normalny timeout
            socket.setSoTimeout(originalTimeout);

            if (messagesCleared > 0) {
                log("Wyczyszczono " + messagesCleared + " wiadomości z bufora");
            }

        } catch (SocketTimeoutException e) {
            // To normalne - oznacza, że nie ma więcej wiadomości
        } catch (IOException e) {
            logError("Błąd czyszczenia bufora: " + e.getMessage());
            if (debug) e.printStackTrace();
        }
    }

    /**
     * Sprawdza czy połączenie jest aktywne
     * @return true jeśli połączony, false w przeciwnym razie
     */
    public boolean isConnected() {
        boolean isConnected = connected && socket != null && !socket.isClosed() && socket.isConnected();
        log("Sprawdzam połączenie - wynik: " + isConnected);
        return isConnected;
    }

    /**
     * Zamyka połączenie z serwerem
     */
    public void disconnect() {
        log("Rozłączam...");
        connected = false;

        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
            log("Rozłączono pomyślnie");
        } catch (IOException e) {
            logError("Błąd podczas zamykania połączenia: " + e.getMessage());
            if (debug) e.printStackTrace();
        }
    }

    /**
     * Implementacja AutoCloseable - do używania w try-with-resources
     */
    @Override
    public void close() {
        disconnect();
    }

    /**
     * Ustawia hosta serwera
     * @param host Adres serwera
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Ustawia port serwera
     * @param port Port serwera
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Ustawia host i port jednocześnie
     * @param host Adres serwera
     * @param port Port serwera
     */
    public void setConnectionParams(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Włącza lub wyłącza logowanie debug
     * @param debug true aby włączyć, false aby wyłączyć
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Pobiera aktualny host
     * @return Adres serwera
     */
    public String getHost() {
        return host;
    }

    /**
     * Pobiera aktualny port
     * @return Port serwera
     */
    public int getPort() {
        return port;
    }

    /**
     * Sprawdza czy debug jest włączony
     * @return true jeśli debug włączony, false w przeciwnym razie
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Pomocnicza metoda do logowania
     * @param message Wiadomość do zalogowania
     */
    private void log(String message) {
        if (debug) {
            System.out.println("ClientConnection: " + message);
        }
    }

    /**
     * Pomocnicza metoda do logowania błędów
     * @param message Wiadomość o błędzie
     */
    private void logError(String message) {
        System.err.println("ClientConnection ERROR: " + message);
    }

    /**
     * Testuje połączenie z serwerem pingiem
     * @return true jeśli serwer odpowiada, false w przeciwnym razie
     */
    public boolean testConnection() {
        if (!isConnected()) {
            return false;
        }

        try {
            String originalResponse = sendAndReceiveWithTimeout("PING", 3000);
            return originalResponse != null && originalResponse.equals("PONG");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Pobiera zdalny adres serwera
     * @return Adres serwera lub null
     */
    public String getRemoteAddress() {
        if (socket != null && socket.isConnected()) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        return null;
    }

    /**
     * Pobiera lokalny adres klienta
     * @return Adres klienta lub null
     */
    public String getLocalAddress() {
        if (socket != null) {
            return socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
        }
        return null;
    }
}