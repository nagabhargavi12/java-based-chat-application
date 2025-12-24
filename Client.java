import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12346;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("✅ Connected to the chat server!");
            System.out.println("💡 Type '/exit' anytime to leave the chat.\n");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread to listen for server messages
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println(serverResponse);
                    }
                } catch (IOException e) {
                    System.out.println("🔌 Disconnected from server.");
                }
            }).start();

            // Main user input loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String userInput = scanner.nextLine();
                out.println(userInput);

                if (userInput.equalsIgnoreCase("/exit")) {
                    System.out.println("👋 Exiting chat...");
                    socket.close();
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Could not connect to the server. Make sure the server is running.");
        }
    }
}
