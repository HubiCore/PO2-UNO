package org.example;

import javafx.fxml.FXML;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import java.util.ArrayList;
import java.util.List;

public class UnoController {

    @FXML
    private StackPane stol;

    @FXML
    private HBox rekaGracza;

    @FXML
    private Label instrukcja;

    private Card wierzchniaKarta;
    private List<Card> kartyGracza;

    @FXML
    public void initialize() {
        System.out.println("=== UnoController.initialize() wywołane ===");
        rozpocznijGre();
    }

    private void rozpocznijGre() {
        System.out.println("=== rozpocznijGre() wywołane ===");
        kartyGracza = new ArrayList<>();

        // Dodaj przykładowe karty do ręki gracza
        kartyGracza.add(new Card("CZERWONY", "7"));
        kartyGracza.add(new Card("NIEBIESKI", "3"));
        kartyGracza.add(new Card("ZIELONY", "5"));
        kartyGracza.add(new Card("ŻÓŁTY", "2"));
        kartyGracza.add(new Card("CZERWONY", "STOP"));
        kartyGracza.add(new Card("NIEBIESKI", "+2"));

        // Karta startowa na stole
        wierzchniaKarta = new Card("CZERWONY", "1");
        stol.getChildren().add(wierzchniaKarta.getView());

        // Wyświetl karty gracza
        wyswietlKartyGracza();
        System.out.println("=== Karty stworzone i wyświetlone ===");
    }

    private void wyswietlKartyGracza() {
        rekaGracza.getChildren().clear();

        for (Card karta : kartyGracza) {
            StackPane kartaView = karta.getView();

            // Dodaj efekt hover
            kartaView.setOnMouseEntered(e -> {
                kartaView.setTranslateY(-15);
                kartaView.setStyle(kartaView.getStyle() +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0.5, 0, 5);");
            });

            kartaView.setOnMouseExited(e -> {
                kartaView.setTranslateY(0);
                kartaView.setStyle(kartaView.getStyle().replace(
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0.5, 0, 5);", ""));
            });

            // Obsługa kliknięcia - zagranie karty
            kartaView.setOnMouseClicked(e -> zagrajKarte(karta));

            rekaGracza.getChildren().add(kartaView);
        }
    }

    private void zagrajKarte(Card karta) {
        // Sprawdź czy karta pasuje (ten sam kolor lub wartość)
        if (karta.getKolor().equals(wierzchniaKarta.getKolor()) ||
                karta.getWartosc().equals(wierzchniaKarta.getWartosc())) {

            // Usuń starą kartę ze stołu
            stol.getChildren().clear();

            // Połóż nową kartę na stole
            wierzchniaKarta = karta;
            stol.getChildren().add(karta.getView());

            // Usuń kartę z ręki gracza
            kartyGracza.remove(karta);

            // Odśwież widok ręki
            wyswietlKartyGracza();

            // Sprawdź wygraną
            if (kartyGracza.isEmpty()) {
                instrukcja.setText("WYGRAŁEŚ!");
                instrukcja.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
            }
        } else {
            // Karta nie pasuje - animacja odrzucenia
            StackPane kartaView = karta.getView();
            kartaView.setTranslateX(-20);
            javafx.animation.Timeline anim = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.millis(100),
                            event -> kartaView.setTranslateX(0)
                    )
            );
            anim.play();
        }
    }
}