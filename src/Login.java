import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Login extends JFrame {

    private static final String DB_URL = "jdbc:postgresql://kolesarskepoti.c9286iewgnlt.eu-north-1.rds.amazonaws.com/kolesarskepoti";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Star.wars1#";

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

        int userId = login(email, password);

        switch (userId) {
            case 0:
                JOptionPane.showMessageDialog(this, "Invalid email or password.");
                break;
            case -1:
                UserSession.isAdmin = true;
                UserSession.userId = userId;
                dispose();
                new MainPage();
                break;
            default:
                UserSession.isAdmin = false;
                UserSession.userId = userId;
                dispose();
                new MainPage();
                break;
        }
    }


    private void openRegisterForm() {
        dispose();
        new Register();
    }

    public static int login(String email, String password) {
        int userId = 0;

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                String query = "{? = call login_user(?, ?)}"; // function must return INTEGER
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.registerOutParameter(1, Types.INTEGER);
                    stmt.setString(2, email);
                    stmt.setString(3, password);
                    stmt.execute();
                    userId = stmt.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return userId;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Login::new);
    }
}