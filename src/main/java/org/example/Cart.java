package org.example;

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
}