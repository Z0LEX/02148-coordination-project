package application;

import controllers.LobbySceneController;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jspace.*;

import java.io.IOException;
import java.net.*;

public class GameApplication {

    private Scene startScene;
    public Scene lobbyScene;
    public static final int WINDOW_WIDTH = 960;
    public static final int WINDOW_HEIGHT = 540;
    public static final String HOST_IP = "192.168.1.107";
    public String name = "defaultName";
    SpaceRepository repository;
    SequentialSpace serverLobby;
    SequentialSpace serverRoom;
    RemoteSpace clientLobby;
    RemoteSpace clientRoom;

    public GameApplication(Stage stage) {
        makeStartScene(stage);
        makeLobbyScene(stage);
    }

    public void startGame(Stage stage) {
        showStartScene(stage);
        stage.show();
    }

    private void showStartScene(Stage stage) {
        stage.setScene(startScene);
        stage.centerOnScreen();
    }

    private void makeStartScene(Stage stage) {
        Label gameTitle = new Label("AZ");

        // Make buttons
        Button lobbyButton = new Button("Start lobby");
        lobbyButton.setPrefSize(150, 30);
        lobbyButton.setOnAction(e -> makeLobbyScene(stage));
        Button roomButton = new Button("Start room");
        roomButton.setPrefSize(150, 30);
        roomButton.setOnAction(e -> hostRoom(stage, HOST_IP));
        Button gameButton = new Button("Start game");
        gameButton.setPrefSize(150, 30);
        gameButton.setOnAction(e -> launchGame(stage));
        Button exitButton = new Button("Exit");
        exitButton.setPrefSize(150, 30);
        exitButton.setOnAction(e -> stage.close());

        // Make layout and insert buttons
        VBox startLayout = new VBox(20);
        startLayout.getChildren().addAll(gameTitle, lobbyButton, roomButton, gameButton, exitButton);
        startLayout.setAlignment(Pos.CENTER);
        startScene = new Scene(startLayout, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    public void joinRoom(Stage stage, String ip){
        try{
            System.out.println("Client joining room");
            String uri = "tcp://" + ip + ":9001/room?keep";
            clientRoom = new RemoteSpace(uri);
            clientRoom.put("name", name);
            new Room(stage, this, clientRoom);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void hostRoom(Stage stage, String ip) {
        try {
            System.out.println("Host is creating a new room");
            repository = new SpaceRepository();
            serverRoom = new SequentialSpace();

            repository.add("room", serverRoom);
            String uri = "tcp://" + ip + ":9001/?keep";
            String clientUri = "tcp://" + ip + ":9001/room?keep";
            repository.addGate(uri);

            serverRoom.put("turn", 1);
            serverRoom.put("players", 1);
            serverRoom.put("readers", 0);

            serverRoom.put("clientIp", ip);
            serverRoom.put(name, ip);
            serverRoom.put("name",name);
            new Room(stage, this, serverRoom);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void launchGame(Stage stage) {
        try {
            Game game;

            if (isHost()) {
                System.out.println("Host is creating a new game...");
                game = new Game(stage);

                repository = new SpaceRepository();
                serverLobby = new SequentialSpace();
                repository.add("lobby", serverLobby);
                String uri = "tcp://" + HOST_IP + ":9001/?keep";
                repository.addGate(uri);

                serverLobby.put("game", game); // TODO: game needs to be serialized
            } else {
                System.out.println("Client is getting existing game...");

                String uri = "tcp://" + HOST_IP + ":9001/lobby?keep";
                clientLobby = new RemoteSpace(uri);

                game = (Game) clientLobby.query(new ActualField("game"), new FormalField(Game.class))[1];
            }

            stage.setScene(game.gameScene);
            Player player = new Player(game);
            game.addPlayer(player);
            game.gameScene.getRoot().requestFocus();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isHost() {
        String ip;

        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return ip.equals(HOST_IP);
    }

    private void makeLobbyScene(Stage stage) {
        try {
            new LobbyConnector(stage, this, "192.168.1.107", name);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
