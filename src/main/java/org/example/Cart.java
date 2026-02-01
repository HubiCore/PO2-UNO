package org.example;

import java.util.random.RandomGenerator;

/**
 * Reprezentuje kartę do gry w UNO.
 * Karta posiada kolor i wartość. Karta typu WILD jest zawsze ważna do zagrania.
 *
 */
public class Cart {
    private String kolor;
    private String wartosc;

    /**
     * Tworzy nową kartę o podanym kolorze i wartości.
     *
     * @param kolor   kolor karty (RED, GREEN, BLUE, YELLOW, WILD)
     * @param wartosc wartość karty (0-9, +2, ⏸, ↺, W, W4)
     */
    public Cart(String kolor, String wartosc) {
        this.kolor = kolor;
        this.wartosc = wartosc;
    }

    /**
     * Zwraca kolor karty.
     *
     * @return kolor karty
     */
    public String getKolor() {
        return kolor;
    }

    /**
     * Zwraca wartość karty.
     *
     * @return wartość karty
     */
    public String getWartosc() {
        return wartosc;
    }

    /**
     * Zwraca reprezentację tekstową karty w formacie "kolor:wartosc".
     *
     * @return łańcuch znaków reprezentujący kartę
     */
    @Override
    public String toString() {
        return kolor + ":" + wartosc;
    }

    /**
     * Tworzy obiekt Cart na podstawie łańcucha znaków w formacie "kolor:wartosc".
     *
     * @param str łańcuch znaków w formacie "kolor:wartosc"
     * @return nowy obiekt Cart
     * @throws ArrayIndexOutOfBoundsException jeśli łańcuch nie zawiera dwukropka
     */
    public static Cart fromString(String str) {
        String[] parts = str.split(":");
        return new Cart(parts[0], parts[1]);
    }

    /**
     * Generuje losową kartę. Karty WILD mają przypisane specjalne wartości (W lub W4).
     *
     * @return losowo wygenerowana karta
     */
    public static Cart generate_random_cart() {
        String[] colors = {"RED", "GREEN", "BLUE", "YELLOW", "WILD"};
        String[] weights = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "+2", "⏸", "↺", "W", "W4"};

        int colorIndex = (int)(Math.random() * colors.length);
        int weightIndex = (int)(Math.random() * weights.length);

        // Jeśli karta jest WILD, to wartość musi być odpowiednia
        if (colors[colorIndex].equals("WILD")) {
            weightIndex = (int)(Math.random() * 2) + 13; // WILD lub WILD_DRAW4
        }

        return new Cart(colors[colorIndex], weights[weightIndex]);
    }

    /**
     * Sprawdza, czy tę kartę można zagrać na podanej karcie.
     * Karta może być zagrana, jeśli ma ten sam kolor lub tę samą wartość,
     * lub jest kartą typu WILD.
     *
     * @param otherCard karta, na której chcemy położyć obecną kartę
     * @return true jeśli kartę można zagrać, false w przeciwnym razie
     */
    public boolean canPlayOn(Cart otherCard) {
        if (this.kolor.equals("WILD")) {
            return true;
        }

        // Można zagrać jeśli ten sam kolor lub ta sama wartość
        return this.kolor.equals(otherCard.kolor) ||
                this.wartosc.equals(otherCard.wartosc);
    }
}