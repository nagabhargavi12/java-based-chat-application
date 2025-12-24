import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Scanner;
import java.security.MessageDigest;

public class Server {
    private static final int PORT = 12346;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // JDBC connection details
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatdb";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "";
    private static Connection connection;

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            System.out.println("✅ Connected to MySQL Database.");

            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("💬 Chat Server running on port " + PORT);

            // Admin input thread
            new Thread(() -> {
                Scanner sc = new Scanner(System.in);
                while (true) {
                    String msg = sc.nextLine();
                    broadcast("[Server]: " + msg, null);
                    saveMessage("Server", msg);
                }
            }).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 New client connected: " + clientSocket);

                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Broadcast message to all clients
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    // Save message to DB
    public static void saveMessage(String username, String message) {
        try {
            String sql = "INSERT INTO messages (username, message) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Hash password (SHA-256)
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return password;
        }
    }

    // Inner class: Client handler
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("Welcome! Type 'login' or 'signup' to continue:");

                while (true) {
                    String choice = in.readLine();
                    if (choice == null) return;

                    if (choice.equalsIgnoreCase("signup")) {
                        signupUser();
                        break;
                    } else if (choice.equalsIgnoreCase("login")) {
                        if (loginUser()) break;
                    } else {
                        out.println("Invalid option. Type 'login' or 'signup':");
                    }
                }

                out.println("✅ Welcome to the chat, " + username + "!");
                broadcast("🔔 " + username + " joined the chat!", this);

                // Chat messages
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equalsIgnoreCase("/exit")) {
                        out.println("👋 You have left the chat.");
                        break;
                    }
                    System.out.println("[" + username + "]: " + msg);
                    broadcast("[" + username + "]: " + msg, this);
                    saveMessage(username, msg);
                }

            } catch (IOException e) {
                System.out.println(username + " disconnected.");
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                    broadcast("🚪 " + username + " left the chat.", this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Signup logic
        private void signupUser() throws IOException {
            try {
                out.println("Enter new username:");
                username = in.readLine();

                out.println("Enter new password:");
                String password = in.readLine();
                String hashed = hashPassword(password);

                // Check if user already exists
                String checkSql = "SELECT * FROM users WHERE username = ?";
                PreparedStatement checkPs = connection.prepareStatement(checkSql);
                checkPs.setString(1, username);
                ResultSet rs = checkPs.executeQuery();

                if (rs.next()) {
                    out.println("⚠ You are already signed up! Please login instead.");
                    signupOrLoginAgain();
                    return;
                }

                // Insert new user
                String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
                PreparedStatement ps = connection.prepareStatement(insertSql);
                ps.setString(1, username);
                ps.setString(2, hashed);
                ps.executeUpdate();

                out.println("✅ Signup successful! You can now login as " + username + ".");
                System.out.println("🆕 New user signed up: " + username);
                signupOrLoginAgain();

            } catch (SQLException e) {
                e.printStackTrace();
                out.println("⚠ Error during signup. Try again.");
                signupOrLoginAgain();
            }
        }

        // After signup, ask if user wants to login
        private void signupOrLoginAgain() throws IOException {
            out.println("Type 'login' to login or '/exit' to leave:");
            while (true) {
                String next = in.readLine();
                if (next == null) return;
                if (next.equalsIgnoreCase("login")) {
                    if (loginUser()) return;
                } else if (next.equalsIgnoreCase("/exit")) {
                    out.println("👋 Goodbye!");
                    socket.close();
                    return;
                } else {
                    out.println("Please type 'login' or '/exit':");
                }
            }
        }

        // Login logic
        private boolean loginUser() throws IOException {
            try {
                out.println("Enter username:");
                username = in.readLine();

                out.println("Enter password:");
                String password = in.readLine();
                String hashed = hashPassword(password);

                String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, hashed);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    out.println("✅ Login successful!");
                    System.out.println("👤 User logged in: " + username);
                    return true;
                } else {
                    out.println("❌ Invalid credentials. Try again.");
                    return false;
                }

            } catch (SQLException e) {
                e.printStackTrace();
                out.println("⚠ Database error. Try again.");
                return false;
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
