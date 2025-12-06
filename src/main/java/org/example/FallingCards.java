package org.example;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.Random;
//Boże ale to będzie spierdolone
//D:
//nie robie i chuj, kiedyś zrobie

public class FallingCards {
    @FXML
    private Pane gamePane;

    private ArrayList<FallingCards> objects = new ArrayList<>();
    private Random random = new Random();
    private AnimationTimer timer;
    private long lastSpawn = 0;
}
/*
    public static void start_animation() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (now - lastSpawn > 500_000) { //jakis czas, chyba 5ms
                    spawnObject();
                    lastSpawn = now;
                }
            }
        };

    }
}
*/