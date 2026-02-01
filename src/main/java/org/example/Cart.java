package org.example;
import java.util.random.RandomGenerator;
public class Cart {
    private String kolor;
    private String wartosc;

    public Cart(String kolor, String wartosc) {
        this.kolor = kolor;
        this.wartosc = wartosc;
    }

    public String getKolor() {
        return kolor;
    }

    public String getWartosc() {
        return wartosc;
    }

    @Override
    public String toString() {
        return kolor + ":" + wartosc;
    }

    public static Cart fromString(String str) {
        String[] parts = str.split(":");
        return new Cart(parts[0], parts[1]);
    }

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

    public boolean canPlayOn(Cart otherCard) {

        if (this.kolor.equals("WILD")) {
            return true;
        }

        // Można zagrać jeśli ten sam kolor lub ta sama wartość
        return this.kolor.equals(otherCard.kolor) ||
                this.wartosc.equals(otherCard.wartosc);
    }
}