package application;

import controllers.ChatBoxViewController;
import controllers.RoomSceneViewController;
import datatypes.ArrayListInt;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Room {

    private String chatFileName = "/ChatboxView.fxml";
    private String roomFileName = "/room-scene-view.fxml";

    private Scene roomScene;

    private FXMLLoader chatboxLoader;
    private FXMLLoader roomLoader;
    private RoomSceneViewController roomController;
    private Space space;
    private String name;
    private String uri;
    private int playerId;
    private int players = 0;
    private ArrayList<String> playerNames;
    private ArrayListInt playerIds;
    private String hostName;

    public Room(Stage stage, GameApplication application, Space space) {
        this.space = space;
        playerNames = new ArrayList();
        playerIds = new ArrayListInt();
        try {
            uri = (String) space.query(new ActualField("clientUri"), new FormalField(String.class))[1];
            hostName = (String) space.query(new ActualField("host name"), new FormalField(String.class))[1];
            name = (String) space.get(new ActualField("name"), new FormalField(String.class))[1];
            playerId = (int) space.get(new ActualField("player id"), new FormalField(Integer.class))[1];

            initializePlayerNames(space);

            initializePlayerIds(space);

            roomLoader = new FXMLLoader(RoomSceneViewController.class.getResource(roomFileName));
            chatboxLoader = new FXMLLoader(ChatBoxViewController.class.getResource(chatFileName));

            populateChatBoxConstructor(uri, playerNames.size(), name);

            setupRoomLayout(stage, application);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        stage.setScene(roomScene);

    }

    private void initializePlayerIds(Space space) throws InterruptedException {
        Object[] listOfPlayerIds = space.getp(new ActualField("playerIdList"), new FormalField(ArrayListInt.class));
        if (listOfPlayerIds != null) {
            playerIds = (ArrayListInt) listOfPlayerIds[1];
        }
        playerIds.add(playerId);
        space.put("playerIdList", playerIds);
    }

    private void initializePlayerNames(Space space) throws InterruptedException {
        Object[] playerNameLists = space.getp(new ActualField("playerNameList"), new FormalField(ArrayList.class));
        if (playerNameLists != null) {
            playerNames = (ArrayList<String>) playerNameLists[1];
        }
        playerNames.add(name);
        space.put("playerNameList", playerNames);
    }


    private void setupRoomLayout(Stage stage, GameApplication application) throws IOException {
        BorderPane chatbox = chatboxLoader.load();
        BorderPane roomLayout = roomLoader.load();

        roomController = roomLoader.getController();

        Button lobbyButton = (Button) roomLayout.lookup("#lobbyButton");
        Button startGameButton = (Button) roomLayout.lookup("#startGameButton");
        lobbyButton.setOnAction(e -> {
            try {
                ArrayList<String> playerNames = (ArrayList<String>) space.get(new ActualField("playerNameList"), new FormalField(ArrayList.class))[1];
                playerNames.remove(name);
                space.put("playerNameList", playerNames);
                // TODO: switch to lobby scene
                stage.setScene(GameApplication.startScene);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        startGameButton.setOnAction(e -> application.launchGame(stage));
        roomController.setRoomNameText(hostName);

        roomLayout.setRight(chatbox);
        roomScene = new Scene(roomLayout, application.WINDOW_WIDTH, application.WINDOW_HEIGHT);

        // After we've set up the scene we start the listener thread to update the ListView when newp players join
        new Thread(new RoomListener(space, roomController)).start();
    }

    private void populateChatBoxConstructor(String uri, int players, String name) {
        ArrayList arrayData = new ArrayList();
        arrayData.add(uri);
        arrayData.add(players);
        arrayData.add(name);

        ObservableList<String> data = FXCollections.observableArrayList(arrayData);
        chatboxLoader.setControllerFactory(new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> param) {
                if (param == ChatBoxViewController.class) {
                    return new ChatBoxViewController(data);
                } else
                    try {
                        return param.newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
            }
        });
    }
}

class RoomListener implements Runnable {
    Space space;
    RoomSceneViewController roomController;
    ArrayList<String> playerNames = new ArrayList<>();

    public RoomListener(Space space, RoomSceneViewController roomController) {
        this.space = space;
        this.roomController = roomController;
    }

    @Override
    public void run() {
        while (true) {
            try {
                ArrayList<String> newPlayerNames = (ArrayList<String>) space.query(new ActualField("playerNameList"), new FormalField(ArrayList.class))[1];

                // Update list of player names if the two lists are different
                if (!playerNames.equals(newPlayerNames)) {
                    playerNames = newPlayerNames;
                    Platform.runLater(() -> {
                        roomController.updatePlayerList(newPlayerNames);
                    });
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
