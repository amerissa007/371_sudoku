import java.net.*;
import java.util.HashSet;
import java.io.*;

public class Server {
    private static Sudoku sudoku = new Sudoku();
    private static HashSet<PrintWriter> clientWriters = new HashSet<>();
    private static boolean gameOver = false;

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
                out.println(
                        "Welcome to Sudoku! Type 'show' to see the board or 'update <row> <col> <num>' to make a move.");

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                new Thread(new ClientHandler(clientSocket, in, out, sudoku)).start();
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

        public ClientHandler(Socket clientSocket, BufferedReader in, PrintWriter out, Sudoku sudoku) {
            this.clientSocket = clientSocket;
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
                        String board = sudoku.getSudokuString();
                        String[] lines = board.split("\n");
                        for (String line : lines) {
                            out.println(line);
                        }
                        out.println("END_BOARD");
                        continue;
                    }

                    if (inputParts.length == 4 && inputParts[0].equals("update")) {
                        try {
                            int row = Integer.parseInt(inputParts[1]);
                            int col = Integer.parseInt(inputParts[2]);
                            int num = Integer.parseInt(inputParts[3]);

                            if (sudoku.enterNumber(row, col, num)) {
                                broadcastBoard();

                                synchronized (clientWriters) {
                                    for (PrintWriter writer : clientWriters) {
                                        writer.println("END_BOARD");
                                    }
                                }

                                if (sudoku.isBoardFull()) {
                                    broadcastBoard();
                                    out.println("Game Over! The board is full :(");
                                    out.println("Thank you for playing Sudoku!");
                                    gameOver = true;
                                    break;
                                }
                            } else {
                                this.out.println("Invalid move. Try again.");
                                this.out.println("END_BOARD");

                            }

                        } catch (NumberFormatException e) {
                            out.println("Invalid input. Format: update <row> <col> <num>");
                        }
                        continue;
                    }

                    if (inputParts.length == 1 && inputParts[0].equals("disconnect")) {
                        out.println("Disconnected from server.");
                        break;
                    }

                    if (inputParts.length == 3) {
                        try {
                            int row = Integer.parseInt(inputParts[0]);
                            int col = Integer.parseInt(inputParts[1]);
                            int num = Integer.parseInt(inputParts[2]);

                            if (sudoku.enterNumber(row, col, num)) {
                                broadcastBoard();
                            } else {
                                out.println("Invalid move. Try again.");
                            }
                        } catch (NumberFormatException e) {
                            out.println("Invalid input. Format: <row> <col> <number>");
                        }
                    }

                    if (sudoku.isBoardFull()) {
                        broadcastBoard();
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
    }
}
