package application.controller;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;


public class Controller implements Initializable {
    private static final int EMPTY = 0;
    private static final int BOUND = 90;
    private static final int OFFSET = 15;

    @FXML
    private Pane base_square;

    @FXML
    private Rectangle game_panel;

    @FXML
    private Label result_label;

    private static boolean TURN = false;

    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public Socket getClientSocket() {
        return clientSocket;
    }

    public BufferedReader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("new Player");
        try {
            this.clientSocket = new Socket("127.0.0.1", 8888);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        game_panel.setOnMouseClicked(event -> {
            int x = (int) (event.getX() / BOUND);
            int y = (int) (event.getY() / BOUND);
            out.println(x + "," + y);
        });
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String line = null;
                    while (true) {
                        if((line = in.readLine()) != null){
                            System.out.println("Client "
                                    + clientSocket.getPort()
                                    +" receive message: " + line);
                            if (line.contains("Game Over")) {
                                String finalMessage = line;
                                Platform.runLater(() -> {
                                    gameOver(finalMessage);
                                });
                            }else if (line.equals("Please wait for another player")) {
                                System.out.println(line);
                            }else if (line.contains("disconnected")) {
                                System.out.println(line);
                            }else {
                                String[] strs = line.split(",");
                                int x = Integer.parseInt(strs[0]);
                                int y = Integer.parseInt(strs[1]);
                                int v = (Objects.equals(strs[2], "X"))? -1:1;
                                Platform.runLater(() -> {
                                    drawChess(x, y, v);
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    public void gameOver(String message) {
//        Stage stage = new Stage();
//        stage.initModality(Modality.APPLICATION_MODAL);
//        Label label = new Label();
//        label.setText(message);
//        Button btn1 = new Button("OK");
//        btn1.setOnMouseClicked(event -> {
//            stage.close();
//        });
//        VBox vBox = new VBox();
//        vBox.getChildren().addAll(label,btn1);
//        vBox.setAlignment(Pos.CENTER);
//        Scene scene = new Scene(vBox,200,200);
//        stage.setScene(scene);
//        stage.setX(game_panel.getTranslateX());
//        stage.setY(game_panel.getTranslateY());
//        stage.showAndWait();
        this.result_label.setText(message);
    }

    public void drawChess(int x, int y, int v) {
        switch (v) {
            case 1:
                drawCircle(x, y);
                break;
            case -1:
                drawLine(x, y);
                break;
            case EMPTY:
                break;
            default:
                System.err.println("Invalid value!");
        }
    }

    public void drawCircle(int i, int j) {
        Circle circle = new Circle();
        base_square.getChildren().add(circle);
        circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
        circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
        circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
        circle.setStroke(Color.RED);
        circle.setFill(Color.TRANSPARENT);
        flag[i][j] = true;
    }

    public void drawLine(int i, int j) {
        Line line_a = new Line();
        Line line_b = new Line();
        base_square.getChildren().add(line_a);
        base_square.getChildren().add(line_b);
        line_a.setStartX(i * BOUND + OFFSET * 1.5);
        line_a.setStartY(j * BOUND + OFFSET * 1.5);
        line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
        line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_a.setStroke(Color.BLUE);

        line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
        line_b.setStartY(j * BOUND + OFFSET * 1.5);
        line_b.setEndX(i * BOUND + OFFSET * 1.5);
        line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_b.setStroke(Color.BLUE);
        flag[i][j] = true;
    }

}
