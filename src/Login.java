import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Login extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;

    public Login() {
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        setTitle("Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2));

        add(new JLabel("Email:"));
        emailField = new JTextField();
        add(emailField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        add(loginButton);
        add(registerButton);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> openRegisterForm());

        setVisible(true);
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        UserLoginResult result = login(email, password);

        if (result.userId == 0) {
            JOptionPane.showMessageDialog(this, "Invalid email or password.");
        } else {
            UserSession.userId = result.userId;
            UserSession.isAdmin = result.isAdmin;
            dispose();
            new MainPage();
        }
    }

    private void openRegisterForm() {
        dispose();
        new Register();
    }

    // Helper class to hold login results
    private static class UserLoginResult {
        int userId;
        boolean isAdmin;

        UserLoginResult(int userId, boolean isAdmin) {
            this.userId = userId;
            this.isAdmin = isAdmin;
        }
    }

    public static UserLoginResult login(String email, String password) {
        int userId = 0;
        boolean isAdmin = false;

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
                String query = "SELECT * FROM login_user(?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, email);
                    stmt.setString(2, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("userid");
                            isAdmin = rs.getBoolean("isadmin");
                        } else {
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new UserLoginResult(userId, isAdmin);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Login::new);
    }
}
