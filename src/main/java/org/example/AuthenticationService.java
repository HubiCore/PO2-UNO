package org.example;

import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthenticationService {
    private Map<String, String> users = new HashMap<>();

    public AuthenticationService() {
        // Przykładowi użytkownicy z zahaszowanymi hasłami
        registerUser("admin", "admin123");
        registerUser("user", "password123");
        registerUser("test", "test123");
    }

    public boolean authenticate(String username, String password) {
        String storedHash = users.get(username);
        if (storedHash == null) {
            return false; // Użytkownik nie istnieje
        }

        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

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

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

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

    // Metoda pomocnicza do wyświetlania istniejących użytkowników (debug)
    public void printUsers() {
        System.out.println("Zarejestrowani użytkownicy:");
        for (Map.Entry<String, String> entry : users.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}