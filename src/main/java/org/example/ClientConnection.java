package org.example;

import java.io.*;
import java.net.*;

/**
 * Klasa zarządzająca połączeniem klienta z serwerem TCP.
 * Implementuje interfejs AutoCloseable, co umożliwia użycie w try-with-resources.
 * Zapewnia metody do nawiązywania połączenia, wysyłania i odbierania wiadomości,
 * zarządzania timeoutami oraz zarządzania stanem połączenia.
 *
 */
public class ClientConnection implements AutoCloseable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;
    private String host = "localhost";
    private int port = 2137;
    private boolean debug = true; // Włącz/Wyłącz logowanie debug

    /**
     * Podstawowy konstruktor tworzący połączenie z domyślnymi ustawieniami
     * (localhost:2137).
     */
    public ClientConnection() {
    }

    /**
     * Konstruktor umożliwiający ustawienie niestandardowego hosta i portu.
     *
     * @param host Adres serwera (np. "localhost", "192.168.1.1")
     * @param port Port serwera (np. 8080)
     */
    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Nawiązuje połączenie z serwerem.
     * W przypadku już istniejącego połączenia, najpierw je zamyka.
     * Ustawia timeout połączenia na 5 sekund i timeout odczytu na 10 sekund.
     *
     * @return true jeśli połączenie zostało nawiązane pomyślnie,
     *         false w przypadku jakiegokolwiek błędu
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
     * Wysyła wiadomość tekstową do serwera.
     *
     * @param message Wiadomość do wysłania
     * @return true jeśli wiadomość została wysłana pomyślnie,
     *         false w przypadku braku połączenia lub błędu wysyłania
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
     * Odbiera jedną wiadomość od serwera w sposób blokujący.
     * Czeka na dane przez czas określony przez socket timeout (domyślnie 10 sekund).
     *
     * @return Odebrana wiadomość jako String, lub null w przypadku błędu lub timeoutu
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
     * Odbiera wiadomość od serwera z określonym timeoutem.
     * Tymczasowo zmienia timeout socketa na podaną wartość.
     *
     * @param timeoutMs Timeout w milisekundach
     * @return Odebrana wiadomość jako String, lub null w przypadku błędu lub timeoutu
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
     * Wysyła wiadomość do serwera i czeka na odpowiedź.
     * Używa domyślnego timeoutu socketa (10 sekund).
     *
     * @param message Wiadomość do wysłania
     * @return Odpowiedź serwera jako String, lub null w przypadku błędu
     */
    public String sendAndReceive(String message) {
        log("Wysyłam i oczekuję odpowiedzi...");
        if (sendMessage(message)) {
            return receiveMessage();
        }
        return null;
    }

    /**
     * Wysyła wiadomość do serwera i czeka na odpowiedź z określonym timeoutem.
     *
     * @param message Wiadomość do wysłania
     * @param timeoutMs Maksymalny czas oczekiwania na odpowiedź (w milisekundach)
     * @return Odpowiedź serwera jako String, lub null w przypadku błędu lub timeoutu
     */
    public String sendAndReceiveWithTimeout(String message, int timeoutMs) {
        log("Wysyłam i oczekuję odpowiedzi z timeoutem " + timeoutMs + "ms...");
        if (sendMessage(message)) {
            return receiveMessageWithTimeout(timeoutMs);
        }
        return null;
    }

    /**
     * Wysyła wiadomość i czeka na odpowiedź o określonym prefiksie w podanym czasie.
     * Pomija wiadomości, które nie zaczynają się od oczekiwanego prefiksu.
     *
     * @param message Wiadomość do wysłania
     * @param expectedPrefix Prefix, od którego powinna zaczynać się oczekiwana odpowiedź
     * @param timeoutMs Maksymalny czas oczekiwania (w milisekundach)
     * @return Odpowiedź serwera zaczynająca się od expectedPrefix, lub null jeśli nie znaleziono
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
     * Czyści bufor wejściowy poprzez odczytanie wszystkich dostępnych wiadomości.
     * Przydatne do usuwania zaległych wiadomości przed rozpoczęciem nowej sekwencji komunikacji.
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
     * Sprawdza, czy połączenie z serwerem jest aktywne.
     * Weryfikuje flagę connected oraz stan socketa.
     *
     * @return true jeśli połączenie jest aktywne, false w przeciwnym razie
     */
    public boolean isConnected() {
        boolean isConnected = connected && socket != null && !socket.isClosed() && socket.isConnected();
        log("Sprawdzam połączenie - wynik: " + isConnected);
        return isConnected;
    }

    /**
     * Zamyka połączenie z serwerem.
     * Zamyka wszystkie zasoby: writer, reader i socket.
     * Ustawia flagę connected na false.
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
     * Implementacja metody z interfejsu AutoCloseable.
     * Umożliwia użycie klasy w try-with-resources.
     * Wywołuje metodę disconnect().
     */
    @Override
    public void close() {
        disconnect();
    }

    /**
     * Ustawia adres hosta serwera.
     * Zmiana hosta nie wpływa na istniejące połączenie.
     *
     * @param host Adres serwera (np. "localhost", "192.168.1.1")
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Ustawia port serwera.
     * Zmiana portu nie wpływa na istniejące połączenie.
     *
     * @param port Port serwera
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Ustawia zarówno host jak i port serwera jednocześnie.
     * Zmiana parametrów nie wpływa na istniejące połączenie.
     *
     * @param host Adres serwera
     * @param port Port serwera
     */
    public void setConnectionParams(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Włącza lub wyłącza tryb debugowania.
     * W trybie debugowania wyświetlane są dodatkowe informacje w konsoli.
     *
     * @param debug true aby włączyć logowanie debug, false aby wyłączyć
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Zwraca aktualnie ustawiony adres hosta.
     *
     * @return Adres serwera
     */
    public String getHost() {
        return host;
    }

    /**
     * Zwraca aktualnie ustawiony port.
     *
     * @return Port serwera
     */
    public int getPort() {
        return port;
    }

    /**
     * Sprawdza, czy tryb debugowania jest włączony.
     *
     * @return true jeśli debug włączony, false w przeciwnym razie
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Pomocnicza metoda do logowania informacji debugowych.
     * Wiadomości są wyświetlane tylko gdy debug = true.
     *
     * @param message Wiadomość do zalogowania
     */
    private void log(String message) {
        if (debug) {
            System.out.println("ClientConnection: " + message);
        }
    }

    /**
     * Pomocnicza metoda do logowania błędów.
     * Wiadomości błędów są zawsze wyświetlane (nawet gdy debug = false).
     *
     * @param message Wiadomość o błędzie
     */
    private void logError(String message) {
        System.err.println("ClientConnection ERROR: " + message);
    }

    /**
     * Testuje połączenie z serwerem wysyłając komendę "PING".
     * Oczekuje odpowiedzi "PONG" w ciągu 3 sekund.
     *
     * @return true jeśli serwer odpowiada prawidłowo, false w przeciwnym razie
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
     * Zwraca zdalny adres serwera, z którym jest nawiązane połączenie.
     * Format: "adres_ip:port"
     *
     * @return Zdalny adres serwera jako String, lub null jeśli brak połączenia
     */
    public String getRemoteAddress() {
        if (socket != null && socket.isConnected()) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        return null;
    }

    /**
     * Zwraca lokalny adres klienta.
     * Format: "adres_ip:port"
     *
     * @return Lokalny adres klienta jako String, lub null jeśli brak połączenia
     */
    public String getLocalAddress() {
        if (socket != null) {
            return socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
        }
        return null;
    }
}