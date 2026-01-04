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
    private HBox rekaPrzeciwnika; // górny przeciwnik (można usunąć jeśli nie potrzebny)
    @FXML
    private HBox rekaLewego; // lewy przeciwnik
    @FXML
    private HBox rekaPrawego; // prawy przeciwnik
    @FXML
    private Label instrukcja;
    @FXML
    private Label labelGracz;
    @FXML
    private Label labelPrzeciwnik; // górny przeciwnik

    private Card wierzchniaKarta;
    private List<Card> kartyGracza;
    private int liczbaKartPrzeciwnika;
    private int liczbaKartLewego;
    private int liczbaKartPrawego;

    @FXML
    public void initialize() {
        rozpocznijGre();
    }

    private void rozpocznijGre() {
        kartyGracza = new ArrayList<>();
        liczbaKartPrzeciwnika = 7;
        liczbaKartLewego = 7;
        liczbaKartPrawego = 7;

        // Dodane byle jakie karty by potestować
        kartyGracza.add(new Card("CZERWONY", "7"));
        kartyGracza.add(new Card("NIEBIESKI", "3"));
        kartyGracza.add(new Card("ZIELONY", "5"));
        kartyGracza.add(new Card("ŻÓŁTY", "2"));
        kartyGracza.add(new Card("CZERWONY", "STOP"));
        kartyGracza.add(new Card("NIEBIESKI", "+2"));
        kartyGracza.add(new Card("ZIELONY", "8"));

        wierzchniaKarta = new Card("CZERWONY", "1");
        stol.getChildren().add(wierzchniaKarta.getView());

        wyswietlKartyGracza();
        wyswietlKartyPrzeciwnika();
        wyswietlKartyLewego();
        wyswietlKartyPrawego();

        // to pokazuje napis ile kto ma kart
        aktualizujLabele();
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
        StackPane backview = new StackPane();
        backview.setMinSize(80, 120);
        backview.setMaxSize(80, 120);

        var stream = getClass().getResourceAsStream("/assets/textures/card_back.png");
        if (stream == null) {
            throw new RuntimeException("Nie znaleziono pliku tekstury!");
        }
        Image texture = new Image(stream);
        if (texture.isError()) {
            throw new RuntimeException("Błąd ładowania tekstury!");
        }

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
        backview.getChildren().addAll(imageView, ramka);
        return backview;
    }

    private void aktualizujLabele() {
        labelGracz.setText("Twoje karty (" + kartyGracza.size() + ")");
        labelPrzeciwnik.setText("Przeciwnik - " + liczbaKartPrzeciwnika + " kart");
        // Możesz też dodać etykiety dla lewego i prawego jeśli chcesz
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
            aktualizujLabele();

            // Sprawdź wygraną
            if (kartyGracza.isEmpty()) {
                instrukcja.setText("WYGRAŁEŚ!");
                instrukcja.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
            } else {
                // Tura przeciwnika (górnego) - pozostaje bez zmian
                javafx.application.Platform.runLater(() -> turaPrzeciwnika());
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

    private void turaPrzeciwnika() {
        if (liczbaKartPrzeciwnika == 0) {
            instrukcja.setText("PRZECIWNIK WYGRAŁ!");
            instrukcja.setStyle("-fx-text-fill: red; -fx-font-size: 48px; -fx-font-weight: bold;");
            return;
        }

        Random random = new Random();
        if (random.nextBoolean() && liczbaKartPrzeciwnika > 0) {
            liczbaKartPrzeciwnika--;
            String[] kolory = {"CZERWONY", "NIEBIESKI", "ZIELONY", "ŻÓŁTY"};
            String[] wartosci = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "STOP", "+2"};
            Card nowaKarta = new Card(
                    kolory[random.nextInt(kolory.length)],
                    wartosci[random.nextInt(wartosci.length)]
            );
            stol.getChildren().clear();
            wierzchniaKarta = nowaKarta;
            stol.getChildren().add(nowaKarta.getView());
            instrukcja.setText("Przeciwnik zagrał!");
            javafx.animation.Timeline delay = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(1),
                            e -> instrukcja.setText("Twoja kolej!")
                    )
            );
            delay.play();
        } else {
            liczbaKartPrzeciwnika++;
            instrukcja.setText("Przeciwnik dobrał kartę");

            javafx.animation.Timeline delay = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(1),
                            e -> instrukcja.setText("Twoja kolej!")
                    )
            );
            delay.play();
        }

        wyswietlKartyPrzeciwnika();
        aktualizujLabele();
    }
}