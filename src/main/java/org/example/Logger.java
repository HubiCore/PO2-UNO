package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Klasa odpowiedzialna za zarządzanie logami aplikacji.
 * Zapisuje logi do pliku oraz wyświetla je na konsoli.
 * Implementuje wzorzec Singleton.
 */
public class Logger {

    private static Logger instance;
    private PrintWriter logFile;
    private final SimpleDateFormat dateFormat;
    private final ReentrantLock lock;

    // Poziomy logowania
    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";
    public static final String DEBUG = "DEBUG";

    /**
     * Prywatny konstruktor - wzorzec Singleton
     */
    private Logger() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lock = new ReentrantLock();
        initLogFile();
    }

    /**
     * Zwraca instancję Logger (Singleton)
     */
    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    /**
     * Inicjalizuje plik logów
     */
    private void initLogFile() {
        try {
            // Tworzenie katalogu logów jeśli nie istnieje
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // Nazwa pliku z datą
            SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String fileName = "logs/uno-server_" + fileDateFormat.format(new Date()) + ".log";

            logFile = new PrintWriter(new FileWriter(fileName, true), true);
            log(INFO, "Logger initialized. Log file: " + fileName);
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
            // Fallback - logowanie tylko na konsolę
            logFile = null;
        }
    }

    /**
     * Zapisuje wiadomość do logów
     *
     * @param level poziom logowania (INFO, WARNING, ERROR, DEBUG)
     * @param message wiadomość do zalogowania
     */
    public void log(String level, String message) {
        lock.lock();
        try {
            String timestamp = dateFormat.format(new Date());
            String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

            // Wyświetl na konsoli
            if (ERROR.equals(level)) {
                System.err.println(logMessage);
            } else {
                System.out.println(logMessage);
            }

            // Zapisz do pliku
            if (logFile != null) {
                logFile.println(logMessage);
                logFile.flush();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Zapisuje wiadomość informacyjną
     *
     * @param message wiadomość do zalogowania
     */
    public void info(String message) {
        log(INFO, message);
    }

    /**
     * Zapisuje ostrzeżenie
     *
     * @param message wiadomość ostrzeżenia
     */
    public void warning(String message) {
        log(WARNING, message);
    }

    /**
     * Zapisuje błąd
     *
     * @param message wiadomość błędu
     */
    public void error(String message) {
        log(ERROR, message);
    }

    /**
     * Zapisuje wiadomość debug
     *
     * @param message wiadomość debug
     */
    public void debug(String message) {
        log(DEBUG, message);
    }

    /**
     * Formatuje i loguje wyjątek
     *
     * @param e wyjątek do zalogowania
     * @param context kontekst w którym wystąpił wyjątek
     */
    public void error(Exception e, String context) {
        String message = String.format("%s: %s", context, e.getMessage());
        error(message);

        // Dodaj stack trace do logów
        if (logFile != null) {
            lock.lock();
            try {
                logFile.println("Stack trace:");
                e.printStackTrace(logFile);
                logFile.flush();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Zamyka zasoby loggera
     */
    public void shutdown() {
        lock.lock();
        try {
            if (logFile != null) {
                info("Logger shutting down");
                logFile.close();
            }
        } finally {
            lock.unlock();
        }
    }
}