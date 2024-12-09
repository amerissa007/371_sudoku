
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java Client <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
                Socket socket = new Socket(hostName, portNumber);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Connected to Sudoku server!");
            System.out.println("Valid commands:");
            System.out.println("1. show - View the current Sudoku board.");
            System.out.println("2. update <row> <col> <num> - Update the board (row and col: 0-8, num: 1-9).");
            System.out.println("3. disconnect - Disconnect from the server.");

            String fromUser;
            String disconnect = "disconnect";
            String show = "show";
            String update = "udpate";

            while (true) {
                System.out.print("Your command: ");
                fromUser = stdIn.readLine();

                if (fromUser == null || fromUser.equalsIgnoreCase(disconnect)) {
                    out.println(disconnect);
                    System.out.println("Disconnected from server.");
                    break;
                }

                if (fromUser.equalsIgnoreCase(show)) {
                    out.println(show);

                    String line;
                    while ((line = in.readLine()) != null && !line.equals("END_BOARD")) {
                        System.out.println(line);

                        if (line.contains("Game Over!") || line.contains("Thank you for playing Sudoku!")) {
                            return;
                        }
                    }
                    continue;
                }

                if (fromUser.toLowerCase().startsWith(update)) {
                    String[] parts = fromUser.split(" ");
                    if (parts.length == 4) {
                        try {
                            int row = Integer.parseInt(parts[1]);
                            int col = Integer.parseInt(parts[2]);
                            int num = Integer.parseInt(parts[3]);

                            if (row >= 0 && row <= 8 && col >= 0 && col <= 8 && num >= 1 && num <= 9) {
                                out.println(fromUser);
                            } else {
                                System.out.println("Invalid input. Row and col must be 0-8, and num must be 1-9.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input format. Make sure row, col, and num are integers.");
                            continue;
                        }
                    } else {
                        System.out.println("Invalid format. Use: update <row> <col> <num>");
                        continue;
                    }
                }

                if (isValidCommand(fromUser)) {
                    out.println(fromUser);
                    String response = in.readLine();
                    if (response == null) {
                        System.out.println("Server closed the connection.");
                        break;
                    }
                    System.out.println(response);

                    if (response.contains("Game Over!") || response.contains("Thank you for playing Sudoku!")) {
                        break;
                    }

                    String boardLine;
                    while ((boardLine = in.readLine()) != null && !boardLine.equals("END_BOARD")) {
                        System.out.println(boardLine);
                        if (boardLine.contains("Game Over!") || boardLine.contains("Thank you for playing Sudoku!")) {
                            break;
                        }
                    }

                } else {
                    System.out.println("Invalid command. Please use 'show' or 'update <row> <col> <num>'.");
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }

    private static boolean isValidCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 1 && parts[0].equals("show")) {
            return true;
        } else if (parts.length == 4 && parts[0].equals("update")) {
            try {
                int row = Integer.parseInt(parts[1]);
                int col = Integer.parseInt(parts[2]);
                int num = Integer.parseInt(parts[3]);
                return row >= 0 && row < 9 && col >= 0 && col < 9 && num >= 1 && num <= 9;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
