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
            System.out.println("Valid commands: ");
            System.out.println("1. show - View the current Sudoku board.");
            System.out.println("2. update <row> <col> <num> - Update the board (row and col: 0-8, num: 1-9).");
            System.out.println("3. disconnect - Disconnect from the server.");
            System.out.println("Enter your command: ");

            String fromServer;
            String fromUser;
            String disconnect = "disconnect";
            String show = "show";
            Sudoku sudoku = new Sudoku();

            

            while ((fromServer = in.readLine()) != null) {
                System.out.println(fromServer);

                if (fromServer.contains("Game Over!") || fromServer.contains("Thank you for playing Sudoku!")) {
                    break;
                }

                System.out.print("Your command: ");
                fromUser = stdIn.readLine();

                if (fromUser.equals(disconnect)) {
                    out.println(disconnect);
                    System.out.println("Disconnected from server.");
                    break;
                }
                if (fromUser.equals(show)) {
                    out.println(show);
                    String board = sudoku.getSudokuString();
                    System.out.println(board);
                    System.out.print("Your command: ");
                    fromUser = stdIn.readLine();
                }
                // if (fromUser.equals(show)){
                //     out.println(sudoku.getSudokuString());
                //     continue;
                // }

                if (fromUser != null) {
                    if (isValidCommand(fromUser)) {
                        out.println(fromUser);
                    } else {
                        System.out.println("Invalid command. Please use 'show' or 'update <row> <col> <num>'.");
                        System.out.print("Your command: ");
                        fromUser = stdIn.readLine();
                    }
                }
                continue;
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
