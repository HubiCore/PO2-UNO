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
        // Przykładowi użytkownicy z zahaszowanymi hasłami
        registerUser("admin", "admin123");
        registerUser("user", "password123");
        registerUser("test", "test123");
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
        String storedHash = users.get(username);
        if (storedHash == null) {
            return false; // Użytkownik nie istnieje
        }

        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
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
        // Walidacja nazwy użytkownika - tylko litery
        if (!username.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+")) {
            return false;
        }

        // Walidacja hasła - min 6 znaków
        if (password.length() < 6) {
            return false;
        }

        if (users.containsKey(username)) {
            return false; // Użytkownik już istnieje
        }

        // Hashuj hasło przed zapisaniem
        String hashedPassword = hashPassword(password);
        users.put(username, hashedPassword);
        return true;
    }

    /**
     * Sprawdza, czy użytkownik o podanej nazwie istnieje w systemie.
     *
     * @param username nazwa użytkownika do sprawdzenia
     * @return {@code true} jeśli użytkownik istnieje, {@code false} w przeciwnym wypadku
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
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
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // W przypadku błędu, użyj fallback - NIE RÓB TEGO W PRODUKCJI!
            System.err.println("MD5 algorithm not found, using plain text (INSECURE!)");
            return password; // Bardzo niebezpieczne!
        }
    }

    /**
     * Wyświetla w konsoli listę wszystkich zarejestrowanych użytkowników oraz ich zahaszowane hasła.
     * <p><b>Uwaga:</b> metoda służy wyłącznie do celów debugowania i nie powinna być używana w środowisku produkcyjnym.
     */
    public void printUsers() {
        System.out.println("Zarejestrowani użytkownicy:");
        for (Map.Entry<String, String> entry : users.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}