package org.example;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import java.util.Random;

/**
 * Klasa reprezentująca kartę do gry UNO.
 * Zawiera informacje o kolorze i wartości karty, a także generuje jej widok graficzny.
 * Klasa dostarcza metody do tworzenia kart z ciągów znaków, generowania losowych kart
 * oraz sprawdzania zgodności kart podczas rozgrywki.
 *
 * @see StackPane
 * @see Color
 */
public class Card {
    private String color;
    private String value;
    private StackPane view;
    private StackPane backView;
    private static final Logger logger = Logger.getInstance();

    /** Dostępne kolory kart UNO */
    private static final String[] COLORS = {"RED", "GREEN", "BLUE", "YELLOW"};

    /** Dostępne wartości kart UNO */
    private static final String[] VALUES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "+2", "⏸", "↺"};

    /**
     * Konstruuje nową kartę o podanym kolorze i wartości.
     * Automatycznie tworzy widok graficzny karty (przód i tył).
     *
     * @param color kolor karty (RED, GREEN, BLUE, YELLOW)
     * @param value wartość karty (0-9, +2, ⏸, ↺)
     */
    public Card(String color, String value) {
        this.color = color;
        this.value = value;
        logger.debug("Tworzenie karty: " + color + ":" + value);
        createView();
        createBackView();
    }

    /**
     * Tworzy obiekt Card z ciągu znaków w formacie "kolor:wartość".
     *
     * @param cardStr ciąg znaków reprezentujący kartę (np. "RED:5")
     * @return nowy obiekt Card
     * @throws IllegalArgumentException jeśli format ciągu jest nieprawidłowy
     */
    public static Card fromString(String cardStr) {
        try {
            String[] parts = cardStr.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Nieprawidłowy format karty: " + cardStr);
            }
            Card card = new Card(parts[0], parts[1]);
            logger.debug("Utworzono kartę z ciągu: " + cardStr);
            return card;
        } catch (Exception e) {
            logger.error("Błąd tworzenia karty z ciągu: " + cardStr);
            throw e;
        }
    }

    /**
     * Generuje losową kartę UNO.
     * Wybiera losowy kolor z dostępnych i losową wartość.
     *
     * @return nowa losowo wygenerowana karta
     */
    public static Card generateRandomCard() {
        Random random = new Random();
        String color = COLORS[random.nextInt(COLORS.length)];
        String value = VALUES[random.nextInt(VALUES.length)];
        logger.debug("Wygenerowano losową kartę: " + color + ":" + value);
        return new Card(color, value);
    }

    /**
     * Zwraca reprezentację tekstową karty w formacie "kolor:wartość".
     *
     * @return ciąg znaków reprezentujący kartę
     */
    @Override
    public String toString() {
        return color + ":" + value;
    }

    /**
     * Tworzy widok graficzny przodu karty.
     * Próbuje załadować teksturę z pliku, a jeśli się nie uda, używa stylu CSS.
     * Widok zawiera kolorowe tło, ramkę i etykietę z wartością karty.
     */
    private void createView() {
        try {
            view = new StackPane();
            view.setMinSize(80, 120);
            view.setMaxSize(80, 120);
            try {
                var stream = getClass().getResourceAsStream("/assets/textures/card_front.png");
                if (stream == null) {
                    throw new RuntimeException("Texture file not found!");
                }
                Image texture = new Image(stream);

                if (texture.isError()) {
                    throw new RuntimeException("Error loading texture!");
                }

                ImageView imageView = new ImageView(texture);
                imageView.setFitWidth(80);
                imageView.setFitHeight(120);
                imageView.setPreserveRatio(false);

                Rectangle clip = new Rectangle(80, 120);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                imageView.setClip(clip);

                Rectangle frame = new Rectangle(80, 120);
                frame.setArcWidth(10);
                frame.setArcHeight(10);
                frame.setFill(Color.TRANSPARENT);
                frame.setStroke(Color.BLACK);
                frame.setStrokeWidth(2);

                Rectangle colorOverlay = new Rectangle(80, 120);
                colorOverlay.setArcWidth(10);
                colorOverlay.setArcHeight(10);
                colorOverlay.setFill(getColorFill());
                colorOverlay.setOpacity(0.6);

                Label valueLabel = new Label(value);
                valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, black, 2, 1.0, 0, 0);");

                view.getChildren().addAll(imageView, colorOverlay, frame, valueLabel);
                logger.debug("Utworzono widok karty z teksturą: " + color + ":" + value);

            } catch (Exception e) {
                logger.warning("Texture not found, using CSS pattern. Error: " + e.getMessage());

                Region background = new Region();
                background.setMinSize(80, 120);
                background.setMaxSize(80, 120);
                background.setStyle(
                        "-fx-background-color: " + getColorHex() + ";" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: black;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-image: repeating-linear-gradient(" +
                                "45deg, transparent, transparent 10px, " +
                                "rgba(255,255,255,0.15) 10px, rgba(255,255,255,0.15) 20px);"
                );

                Label valueLabel = new Label(value);
                valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, black, 2, 1.0, 0, 0);");

                view.getChildren().addAll(background, valueLabel);
            }
            view.setStyle("-fx-cursor: hand;");
        } catch (Exception e) {
            logger.error(e, "Błąd tworzenia widoku karty");
        }
    }

    /**
     * Tworzy widok graficzny tyłu karty (rewers).
     * Próbuje załadować teksturę z pliku, a jeśli się nie uda, używa domyślnego wzoru.
     */
    private void createBackView() {
        try {
            backView = new StackPane();
            backView.setMinSize(80, 120);
            backView.setMaxSize(80, 120);

            try {
                var stream = getClass().getResourceAsStream("/assets/textures/card_back.png");
                if (stream == null) {
                    throw new RuntimeException("Back texture file not found!");
                }
                Image texture = new Image(stream);

                if (texture.isError()) {
                    throw new RuntimeException("Error loading back texture!");
                }

                ImageView imageView = new ImageView(texture);
                imageView.setFitWidth(80);
                imageView.setFitHeight(120);
                imageView.setPreserveRatio(false);

                Rectangle clip = new Rectangle(80, 120);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                imageView.setClip(clip);

                Rectangle frame = new Rectangle(80, 120);
                frame.setArcWidth(10);
                frame.setArcHeight(10);
                frame.setFill(Color.TRANSPARENT);
                frame.setStroke(Color.BLACK);
                frame.setStrokeWidth(2);

                backView.getChildren().addAll(imageView, frame);
                backView.setStyle("-fx-cursor: default;");
                logger.debug("Utworzono widok tyłu karty z teksturą");

            } catch (Exception e) {
                logger.warning("Back texture not found, using default pattern. Error: " + e.getMessage());

                Region background = new Region();
                background.setMinSize(80, 120);
                background.setMaxSize(80, 120);
                background.setStyle(
                        "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #1a1a1a, #333333);" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: black;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-image: repeating-linear-gradient(" +
                                "45deg, transparent, transparent 10px, " +
                                "rgba(255,255,255,0.1) 10px, rgba(255,255,255,0.1) 20px);"
                );

                backView.getChildren().add(background);
            }
        } catch (Exception e) {
            logger.error(e, "Błąd tworzenia widoku tyłu karty");
        }
    }

    /**
     * Zwraca obiekt Color odpowiadający kolorowi karty.
     *
     * @return kolor JavaFX dla danej karty
     */
    private Color getColorFill() {
        switch (color) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            default: return Color.BLACK;
        }
    }

    /**
     * Zwraca reprezentację koloru w formacie heksadecymalnym.
     *
     * @return ciąg znaków z kodem koloru HEX
     */
    private String getColorHex() {
        switch (color) {
            case "RED": return "#E53935";
            case "BLUE": return "#1E88E5";
            case "GREEN": return "#43A047";
            case "YELLOW": return "#FDD835";
            default: return "#000000";
        }
    }

    /**
     * Zwraca widok graficzny przodu karty.
     *
     * @return StackPane zawierający graficzną reprezentację karty
     */
    public StackPane getView() {
        return view;
    }

    /**
     * Zwraca widok graficzny tyłu karty.
     *
     * @return StackPane zawierający graficzną reprezentację rewersu karty
     */
    public StackPane getBackView() {
        return backView;
    }

    /**
     * Zwraca kolor karty.
     *
     * @return kolor karty jako String
     */
    public String getColor() {
        return color;
    }

    /**
     * Zwraca wartość karty.
     *
     * @return wartość karty jako String
     */
    public String getValue() {
        return value;
    }

    /**
     * Sprawdza, czy ta karta może być położona na podanej karcie
     * zgodnie z zasadami UNO (ten sam kolor lub ta sama wartość).
     *
     * @param other karta, na której chcemy położyć obecną kartę
     * @return true jeśli karty są kompatybilne, false w przeciwnym razie
     */
    public boolean canPlayOn(Card other) {
        boolean canPlay = this.color.equals(other.getColor()) ||
                this.value.equals(other.getValue());
        logger.debug("Sprawdzanie kompatybilności kart: " + this + " na " + other + " -> " + canPlay);
        return canPlay;
    }
}