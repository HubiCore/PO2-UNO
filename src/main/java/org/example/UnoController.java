package org.example;

import javafx.fxml.FXML;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class UnoController {
    @FXML
    private StackPane stol;
    @FXML
    private HBox rekaGracza;
    @FXML
    private HBox rekaPrzeciwnika;
    @FXML
    private HBox rekaLewego;
    @FXML
    private HBox rekaPrawego;
    @FXML
    private Label instrukcja;
    @FXML
    private Label labelGracz;
    @FXML
    private Label labelPrzeciwnik;

    private Card wierzchniaKarta;
    private List<Card> kartyGracza;
    private int liczbaKartPrzeciwnika;
    private int liczbaKartLewego;
    private int liczbaKartPrawego;
    private Random random;

    @FXML
    public void initialize() {
        random = new Random();
        rozpocznijGre();
    }

    private void rozpocznijGre() {
        kartyGracza = new ArrayList<>();
        liczbaKartPrzeciwnika = 7;
        liczbaKartLewego = 7;
        liczbaKartPrawego = 7;

        kartyGracza.add(new Card("RED", "7"));
        kartyGracza.add(new Card("BLUE", "3"));
        kartyGracza.add(new Card("GREEN", "5"));
        kartyGracza.add(new Card("YELLOW", "2"));
        kartyGracza.add(new Card("RED", "STOP"));
        kartyGracza.add(new Card("BLUE", "+2"));
        kartyGracza.add(new Card("GREEN", "8"));

        wierzchniaKarta = new Card("RED", "1");
        stol.getChildren().add(wierzchniaKarta.getView());

        wyswietlKartyGracza();
        wyswietlKartyPrzeciwnika();
        wyswietlKartyLewego();
        wyswietlKartyPrawego();

        aktualizujLabele();
        instrukcja.setText("Twoja kolej!");
    }

    private void wyswietlKartyGracza() {
        rekaGracza.getChildren().clear();

        for (Card karta : kartyGracza) {
            StackPane kartaView = karta.getView();

            kartaView.setOnMouseEntered(e -> {
                kartaView.setTranslateY(-15);
                kartaView.setStyle(kartaView.getStyle() +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0.5, 0, 5);");
            });

            kartaView.setOnMouseExited(e -> {
                kartaView.setTranslateY(0);
                kartaView.setStyle("-fx-cursor: hand; -fx-effect: none;");
            });

            kartaView.setOnMouseClicked(e -> zagrajKarte(karta));

            rekaGracza.getChildren().add(kartaView);
        }
    }

    private void wyswietlKartyPrzeciwnika() {
        rekaPrzeciwnika.getChildren().clear();
        for (int i = 0; i < liczbaKartPrzeciwnika; i++) {
            StackPane kartaback = createReverseCard();
            rekaPrzeciwnika.getChildren().add(kartaback);
        }
    }

    private void wyswietlKartyLewego() {
        rekaLewego.getChildren().clear();
        for (int i = 0; i < liczbaKartLewego; i++) {
            StackPane kartaback = createReverseCard();
            rekaLewego.getChildren().add(kartaback);
        }
    }

    private void wyswietlKartyPrawego() {
        rekaPrawego.getChildren().clear();
        for (int i = 0; i < liczbaKartPrawego; i++) {
            StackPane kartaback = createReverseCard();
            rekaPrawego.getChildren().add(kartaback);
        }
    }

    private StackPane createReverseCard() {
        Card dummyCard = new Card("RED", "0");
        return dummyCard.getBackView();
    }

    private void aktualizujLabele() {
        labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
        labelPrzeciwnik.setText("Przeciwnik - " + liczbaKartPrzeciwnika + " kart");
    }

    private void zagrajKarte(Card karta) {
        // Użyjemy metody canPlayOn z klasy Card
        if (karta.canPlayOn(wierzchniaKarta)) {
            stol.getChildren().clear();
            wierzchniaKarta = karta;
            stol.getChildren().add(karta.getView());

            kartyGracza.remove(karta);

            wyswietlKartyGracza();
            aktualizujLabele();

            if (kartyGracza.isEmpty()) {
                instrukcja.setText("WYGRAŁEŚ!");
                instrukcja.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
                zablokujKarty();
            } else {
            }
        } else {
            StackPane kartaView = karta.getView();
            kartaView.setTranslateX(-20);
            javafx.animation.Timeline anim = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.millis(100),
                            event -> kartaView.setTranslateX(0)
                    )
            );
            anim.setCycleCount(2);
            anim.setAutoReverse(true);
            anim.play();

            javafx.animation.Timeline delay = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(1),
                            e -> {
                                instrukcja.setText("Twoja kolej!");
                            }
                    )
            );
            delay.play();
        }
    }

    private void zablokujKarty() {
        for (var child : rekaGracza.getChildren()) {
            child.setDisable(true);
            child.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
        }
    }
}