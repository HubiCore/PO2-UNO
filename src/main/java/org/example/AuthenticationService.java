/**
 * Klasa AuthenticationService zapewnia podstawowe funkcjonalności uwierzytelniania użytkowników,
 * w tym rejestrację i logowanie z wykorzystaniem hashowania haseł.
 * Przechowuje dane użytkowników w pamięci za pomocą mapy (username -> hash hasła).
 *
 * <p><b>Uwaga dotycząca bezpieczeństwa:</b> klasa wykorzystuje algorytm MD5 do hashowania haseł,
 * który jest obecnie uznawany za niebezpieczny w kontekstach produkcyjnych. W przypadku braku dostępności
 * MD5, hasła są przechowywane w postaci jawnej (tylko w celach demonstracyjnych — NIE UŻYWAĆ W PRODUKCJI).
 *
 * <p>Przykład użycia:
 * <pre>{@code
 * AuthenticationService authService = new AuthenticationService();
 * authService.registerUser("nowyUzytkownik", "tajneHasło");
 * boolean result = authService.authenticate("nowyUzytkownik", "tajneHasło");
 * }</pre>
 *
 * @see java.security.MessageDigest
 */
package org.example;

import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthenticationService {
    private Map<String, String> users = new HashMap<>();
    private static final Logger logger = Logger.getInstance();

    /**
     * Konstruktor domyślny inicjalizujący serwis uwierzytelniania.
     * Automatycznie rejestruje przykładowych użytkowników:
     * <ul>
     *   <li>admin / admin123</li>
     *   <li>user / password123</li>
     *   <li>test / test123</li>
     * </ul>
     */
    public AuthenticationService() {
        logger.info("Inicjalizacja AuthenticationService");

        // Przykładowi użytkownicy z zahaszowanymi hasłami
        registerUser("admin", "admin123");
        registerUser("user", "password123");
        registerUser("test", "test123");

        logger.info("AuthenticationService zainicjalizowany z przykładowymi użytkownikami");
    }

    /**
     * Uwierzytelnia użytkownika na podstawie podanej nazwy i hasła.
     * Porównuje hash podanego hasła z zapisanym hashem w systemie.
     *
     * @param username nazwa użytkownika (nie może być null)
     * @param password hasło do weryfikacji (nie może być null)
     * @return {@code true} jeśli uwierzytelnienie się powiodło, {@code false} w przeciwnym wypadku
     */
    public boolean authenticate(String username, String password) {
        logger.debug("Próba uwierzytelnienia użytkownika: " + username);

        String storedHash = users.get(username);
        if (storedHash == null) {
            logger.warning("Użytkownik nie istnieje: " + username);
            return false; // Użytkownik nie istnieje
        }

        String inputHash = hashPassword(password);
        boolean result = storedHash.equals(inputHash);

        if (result) {
            logger.info("Uwierzytelnienie udane dla użytkownika: " + username);
        } else {
            logger.warning("Uwierzytelnienie nieudane dla użytkownika: " + username);
        }

        return result;
    }

    /**
     * Rejestruje nowego użytkownika w systemie po uprzedniej walidacji danych.
     * <p>Wymagania walidacyjne:
     * <ul>
     *   <li>Nazwa użytkownika musi składać się wyłącznie z liter (w tym polskich znaków diakrytycznych)</li>
     *   <li>Hasło musi mieć co najmniej 6 znaków</li>
     *   <li>Nazwa użytkownika nie może być już zajęta</li>
     * </ul>
     *
     * @param username nazwa użytkownika do zarejestrowania
     * @param password hasło użytkownika (przed hashowaniem)
     * @return {@code true} jeśli rejestracja się powiodła, {@code false} jeśli dane są nieprawidłowe
     *         lub użytkownik już istnieje
     */
    public boolean registerUser(String username, String password) {
        logger.debug("Próba rejestracji użytkownika: " + username);

        // Walidacja nazwy użytkownika - tylko litery
        if (!username.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+")) {
            logger.warning("Nieprawidłowa nazwa użytkownika (tylko litery): " + username);
            return false;
        }

        // Walidacja hasła - min 6 znaków
        if (password.length() < 6) {
            logger.warning("Hasło za krótkie (min 6 znaków) dla użytkownika: " + username);
            return false;
        }

        if (users.containsKey(username)) {
            logger.warning("Użytkownik już istnieje: " + username);
            return false; // Użytkownik już istnieje
        }

        // Hashuj hasło przed zapisaniem
        String hashedPassword = hashPassword(password);
        users.put(username, hashedPassword);

        logger.info("Użytkownik zarejestrowany pomyślnie: " + username);
        return true;
    }

    /**
     * Sprawdza, czy użytkownik o podanej nazwie istnieje w systemie.
     *
     * @param username nazwa użytkownika do sprawdzenia
     * @return {@code true} jeśli użytkownik istnieje, {@code false} w przeciwnym wypadku
     */
    public boolean userExists(String username) {
        boolean exists = users.containsKey(username);
        logger.debug("Sprawdzanie istnienia użytkownika " + username + ": " + exists);
        return exists;
    }

    /**
     * Haszuje podane hasło za pomocą algorytmu MD5.
     * <p><b>Uwaga:</b> MD5 jest algorytmem przestarzałym i niezalecanym do zastosowań kryptograficznych.
     * W przypadku braku dostępności algorytmu (NoSuchAlgorithmException) zwraca hasło w postaci jawnej.
     *
     * @param password hasło w postaci tekstowej
     * @return zahaszowane hasło w formie heksadecymalnej (lub w postaci jawnej w przypadku błędu)
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes());

            // Konwertuj byte[] na hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String result = hexString.toString();
            logger.debug("Hash hasła wygenerowany: " + result.substring(0, Math.min(8, result.length())) + "...");
            return result;

        } catch (NoSuchAlgorithmException e) {
            // W przypadku błędu, użyj fallback - NIE RÓB TEGO W PRODUKCJI!
            logger.error("MD5 algorithm not found, using plain text (INSECURE!)");
            logger.error(e, "Błąd podczas hashowania");
            return password; // Bardzo niebezpieczne!
        }
    }

    /**
     * Wyświetla w konsoli listę wszystkich zarejestrowanych użytkowników oraz ich zahaszowane hasła.
     * <p><b>Uwaga:</b> metoda służy wyłącznie do celów debugowania i nie powinna być używana w środowisku produkcyjnym.
     */
    public void printUsers() {
        logger.info("Zarejestrowani użytkownicy:");
        for (Map.Entry<String, String> entry : users.entrySet()) {
            logger.info(entry.getKey() + " -> " + entry.getValue());
        }
    }
}