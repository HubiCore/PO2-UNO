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
    private static final Logger logger = Logger.getInstance();

    /**
     * Podstawowy konstruktor tworzący połączenie z domyślnymi ustawieniami
     * (localhost:2137).
     */
    public ClientConnection() {
        logger.debug("Utworzono ClientConnection z domyślnymi ustawieniami");
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
        logger.debug("Utworzono ClientConnection: " + host + ":" + port);
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
                logger.warning("Istniejące połączenie, zamykam przed nowym połączeniem");
                disconnect();
            }

            logger.info("Próbuję połączyć się z " + host + ":" + port);
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000); // Timeout połączenia 5 sekund
            socket.setSoTimeout(300000); // Timeout odczytu 10 sekund

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            connected = true;
            logger.info("Połączono pomyślnie z " + host + ":" + port);
            return true;

        } catch (UnknownHostException e) {
            logger.error("Nieznany host: " + host);
            return false;
        } catch (SocketTimeoutException e) {
            logger.error("Timeout połączenia z serwerem");
            return false;
        } catch (IOException e) {
            logger.error(e, "Błąd połączenia");
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
            logger.error("Nie można wysłać - brak połączenia");
            return false;
        }

        if (writer == null) {
            logger.error("Writer jest null");
            return false;
        }

        try {
            writer.println(message);
            writer.flush();
            logger.debug("Wysłano wiadomość: " + message);
            return true;
        } catch (Exception e) {
            logger.error(e, "Błąd wysyłania wiadomości");
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
            logger.error("Nie można odebrać - brak połączenia");
            return null;
        }

        if (reader == null) {
            logger.error("Reader jest null");
            return null;
        }

        try {
            logger.debug("Oczekuję na wiadomość...");
            String response = reader.readLine();
            logger.debug("Odebrano wiadomość: " + response);
            return response;
        } catch (SocketTimeoutException e) {
            logger.error("Timeout oczekiwania na odpowiedź");
            return null;
        } catch (IOException e) {
            logger.error(e, "Błąd odbierania wiadomości");
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
            logger.error("Nie można odebrać - brak połączenia lub socket jest null");
            return null;
        }

        try {
            logger.debug("Ustawiam timeout na " + timeoutMs + "ms");
            int originalTimeout = socket.getSoTimeout();
            socket.setSoTimeout(timeoutMs);
            String response = receiveMessage();
            socket.setSoTimeout(originalTimeout);
            return response;
        } catch (SocketException e) {
            logger.error(e, "Błąd ustawiania timeoutu");
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
        logger.debug("Wysyłam i oczekuję odpowiedzi...");
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
        logger.debug("Wysyłam i oczekuję odpowiedzi z timeoutem " + timeoutMs + "ms...");
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
                logger.error("Timeout oczekiwania na odpowiedź z prefiksem: " + expectedPrefix);
                return null;
            }

            String response = receiveMessageWithTimeout(remainingTime);
            if (response == null) {
                continue;
            }

            if (response.startsWith(expectedPrefix)) {
                logger.debug("Znaleziono oczekiwaną odpowiedź: " + response);
                return response;
            } else {
                logger.debug("Pomijam nieoczekiwaną odpowiedź: " + response);
            }
        }

        logger.error("Nie znaleziono odpowiedzi z prefiksem: " + expectedPrefix);
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
                logger.debug("Czyszczenie bufora - pomijam: " + message);
                messagesCleared++;
            }

            // Przywróć normalny timeout
            socket.setSoTimeout(originalTimeout);

            if (messagesCleared > 0) {
                logger.info("Wyczyszczono " + messagesCleared + " wiadomości z bufora");
            }

        } catch (SocketTimeoutException e) {
            // To normalne - oznacza, że nie ma więcej wiadomości
            logger.debug("Brak więcej wiadomości w buforze");
        } catch (IOException e) {
            logger.error(e, "Błąd czyszczenia bufora");
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
        logger.debug("Sprawdzam połączenie - wynik: " + isConnected);
        return isConnected;
    }

    /**
     * Zamyka połączenie z serwerem.
     * Zamyka wszystkie zasoby: writer, reader i socket.
     * Ustawia flagę connected na false.
     */
    public void disconnect() {
        logger.info("Rozłączam...");
        connected = false;

        try {
            if (writer != null) {
                writer.close();
                writer = null;
                logger.debug("Writer zamknięty");
            }
            if (reader != null) {
                reader.close();
                reader = null;
                logger.debug("Reader zamknięty");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
                logger.debug("Socket zamknięty");
            }
            logger.info("Rozłączono pomyślnie");
        } catch (IOException e) {
            logger.error(e, "Błąd podczas zamykania połączenia");
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
        logger.debug("Ustawiono host: " + host);
        this.host = host;
    }

    /**
     * Ustawia port serwera.
     * Zmiana portu nie wpływa na istniejące połączenie.
     *
     * @param port Port serwera
     */
    public void setPort(int port) {
        logger.debug("Ustawiono port: " + port);
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
        logger.debug("Ustawiono parametry połączenia: " + host + ":" + port);
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
        logger.debug("Ustawiono tryb debug: " + debug);
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
            logger.debug("Testowanie połączenia...");
            String originalResponse = sendAndReceiveWithTimeout("PING", 3000);
            boolean result = originalResponse != null && originalResponse.equals("PONG");
            logger.debug("Test połączenia: " + result);
            return result;
        } catch (Exception e) {
            logger.error(e, "Błąd testowania połączenia");
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