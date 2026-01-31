package org.example;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationService {
    private Map<String, String> users = new HashMap<>();

    public AuthenticationService() {
        // Przykładowi użytkownicy (w rzeczywistości dane powinny być z bazy danych)
        registerUser("admin", "admin123");
        registerUser("user", "password123");
        registerUser("test", "test123");
    }

    public boolean authenticate(String username, String password) {
        // W rzeczywistości powinno się używać hashowania (bcrypt, PBKDF2)
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
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

        users.put(username, password);
        return true;
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}