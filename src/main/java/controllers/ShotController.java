package controllers;

import application.Game;
import application.Shot;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

public class ShotController {

    public final BooleanProperty spacePressed = new SimpleBooleanProperty();
    private static final double SHOT_RADIUS = 4.;
    private static final int MAX_ACTIVE_SHOTS = 6;
    private final int SHOT_SPEED = 3;
    private Pane gamePane;
    private Queue<Shot> shots;
    private Rectangle tractor;
    private Game game;

    public ShotController(Game game) {
        this.game = game;
        tractor = game.myTractor;
        gamePane = game.gamePane;
        shots = new LinkedList<>();

        spacePressed.addListener((((observableValue, aBoolean, t1) -> {
            if (!aBoolean) timer.start();
            else timer.stop();
        })));
    }

    AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long timestamp) {
            if (spacePressed.get()) {
                spacePressed.set(false);
                shoot();
            }
        }
    };

    public void shoot() {
        if (shots.size() >= MAX_ACTIVE_SHOTS) {
            return;
        }

        Shot shot = new Shot(SHOT_RADIUS);
        shots.add(shot);

        // Place shot at the center of the tractor
        Bounds bounds = tractor.getBoundsInParent();
        shot.setLayoutX(bounds.getCenterX());
        shot.setLayoutY(bounds.getCenterY());
        shot.setRotate(tractor.getRotate());
        gamePane.getChildren().add(shot);

        shot.setTimer(updateShotTimer(shot));
        shot.getTimer().start();

        // Remove shot after 5s delay
        shot.setDelay(new PauseTransition(Duration.seconds(5)));
        shot.getDelay().setOnFinished(e -> {
            gamePane.getChildren().remove(shot);
            shot.getTimer().stop();
            shots.remove();
        });
        shot.getDelay().play();
    }

    private AnimationTimer updateShotTimer(Shot shot) {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateShot(shot);
            }
        };
        return timer;
    }

    public void updateShot(Shot shot) {
        double angle = shot.getRotate() * Math.PI / 180;
        double dX = Math.cos(angle) * SHOT_SPEED;
        double dY = Math.sin(angle) * SHOT_SPEED;
        shot.setLayoutX(shot.getLayoutX() + dX);
        shot.setLayoutY(shot.getLayoutY() + dY);

        // If shot leaves the area of the tractor it is active
        if (!shot.isActive() && !game.grid.isCollision(shot, tractor)) {
            shot.setActive(true);
        }

        // If shot hits a wall it is active
        if (game.grid.isWallCollisionHorizontal(shot)) {
            shot.setActive(true);
            shot.setRotate(invertAngleHorizontal(shot.getRotate()));
        }
        if (game.grid.isWallCollisionVertical(shot)) {
            shot.setActive(true);
            shot.setRotate(invertAngleVertical(shot.getRotate()));
        }

        // If a shot is active and it hits a tractor, ded
        // TODO: Need some form of list of all tractors, so shot can hit all players, not just the shooter.
        if (game.grid.isCollision(shot, tractor) && shot.isActive()) {
            shot.getDelay().stop();
            // TODO: Remove tractor too.
            gamePane.getChildren().remove(shot);
//            gamePane.getChildren().remove(movementController.tractor);
            shot.getTimer().stop();
            shots.remove(shot);
        }
    }

    private double invertAngleVertical(double angle) {
        return (-1 * angle + 180) % 360;
    }

    private double invertAngleHorizontal(double angle) {
        return (-1 * angle);
    }
}
