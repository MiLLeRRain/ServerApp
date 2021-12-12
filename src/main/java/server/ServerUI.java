package server;


import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.ServicedServer;

public class ServerUI extends Application {

    AnchorPane root =  new AnchorPane();
    ServicedServer ss;
    TextArea tf;
    public TextArea statusPanel;
    public static String publicKey = "";
    public VBox chatters;

    @Override
    public void start(Stage stage) throws Exception {
        // build UI
        tf = new TextArea();
        tf.setPrefSize(200, 30);
        tf.setText("server status");

        root.setStyle("-fx-background-color: #fedc32");

        Button stop = new Button("Stop");
        stop.setOnAction(actionEvent -> stopThread());
        Button start = new Button("Start");
        start.setOnAction(actionEvent -> startThread());
        Button reset = new Button("Reset");
        reset.setOnAction(actionEvent -> resetThread());
        ButtonBar bb = new ButtonBar();
        bb.getButtons().addAll(stop, start, reset);

        HBox hb2 = new HBox();
        hb2.setSpacing(5);
        hb2.setAlignment(Pos.CENTER_LEFT);
        hb2.setPrefSize(500, 30);

        hb2.getChildren().addAll(tf, bb);

        root.getChildren().add(hb2);
        root.setLeftAnchor(hb2, 10.0);
        root.setTopAnchor(hb2, 50.0);


        VBox v2 = new VBox();
        v2.setSpacing(10);
        Label statusLbl = new Label("Connection notice: ");
        statusPanel = new TextArea();
        statusPanel.setPrefSize(500,30);

        v2.getChildren().addAll(statusLbl, statusPanel);

        root.getChildren().add(v2);
        root.setLeftAnchor(v2, 10.0);
        root.setTopAnchor(v2, 110.0);


        VBox v1 = new VBox();
        Label chattersLbl = new Label("Connected Clients:");
        v1.setSpacing(10);

        chatters = new VBox();
        chatters.setStyle("-fx-background-color: #FFFFFF");
        chatters.setPrefSize(500,200);
        chatters.setPadding(new Insets(10));
        chatters.setSpacing(10);
        v1.getChildren().addAll(chattersLbl, chatters);

        root.getChildren().add(v1);
        root.setLeftAnchor(v1, 10.0);
        root.setTopAnchor(v1, 200.0);



        Scene scene =  new Scene(root);
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(800);

        stage.show();

    }

    /**
     * Start Service
     */
    private void startThread() {
        if (ss != null) return;
        this.ss = new ServicedServer("CommSimulator", this);
        ss.start();
        tf.setText(ss.getState().toString());
        ss.stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State t1) {
                tf.setText(t1.toString());
            }
        });
        System.out.println("started");
    }

    private void stopThread() {
        if (ss == null) return;
        ss.cancel();
        System.out.println("canceled");
    }

    private void resetThread() {
        if (ss == null) return;
        ss.reset();
        System.out.println("reset");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
