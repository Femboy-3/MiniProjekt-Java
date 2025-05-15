import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class Register extends JFrame {

    public Register() {
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        setTitle("Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(350, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));

        JTextField usernameField = new JTextField();
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField phoneField = new JTextField();

        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back to Login");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneField);
        panel.add(registerButton);
        panel.add(backButton);

        add(panel);
        setVisible(true);

        registerButton.addActionListener((ActionEvent e) -> {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String phone = phoneField.getText().trim();

            if (registerUser(username, email, password, phone)) {
                JOptionPane.showMessageDialog(this, "Registration successful!");
                dispose();
                new Login();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed.");
            }
        });

        backButton.addActionListener((ActionEvent e) -> {
            dispose();
            new Login();
        });
    }

    private boolean registerUser(String username, String email, String password, String phone) {
        boolean success = false;

        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            Class.forName("org.postgresql.Driver");

            String query = "{? = call register_user(?, ?, ?, ?)}";
            try (CallableStatement stmt = conn.prepareCall(query)) {
                stmt.registerOutParameter(1, Types.BOOLEAN);
                stmt.setString(2, username);
                stmt.setString(3, email);
                stmt.setString(4, password);
                stmt.setString(5, phone);

                stmt.execute();

                success = stmt.getBoolean(1);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return success;
    }

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new Register());
    }
}
