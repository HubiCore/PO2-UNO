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
    private StackPane backview;
    public Card(String kolor, String wartosc) {
        this.kolor = kolor;
        this.wartosc = wartosc;
        stworzWidok();
        //create_reverse_card();
    }

    private void stworzWidok() {
        view = new StackPane();
        view.setMinSize(80, 120);
        view.setMaxSize(80, 120);
        // OPCJA 1: Użyj obrazka jako tło
        try {
            System.out.println("W catch");
            var stream = getClass().getResourceAsStream("/assets/textures/card_front.png");
            if (stream == null) {
                throw new RuntimeException("Nie znaleziono pliku tekstury!");
            }
            Image texture = new Image(stream);

            if (texture.isError()) {
                throw new RuntimeException("Błąd ładowania tekstury!");
            }

            System.out.println("Tekstura załadowana poprawnie: " + texture.getWidth() + "x" + texture.getHeight());

            ImageView imageView = new ImageView(texture);
            imageView.setFitWidth(80);
            imageView.setFitHeight(120);
            imageView.setPreserveRatio(false);

            Rectangle clip = new Rectangle(80, 120);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            imageView.setClip(clip);

            Rectangle ramka = new Rectangle(80, 120);
            ramka.setArcWidth(10);
            ramka.setArcHeight(10);
            ramka.setFill(Color.TRANSPARENT);
            ramka.setStroke(Color.BLACK);
            ramka.setStrokeWidth(2);

            Rectangle kolorowaNakladka = new Rectangle(80, 120);
            kolorowaNakladka.setArcWidth(10);
            kolorowaNakladka.setArcHeight(10);
            kolorowaNakladka.setFill(getKolorFill());
            kolorowaNakladka.setOpacity(0.6);

            Label wartoscLabel = new Label(wartosc);
            wartoscLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, black, 2, 1.0, 0, 0);");

            view.getChildren().addAll(imageView, kolorowaNakladka, ramka, wartoscLabel);

        } catch (Exception e) {
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
    //problemem tego rozwiązania jest, że jest w chuj backview (50 frontów i 50 backów)

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
    public StackPane getbackview() {
        return backview;
    }
    public String getKolor() {
        return kolor;
    }

    public String getWartosc() {
        return wartosc;
    }
}