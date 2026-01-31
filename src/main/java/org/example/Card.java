package org.example;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class Card {
    private String color;
    private String value;
    private StackPane view;
    private StackPane backView;

    public Card(String color, String value) {
        this.color = color;
        this.value = value;
        createView();
        createBackView();
    }

    public static Card fromString(String cardStr) {
        String[] parts = cardStr.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Nieprawidłowy format karty: " + cardStr);
        }
        return new Card(parts[0], parts[1]);
    }

    @Override
    public String toString() {
        return color + ":" + value;
    }

    private void createView() {
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

        } catch (Exception e) {
            System.out.println("Texture not found, using CSS pattern. Error: " + e.getMessage());

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
    }

    private void createBackView() {
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

        } catch (Exception e) {
            System.out.println("Back texture not found, using default pattern. Error: " + e.getMessage());

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
    }

    private Color getColorFill() {
        switch (color) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            default: return Color.BLACK;
        }
    }

    private String getColorHex() {
        switch (color) {
            case "RED": return "#E53935";
            case "BLUE": return "#1E88E5";
            case "GREEN": return "#43A047";
            case "YELLOW": return "#FDD835";
            default: return "#000000";
        }
    }

    public StackPane getView() {
        return view;
    }

    public StackPane getBackView() {
        return backView;
    }

    public String getColor() {
        return color;
    }

    public String getValue() {
        return value;
    }

    public boolean canPlayOn(Card other) {
        return this.color.equals(other.getColor()) ||
                this.value.equals(other.getValue());
    }
}