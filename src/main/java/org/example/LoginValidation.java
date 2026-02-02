package org.example;

import java.util.regex.Pattern;

/**
 * Klasa udostępniająca metody walidacji tekstu przy użyciu wyrażeń regularnych.
 * Obsługuje różne typy walidacji zdefiniowane w enum {@link ValidationType}.
 * Zawiera metody do walidacji z różnymi parametrami oraz zwracania szczegółowych wyników.
 */
public class LoginValidation {
    private static final Logger logger = Logger.getInstance();

    /**
     * Enum definiujący typy walidacji dostępne w klasie.
     */
    public enum ValidationType {
        /** Tylko litery (w tym polskie znaki) i spacje */
        ONLY_TEXT,

        /** Tylko cyfry */
        ONLY_NUMBERS,

        /** Tylko litery i cyfry (bez polskich znaków) */
        ALPHANUMERIC,

        /** Litery (w tym polskie znaki), cyfry i spacje - bez znaków specjalnych */
        NO_SPECIAL_CHARS,

        /** Nazwa użytkownika: litery, cyfry, podkreślenia (3-20 znaków) */
        USERNAME,

        /** Typ niestandardowy - wymaga podania własnego wyrażenia regularnego */
        CUSTOM
    }

    /**
     * Sprawdza, czy podany tekst spełnia kryteria wybranego typu walidacji.
     * Tekst jest przycinany (trim) przed walidacją.
     *
     * @param type typ walidacji do zastosowania
     * @param text tekst do walidacji
     * @return true jeśli tekst jest niepusty i pasuje do wzorca, false w przeciwnym razie
     */
    public static boolean isValid(ValidationType type, String text) {
        logger.debug("Walidacja typu " + type + " dla tekstu: '" + text + "'");

        if (text == null || text.trim().isEmpty()) {
            logger.debug("Tekst jest null lub pusty");
            return false;
        }

        String regex = getRegexForType(type);
        boolean result = Pattern.matches(regex, text.trim());

        logger.debug("Wynik walidacji: " + result);
        return result;
    }

    /**
     * Sprawdza, czy podany tekst spełnia kryteria wybranego typu walidacji
     * oraz mieści się w określonym zakresie długości.
     * Tekst jest przycinany (trim) przed walidacją.
     *
     * @param type typ walidacji do zastosowania
     * @param text tekst do walidacji
     * @param minLength minimalna dopuszczalna długość tekstu (po przycięciu);
     *                  wartość ≤ 0 oznacza brak ograniczenia minimalnego
     * @param maxLength maksymalna dopuszczalna długość tekstu (po przycięciu);
     *                  wartość ≤ 0 oznacza brak ograniczenia maksymalnego
     * @return true jeśli tekst jest niepusty, pasuje do wzorca i mieści się w zakresie długości,
     *         false w przeciwnym razie
     */
    public static boolean isValid(ValidationType type, String text, int minLength, int maxLength) {
        logger.debug("Walidacja typu " + type + " dla tekstu: '" + text + "' (długość: " + minLength + "-" + maxLength + ")");

        if (text == null) {
            logger.debug("Tekst jest null");
            return false;
        }

        String trimmed = text.trim();

        if (minLength > 0 && trimmed.length() < minLength) {
            logger.debug("Tekst za krótki: " + trimmed.length() + " < " + minLength);
            return false;
        }
        if (maxLength > 0 && trimmed.length() > maxLength) {
            logger.debug("Tekst za długi: " + trimmed.length() + " > " + maxLength);
            return false;
        }

        String regex = getRegexForType(type);
        boolean result = Pattern.matches(regex, trimmed);

        logger.debug("Wynik walidacji: " + result);
        return result;
    }

    /**
     * Sprawdza, czy podany tekst pasuje do niestandardowego wyrażenia regularnego.
     * Tekst jest przycinany (trim) przed walidacją.
     *
     * @param customRegex niestandardowe wyrażenie regularne do walidacji
     * @param text tekst do walidacji
     * @return true jeśli tekst pasuje do podanego wyrażenia regularnego, false w przeciwnym razie
     * @throws NullPointerException jeśli customRegex lub text są null
     */
    public static boolean isValidWithCustomRegex(String customRegex, String text) {
        logger.debug("Walidacja z wyrażeniem niestandardowym dla tekstu: '" + text + "'");

        if (text == null || customRegex == null) {
            logger.debug("Tekst lub wyrażenie jest null");
            return false;
        }

        boolean result = Pattern.matches(customRegex, text.trim());
        logger.debug("Wynik walidacji niestandardowej: " + result);
        return result;
    }

    /**
     * Zwraca wyrażenie regularne odpowiadające podanemu typowi walidacji.
     *
     * @param type typ walidacji
     * @return wyrażenie regularne dla danego typu
     */
    private static String getRegexForType(ValidationType type) {
        String regex = switch (type) {
            case ONLY_TEXT -> "^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\s]+$";
            case ONLY_NUMBERS -> "^[0-9]+$";
            case ALPHANUMERIC -> "^[a-zA-Z0-9]+$";
            case NO_SPECIAL_CHARS -> "^[a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\s]+$";
            case USERNAME -> "^[a-zA-Z0-9_]{3,20}$";
            default -> ".*";
        };

        logger.debug("Regex dla typu " + type + ": " + regex);
        return regex;
    }

    /**
     * Wykonuje walidację tekstu dla podanego typu i zwraca szczegółowy wynik
     * zawierający status oraz wiadomość w przypadku niepowodzenia.
     *
     * @param type typ walidacji do zastosowania
     * @param text tekst do walidacji
     * @return obiekt {@link ValidationResult} zawierający wynik walidacji i ewentualny komunikat błędu
     */
    public static ValidationResult validateWithMessage(ValidationType type, String text) {
        logger.debug("Walidacja z wiadomością zwrotną dla typu " + type + " i tekstu: '" + text + "'");

        ValidationResult result = new ValidationResult();

        if (text == null || text.trim().isEmpty()) {
            logger.debug("Tekst jest null lub pusty");
            result.setValid(false);
            result.setMessage("Tekst nie może być pusty");
            return result;
        }

        String trimmed = text.trim();
        String regex = getRegexForType(type);

        if (!Pattern.matches(regex, trimmed)) {
            logger.debug("Tekst nie pasuje do wzorca");
            result.setValid(false);
            result.setMessage(getErrorMessage(type));
            return result;
        }

        logger.debug("Walidacja udana");
        result.setValid(true);
        return result;
    }

    /**
     * Zwraca komunikat błędu odpowiedni dla danego typu walidacji.
     *
     * @param type typ walidacji
     * @return tekstowy opis błędu walidacji
     */
    private static String getErrorMessage(ValidationType type) {
        String errorMessage = switch (type) {
            case ONLY_TEXT -> "Dozwolone są tylko litery (w tym polskie znaki)";
            case ONLY_NUMBERS -> "Dozwolone są tylko cyfry";
            case ALPHANUMERIC -> "Dozwolone są tylko litery i cyfry";
            case NO_SPECIAL_CHARS -> "Znaki specjalne są niedozwolone";
            case USERNAME -> "Nazwa użytkownika może zawierać tylko litery, cyfry i podkreślenia (3-20 znaków)";
            default -> "Nieprawidłowy format";
        };

        logger.debug("Komunikat błędu dla typu " + type + ": " + errorMessage);
        return errorMessage;
    }

    /**
     * Klasa reprezentująca wynik walidacji.
     * Zawiera flagę określającą poprawność oraz opcjonalny komunikat błędu.
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;

        /**
         * Zwraca status walidacji.
         *
         * @return true jeśli walidacja zakończyła się sukcesem, false w przeciwnym razie
         */
        public boolean isValid() { return valid; }

        /**
         * Ustawia status walidacji.
         *
         * @param valid nowa wartość statusu walidacji
         */
        public void setValid(boolean valid) { this.valid = valid; }

        /**
         * Zwraca komunikat błędu walidacji (jeśli wystąpił).
         *
         * @return komunikat błędu lub null, jeśli walidacja przebiegła pomyślnie
         */
        public String getMessage() { return message; }

        /**
         * Ustawia komunikat błędu walidacji.
         *
         * @param message nowy komunikat błędu
         */
        public void setMessage(String message) { this.message = message; }
    }
}