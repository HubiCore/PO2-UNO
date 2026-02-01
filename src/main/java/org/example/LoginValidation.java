package org.example;

import java.util.regex.Pattern;

public class LoginValidation {

    public enum ValidationType {
        ONLY_TEXT,

        ONLY_NUMBERS,

        ALPHANUMERIC,

        NO_SPECIAL_CHARS,

        USERNAME,

        CUSTOM

    }

    public static boolean isValid(ValidationType type, String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String regex = getRegexForType(type);
        return Pattern.matches(regex, text.trim());
    }

    public static boolean isValid(ValidationType type, String text, int minLength, int maxLength) {
        if (text == null) return false;

        String trimmed = text.trim();

        if (minLength > 0 && trimmed.length() < minLength) return false;
        if (maxLength > 0 && trimmed.length() > maxLength) return false;

        String regex = getRegexForType(type);
        return Pattern.matches(regex, trimmed);
    }

    public static boolean isValidWithCustomRegex(String customRegex, String text) {
        if (text == null || customRegex == null) return false;
        return Pattern.matches(customRegex, text.trim());
    }

    private static String getRegexForType(ValidationType type) {
        return switch (type) {
            case ONLY_TEXT -> "^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\s]+$";
            case ONLY_NUMBERS -> "^[0-9]+$";
            case ALPHANUMERIC -> "^[a-zA-Z0-9]+$";
            case NO_SPECIAL_CHARS -> "^[a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\s]+$";
            case USERNAME -> "^[a-zA-Z0-9_]{3,20}$";
            default -> ".*";

        };
    }

    public static ValidationResult validateWithMessage(ValidationType type, String text) {
        ValidationResult result = new ValidationResult();

        if (text == null || text.trim().isEmpty()) {
            result.setValid(false);
            result.setMessage("Tekst nie może być pusty");
            return result;
        }

        String trimmed = text.trim();
        String regex = getRegexForType(type);

        if (!Pattern.matches(regex, trimmed)) {
            result.setValid(false);
            result.setMessage(getErrorMessage(type));
            return result;
        }

        result.setValid(true);
        return result;
    }

    private static String getErrorMessage(ValidationType type) {
        return switch (type) {
            case ONLY_TEXT -> "Dozwolone są tylko litery (w tym polskie znaki)";
            case ONLY_NUMBERS -> "Dozwolone są tylko cyfry";
            case ALPHANUMERIC -> "Dozwolone są tylko litery i cyfry";
            case NO_SPECIAL_CHARS -> "Znaki specjalne są niedozwolone";
            case USERNAME -> "Nazwa użytkownika może zawierać tylko litery, cyfry i podkreślenia (3-20 znaków)";
            default -> "Nieprawidłowy format";
        };
    }

    public static class ValidationResult {
        private boolean valid;
        private String message;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}