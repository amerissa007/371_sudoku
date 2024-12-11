import java.net.*;
import java.io.*;
import java.util.*;



public class Server {
    private static Sudoku sudoku = new Sudoku();
    private static HashSet<PrintWriter> clientWriters = new HashSet<>();
    private static boolean gameOver = false;
    private static Map<String, Integer> usersMove = new HashMap<>();

    public static void main(String[] args) throws IOException {

        System.out.println("Starting server...");

        if (args.length != 1) {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            sudoku.fillValues();
            System.out.println("Initial Board:\n" + sudoku.getSudokuString());
            System.out.println("Server started. Welcome to Sudoku!");

            while (!gameOver) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String uuid = UUID.randomUUID().toString();
                out.println("Welcome to Sudoku! Your unique ID is: " + uuid);
                out.println("Valid commands:");
                out.println("1. show - View the current Sudoku board.");
                out.println("2. update <row> <col> <num> - Update the board (row and col: 0-8, num: 1-9).");
                out.println("3. disconnect - Disconnect from the server.");
                out.println("Your command: ");
                System.out.println("UUID:" + uuid + " has connected.");


                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                new Thread(new ClientHandler(clientSocket, in, out, sudoku, uuid)).start();
            }
            System.out.println("Game Over! Closing the server...");
            serverSocket.close();
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

    private static void broadcastBoard() {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(sudoku.getSudokuString());
            }
        }
    }

    private static void removeClient(PrintWriter out) {
        synchronized (clientWriters) {
            clientWriters.remove(out);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private Sudoku sudoku;
        private String uuid;

        public ClientHandler(Socket clientSocket, BufferedReader in, PrintWriter out, Sudoku sudoku, String uuid) {
            this.clientSocket = clientSocket;
            this.uuid = uuid;
            this.in = in;
            this.out = out;
            this.sudoku = sudoku;
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] inputParts = inputLine.split(" ");

                    if (inputParts.length == 1 && inputParts[0].equals("show")) {
                        System.out.println("server show");
                        // String board = sudoku.getSudokuString();
                        // String[] lines = board.split("\n");
                        // for (String line : lines) {
                        //     out.println(line);
                        // }
                        // broadcastBoard();
                        out.println(sudoku.getSudokuString());
                        out.println("Your commmand:");
                        // continue;
                    }
                    else if (inputParts.length == 4 && inputParts[0].equals("update")) {
                        try {
                            int row = Integer.parseInt(inputParts[1]);
                            int col = Integer.parseInt(inputParts[2]);
                            int num = Integer.parseInt(inputParts[3]);
                    
                            if (sudoku.enterNumber(row, col, num)) {
                                int spots = usersMove.getOrDefault(uuid, 0); 
                                spots++; 
                    
                                usersMove.put(uuid, spots);
                    
                                broadcastBoard();
                                // String board = sudoku.getSudokuString();
                                // String[] lines = board.split("\n");
                                // for (String line : lines) {
                                //     out.println(line);
                                // }
                                out.println("update END_BOARD");
                                // out.print("Your command: ");
                                System.out.println("Current moves:");
                                for (Map.Entry<String, Integer> entry : usersMove.entrySet()) {
                                    System.out.println("uuid:" + entry.getKey() + ", Move:" + entry.getValue());
                                }
                    
                                synchronized (clientWriters) {
                                    for (PrintWriter writer : clientWriters) {
                                        writer.println("Your command:");
                                    }
                                }
                    
                                if (sudoku.isBoardFull()) {
                                    broadcastBoard();
                                    out.println("Game Over! The board is full :(");
                                    declareWinner();
                                    out.println("Thank you for playing Sudoku!");
                                    gameOver = true;
                                    System.exit(1);
                                    break;
                                }
                            } else {
                                out.println("Invalid move. Try again.");
                                out.println("Your command:");
                            }
                        } catch (NumberFormatException e) {
                            out.println("Invalid input. Format: update <row> <col> <num>");
                        }
                        // continue;
                        
                    }
                    else if(inputParts.length == 1 && inputParts[0].equals("show")) {
                        out.println("Invalid input. Use 'show' to see the board or 'update <row> <col> <num>' to make a move.");

                    }
                    else if (inputParts.length == 1 && inputParts[0].equals("disconnect")) {
                        out.println("Disconnected from server.");
                        System.out.println("Client disconnected.");
                        break;
                    }

                    else if (inputParts.length == 3) {
                        try {
                            int row = Integer.parseInt(inputParts[0]);
                            int col = Integer.parseInt(inputParts[1]);
                            int num = Integer.parseInt(inputParts[2]);

                            if (sudoku.enterNumber(row, col, num)) {
                                // String board = sudoku.getSudokuString();
                                // String[] lines = board.split("\n");
                                // for (String line : lines) {
                                //     out.println(line);
                                // }
                                broadcastBoard();
                                out.println("END_BOARD parts == 3");
                                // broadcastBoard();
                            } else {
                                out.println("Invalid move. Try again.");
                            }
                        } catch (NumberFormatException e) {
                            out.println("Invalid input. Format: <row> <col> <number>");
                        }
                    }
                    else if (inputParts.length == 1 && inputParts[0].equals("disconnect")) {
                        out.println("Disconnected from server.");
                        System.out.println("Client disconnected.");
                        break;
                    } else {
                        out.println("Invalid input: Use 'show' to view the board or 'update <row> <col> <num>' to make a move.");
                        out.println("END_BOARD invalid input");
                        out.flush();
                    }

                    if (sudoku.isBoardFull()) {
                        broadcastBoard();
                        declareWinner();
                        out.println("Game Over! The board is full.");
                        out.println("Thank you for playing Sudoku!");
                        gameOver = true;
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }

                removeClient(out);
            }
        }
        private static void declareWinner() {
            String winnerUuid = null;
            int maxMoves = 0;
        
            for (Map.Entry<String, Integer> entry : usersMove.entrySet()) {
                String uuid = entry.getKey();
                int moves = entry.getValue();
        
                if (moves > maxMoves) {
                    maxMoves = moves;
                    winnerUuid = uuid;
                }
            }
        
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    if (winnerUuid != null) {
                        writer.println("Game Over! The winner is the player with UUID: " + winnerUuid + " with " + maxMoves + " moves!");
                    } else {
                        writer.println("Game Over! No winner could be determined.");
                    }
                }
            }
        
            System.out.println("Game Over! The winner is the player with UUID: " + winnerUuid + " with " + maxMoves + " moves!");
        }
        
    }
}