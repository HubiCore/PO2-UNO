/**
 * Główna klasa aplikacji serwera UNO.
 * <p>
 * Ta klasa zawiera punkt wejścia ({@code main}) dla aplikacji serwera.
 * Tworzy instancję serwera UNO ({@link UnoServer}) i uruchamia go.
 * </p>
 *
 */
package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    /**
     * Główna metoda uruchamiająca aplikację serwera UNO.
     * <p>
     * Tworzy instancję serwera UNO ({@code UnoServer}) i rozpoczyna jego działanie
     * poprzez wywołanie metody {@link UnoServer#start()}.
     * </p>
     *
     * @param args argumenty wiersza poleceń (niewykorzystywane w tej implementacji)
     */
    public static void main(String[] args) {
        UnoServer server = new UnoServer();
        server.start();
    }
}