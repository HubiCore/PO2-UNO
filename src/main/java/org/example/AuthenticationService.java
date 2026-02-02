package org.example;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Serwis odpowiedzialny za operacje uwierzytelniające, w szczególności
 * związane z przetwarzaniem haseł użytkowników.
 * <p>
 * Klasa udostępnia metodę do hashowania hasła przy użyciu algorytmu MD5.
 * Należy zauważyć, że MD5 jest uważany za przestarzały i niezbyt bezpieczny
 * dla współczesnych zastosowań kryptograficznych. Rozważ użycie silniejszych
 * algorytmów (np. SHA-256, bcrypt) w środowiskach produkcyjnych.
 * </p>
 */
public class AuthenticationService {

    /**
     * Haszuje podane hasło przy użyciu algorytmu MD5 i zwraca jego reprezentację
     * szesnastkową.
     * <p>
     * W przypadku braku dostępności algorytmu MD5 w środowisku wykonawczym,
     * metoda loguje błąd i zwraca oryginalne, niezhashowane hasło jako
     * zabezpieczenie awaryjne. W praktyce taki scenariusz jest mało prawdopodobny,
     * ponieważ MD5 jest standardowo obsługiwany przez JVM.
     * </p>
     *
     * @param password hasło do zahashowania (nie może być {@code null})
     * @return łańcuch znaków będący szesnastkową reprezentacją skrótu MD5 hasła
     *         lub oryginalne hasło w przypadku wystąpienia wyjątku
     *         {@link NoSuchAlgorithmException}
     * @throws NullPointerException jeśli {@code password} jest {@code null}
     *
     * @example
     * <pre>{@code
     * AuthenticationService auth = new AuthenticationService();
     * String hashed = auth.hashPassword("myPassword123");
     * // hashed = "1a79a4d60de6718e8e5b326e338ae533"
     * }</pre>
     */
        private final Logger logger = Logger.getInstance();

        /**
         * Haszuje podane hasło przy użyciu algorytmu MD5...
         */
        public String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md.digest(password.getBytes());

                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }

                logger.debug("Password hashed successfully");
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                logger.error(e, "MD5 hashing error");
                return password; // W ostateczności zwróć niezhashowane hasło
            }
        }
    }