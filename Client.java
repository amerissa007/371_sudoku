import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Client <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try {
            Socket clientSocket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            ServerListener serverListener = new ServerListener(in);
            serverListener.start();

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String fromUser;

            while (true) {
                fromUser = stdIn.readLine();
                if (fromUser == null) {
                    break;
                }
                out.println(fromUser);
                // System.out.print("Your command: ");

            }

            clientSocket.close();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}

class ServerListener extends Thread {
    private BufferedReader in;

    public ServerListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);

                if (line.contains("Game Over!") || line.contains("Thank you for playing Sudoku!")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from server: " + e.getMessage());
        }
    }
}
