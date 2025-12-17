package org.example;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class Card {
    private String kolor;
    private String wartosc;
    private StackPane view;

    public Card(String kolor, String wartosc) {
        this.kolor = kolor;
        this.wartosc = wartosc;
        stworzWidok();
    }

    private void stworzWidok() {
        view = new StackPane();
        view.setMinSize(80, 120);
        view.setMaxSize(80, 120);
        System.out.println("Przed catch");
        // OPCJA 1: Użyj obrazka jako tło
        try {
            System.out.println("W catch");
            // Sprawdź czy plik istnieje
            var stream = getClass().getResourceAsStream("/assets/textures/card_front.png");
            if (stream == null) {
                throw new RuntimeException("Nie znaleziono pliku tekstury!");
            }

            Image texture = new Image(stream);

            // Sprawdź czy obrazek się załadował
            if (texture.isError()) {
                throw new RuntimeException("Błąd ładowania tekstury!");
            }

            System.out.println("Tekstura załadowana poprawnie: " + texture.getWidth() + "x" + texture.getHeight());

            ImageView imageView = new ImageView(texture);
            imageView.setFitWidth(80);
            imageView.setFitHeight(120);
            imageView.setPreserveRatio(false);

            // Clip dla zaokrąglonych rogów
            Rectangle clip = new Rectangle(80, 120);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            imageView.setClip(clip);

            // Ramka
            Rectangle ramka = new Rectangle(80, 120);
            ramka.setArcWidth(10);
            ramka.setArcHeight(10);
            ramka.setFill(Color.TRANSPARENT);
            ramka.setStroke(Color.BLACK);
            ramka.setStrokeWidth(2);

            // Kolorowa nakładka (opcjonalnie, dla efektu koloru)
            Rectangle kolorowaNakladka = new Rectangle(80, 120);
            kolorowaNakladka.setArcWidth(10);
            kolorowaNakladka.setArcHeight(10);
            kolorowaNakladka.setFill(getKolorFill());
            kolorowaNakladka.setOpacity(0.6); // Półprzezroczysta

            Label wartoscLabel = new Label(wartosc);
            wartoscLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, black, 2, 1.0, 0, 0);");

            view.getChildren().addAll(imageView, kolorowaNakladka, ramka, wartoscLabel);

        } catch (Exception e) {
            // OPCJA 2: Jeśli obrazek nie istnieje, użyj CSS z wzorem
            System.out.println("Nie znaleziono tekstury, używam wzoru CSS. Błąd: " + e.getMessage());
            e.printStackTrace();

            Region tlo = new Region();
            tlo.setMinSize(80, 120);
            tlo.setMaxSize(80, 120);
            tlo.setStyle(
                    "-fx-background-color: " + getKolorHex() + ";" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: black;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 10;" +
                            // Wzór - przekątne linie
                            "-fx-background-image: repeating-linear-gradient(" +
                            "45deg, transparent, transparent 10px, " +
                            "rgba(255,255,255,0.15) 10px, rgba(255,255,255,0.15) 20px);"
            );

            Label wartoscLabel = new Label(wartosc);
            wartoscLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, black, 2, 1.0, 0, 0);");

            view.getChildren().addAll(tlo, wartoscLabel);
        }
        System.out.println("Po catch");
        view.setStyle("-fx-cursor: hand;");
    }

    private Color getKolorFill() {
        switch (kolor) {
            case "CZERWONY": return Color.RED;
            case "NIEBIESKI": return Color.BLUE;
            case "ZIELONY": return Color.GREEN;
            case "ŻÓŁTY": return Color.YELLOW;
            default: return Color.BLACK;
        }
    }

    private String getKolorHex() {
        switch (kolor) {
            case "CZERWONY": return "#E53935";
            case "NIEBIESKI": return "#1E88E5";
            case "ZIELONY": return "#43A047";
            case "ŻÓŁTY": return "#FDD835";
            default: return "#000000";
        }
    }

    public StackPane getView() {
        return view;
    }

    public String getKolor() {
        return kolor;
    }

    public String getWartosc() {
        return wartosc;
    }
}