package application;

import application.controller.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.PrintWriter;
import java.net.Socket;

public class GameClient extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getClassLoader().getResource("mainUI.fxml"));
        Pane root = fxmlLoader.load();
        primaryStage.setTitle("Tic Tac Toe");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
        Controller controller = fxmlLoader.getController();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.runLater(() -> {
                    PrintWriter pw = controller.getOut();
                    Socket client  = controller.getClientSocket();
                    pw.println("Disconnected");
                    System.exit(1);
                });
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
