package application;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class GameSever {
    private ServerSocket serverSocket = null;
    private Map<Integer, Client> clientsMap; //(port, Client)
    private List<ChessBoard> chessBoardList;
    public static final int EMPTY = 0;


    public GameSever() throws IOException {
        this.serverSocket = new ServerSocket(8888);
        this.clientsMap = new HashMap<>();
        this.chessBoardList = new ArrayList<>();
        Thread endThread = new Thread(() -> {
            try {
                System.out.println("Server will exit");
                for(Integer key: clientsMap.keySet()) {
                    PrintWriter pw = new PrintWriter(new BufferedWriter
                            (new OutputStreamWriter(clientsMap.get(key).socket.getOutputStream())), true);
                    pw.println("The Server is disconnected");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        Runtime.getRuntime().addShutdownHook(endThread);
    }

    public void begin(){
        System.out.println("Server start!");
        try {
            while (true) {
                Socket clientSocket = this.serverSocket.accept();
                int clientPort = clientSocket.getPort();
                System.out.println("CONNECT a new client! Port: " + clientPort);
                int oppoPort = match(clientPort); //
                Client client = new Client(clientSocket, oppoPort);
                clientsMap.put(clientPort, client);
                new Thread(new ClientThread(client)).start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int match(int clientPort) {
        for(Integer port: clientsMap.keySet()) {
            if (clientsMap.get(port).oppoPort == -1) {
                clientsMap.get(port).setOppoPort(clientPort);
                clientsMap.get(port).setTurn(1);
                clientsMap.get(port).setTictoe("X");
                System.out.println("Player " + clientPort + " and Player " + port + " match!!!");
                this.chessBoardList.add(new ChessBoard(clientPort, port));
                return port;
            }
        }
        return -1;
    }

    class ClientThread implements Runnable{
        private Client client;
        private BufferedReader in;
        private PrintWriter out;
        public ClientThread(Client client) throws IOException {
            this.client = client;
            this.in = new BufferedReader(new InputStreamReader(client.socket.getInputStream()));
            this.out = new PrintWriter(new BufferedWriter
                    (new OutputStreamWriter(client.socket.getOutputStream())), true);
        }

        @Override
        public void run(){
            try {
                while (true) {
                    String line;
                    if((line = this.in.readLine())!=null){ // Receive message (x,y) from client
                        System.out.println("Server receive message: " + line
                                + " from port " + this.client.socket.getPort()
                                + " It's turn is: " + this.client.turn
                                + " It's opponent is: " + this.client.oppoPort);
                        if (line.contains("Disconnected")) {
                            System.out.println("The client " + client.socket.getPort() + " is disconnected");
                            if (this.client.oppoPort != -1) {
                                Client oppo = clientsMap.get(this.client.oppoPort);
                                PrintWriter pw = new PrintWriter(new BufferedWriter
                                        (new OutputStreamWriter(oppo.socket.getOutputStream())), true);
                                pw.println("Your opponent is disconnected!");
                            }
                            continue;
                        }
                        if (this.client.oppoPort == -1) {
                            this.out.println("Please wait for another player");
                        }else {
                            if (this.client.turn == 1) {
                                Client oppo = clientsMap.get(this.client.oppoPort);
                                String[] strs = line.split(",");
                                int x = Integer.parseInt(strs[0]);
                                int y = Integer.parseInt(strs[1]);
                                String message = line + "," + this.client.tictoe;
                                // Find the chessboard of (oppoport, port)
                                ChessBoard curChessBoard = null;
                                for(ChessBoard chessBoard : chessBoardList) {
                                    if (chessBoard.port1 == this.client.oppoPort || chessBoard.port2 == this.client.oppoPort) {
                                        curChessBoard = chessBoard;
                                        break;
                                    }
                                }
                                assert curChessBoard != null;
                                if (curChessBoard.chessboard[x][y] != 0){
                                    System.out.println("Repeated Position: (" + x + "," + y +")");
                                    continue;
                                }

                                curChessBoard.chessboard[x][y] = (Objects.equals(this.client.tictoe, "X"))? -1 : 1;
                                //broadcast
                                PrintWriter pw = new PrintWriter(new BufferedWriter
                                        (new OutputStreamWriter(oppo.socket.getOutputStream())), true);
                                this.client.setTurn(0);
                                oppo.setTurn(1);
                                this.out.println(message);
                                pw.println(message);
                                if (curChessBoard.terminal()) {
                                    this.client.setTurn(0);
                                    oppo.setTurn(0);
                                    if (curChessBoard.winner == 0){
                                        this.out.println("Game Over: Tie");
                                        pw.println("Game Over: Tie");
                                    }else {
                                        this.out.println("Game Over: You Win");
                                        pw.println("Game Over: You Lose");
                                    }
                                }
                            }
                        }

                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    class Client {
        Socket socket;
        int oppoPort;
        int turn;
        String tictoe;
        public Client(Socket socket, int oppoPort) {
            this.socket = socket;
            this.oppoPort = oppoPort;
            this.turn = 0;
            this.tictoe = "O";
        }

        public void setOppoPort(int oppoPort) {
            this.oppoPort = oppoPort;
        }

        public void setTurn(int turn) {
            this.turn = turn;
        }

        public void setTictoe(String tictoe) {
            this.tictoe = tictoe;
        }
    }

    class ChessBoard {
        public int[][] chessboard;
        public int port1;
        public int port2;

        public int winner;

        public ChessBoard(int port1, int port2) {
            this.port1 = port1;
            this.port2 = port2;
            this.chessboard = new int[3][3];
            this.winner = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    this.chessboard[i][j] = EMPTY;
                }
            }
        }

        public boolean terminal() {
            for (int i = 0; i < 3; i++) {
                if (this.chessboard[i][0] != EMPTY
                        && this.chessboard[i][0] == this.chessboard[i][1]
                        && this.chessboard[i][1] == this.chessboard[i][2]) {
                    this.winner = this.chessboard[i][0];
                    return true;
                }
            }
            for (int j = 0; j < 3; j++) {
                if (this.chessboard[0][j] != EMPTY
                        && this.chessboard[0][j] == this.chessboard[1][j]
                        && this.chessboard[1][j] == this.chessboard[2][j]) {
                    this.winner = this.chessboard[0][j];
                    return true;
                }
            }

            if (this.chessboard[0][0] != EMPTY
                    && this.chessboard[0][0] == this.chessboard[1][1]
                    && this.chessboard[1][1] == this.chessboard[2][2]) {
                this.winner = this.chessboard[0][0];
                return true;
            }

            if (this.chessboard[0][2] != EMPTY
                    && this.chessboard[0][2] == this.chessboard[1][1]
                    && this.chessboard[1][1] == this.chessboard[2][0]) {
                this.winner = this.chessboard[0][2];
                return true;
            }

            int tmp = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (this.chessboard[i][j] != 0) {
                        tmp ++;
                    }
                }
            }
            if (tmp == 9) {
                this.winner = 0;
                return true;
            }

            return false;
        }
    }


    public static void main(String[] args) throws IOException {
        try {
            GameSever gameSever = new GameSever();
            gameSever.begin();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
