package controllers;

import javafx.animation.AnimationTimer;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class MovementController {
    private final BooleanProperty upPressed = new SimpleBooleanProperty();
    private final BooleanProperty downPressed = new SimpleBooleanProperty();
    private final BooleanProperty leftPressed = new SimpleBooleanProperty();
    private final BooleanProperty rightPressed = new SimpleBooleanProperty();
    private final BooleanProperty spacePressed = new SimpleBooleanProperty();

    private final BooleanBinding keyPressed = upPressed.or(downPressed).or(leftPressed).or(rightPressed).or(spacePressed);

    @FXML
    private Rectangle player;

    @FXML
    private BorderPane scene;

    private static final double MOVEMENT_SPEED = 1.9, ROTATION_SPEED = 4.2;
    GameSceneController gameSceneController;

    public MovementController(GameSceneController gameSceneController) {
        this.gameSceneController = gameSceneController;
    }

    public void makeMovable(Rectangle player, BorderPane scene) {
        this.player = player;
        this.scene = scene;
        movementSetup();

        keyPressed.addListener(((observableValue, aBoolean, t1) -> {
            if (!aBoolean) timer.start();
            else timer.stop();
        }));
    }

    private boolean isCollision() {
        for (var wall : gameSceneController.walls) {
            Shape intersect = Shape.intersect(player, wall);

            if (intersect.getBoundsInParent().getWidth() > 0)
                return true;
        }

        return false;
    }

    AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long timestamp) {
            if (upPressed.get()) move("forwards");
            if (downPressed.get()) move("backwards");
            if (leftPressed.get()) rotate("counterclockwise");
            if (rightPressed.get()) rotate("clockwise");
        }
    };

    private void move(String dir) {
        double angle = player.getRotate() * Math.PI / 180;
        double dX = Math.cos(angle) * MOVEMENT_SPEED * (dir.equals("forwards") ? 1 : -1);
        double dY = Math.sin(angle) * MOVEMENT_SPEED * (dir.equals("forwards") ? 1 : -1);

        player.setLayoutX(player.getLayoutX() + dX);
        player.setLayoutY(player.getLayoutY() + dY);

        // naive collision detection - undo movement if colliding with wall
        if (isCollision()) {
            player.setLayoutX(player.getLayoutX() - dX);
            player.setLayoutY(player.getLayoutY() - dY);
        }
    }

    private void rotate(String dir) {
        double dAngle = ROTATION_SPEED * (dir.equals("clockwise") ? 1 : -1);
        player.setRotate(player.getRotate() + dAngle);

        if (isCollision())
            player.setRotate(player.getRotate() - dAngle); // undo rotation
    }

    private void movementSetup() {
        scene.setOnKeyPressed(e -> setButtonStates(e.getCode(), true));
        scene.setOnKeyReleased(e -> setButtonStates(e.getCode(), false));
    }

    private void setButtonStates(KeyCode key, boolean b) {
        if (key == KeyCode.UP) upPressed.set(b); // TODO: use primitive boolean datatype instead?
        if (key == KeyCode.DOWN) downPressed.set(b);
        if (key == KeyCode.LEFT) leftPressed.set(b);
        if (key == KeyCode.RIGHT) rightPressed.set(b);
        if (key == KeyCode.SPACE) spacePressed.set(b);
    }
}