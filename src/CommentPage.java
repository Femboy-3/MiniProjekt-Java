import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;

public class CommentPage extends JFrame {
    private final int routeId;
    private final DefaultTableModel model;
    private final JTable table;
    private final JButton deleteButton;
    private final JTextField commentInput;  // input box for new comment

    private final int currentUserId;
    private final boolean isAdmin;

    public CommentPage(int routeId) {
        super("Comments for Route " + routeId);
        this.routeId = routeId;
        this.currentUserId = UserSession.userId;
        this.isAdmin = UserSession.isAdmin;

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new Object[]{"ID", "Username", "Comment"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // table cells not editable
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.removeColumn(table.getColumnModel().getColumn(0)); // hide ID column

        commentInput = new JTextField();
        commentInput.setToolTipText("Type your comment and press Enter to add");
        commentInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addComment();
                }
            }
        });

        deleteButton = new JButton("Delete Comment");
        deleteButton.addActionListener(e -> deleteSelectedComment());

        // panel to hold input and delete button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(commentInput, BorderLayout.CENTER);
        bottomPanel.add(deleteButton, BorderLayout.SOUTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadComments();

        setSize(600, 400);
        setLocationRelativeTo(null);  // center window on screen
        setVisible(true);
    }

    private void loadComments() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            String sql = "SELECT * FROM get_comments_with_usernames(?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, routeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int commentId = rs.getInt("comment_id");
                        String username = rs.getString("username");
                        String comment = rs.getString("comment_text");
                        model.addRow(new Object[]{commentId, username, comment});
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading comments.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addComment() {
        String commentText = commentInput.getText().trim();
        if (commentText.isEmpty()) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            String sql = "SELECT add_comment(?, ?, ?)"; // assumed DB func: add_comment(route_id, user_id, comment_text)
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, routeId);
                stmt.setInt(2, currentUserId);
                stmt.setString(3, commentText);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getBoolean(1)) {
                        commentInput.setText("");
                        loadComments();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to add comment.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding comment.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedComment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a comment to delete.");
            return;
        }

        int commentId = (int) model.getValueAt(selectedRow, 0);
        String username = (String) model.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete comment by " + username + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            String sql = "SELECT delete_comment(?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, commentId);
                stmt.setInt(2, currentUserId);
                stmt.setBoolean(3, isAdmin);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        boolean success = rs.getBoolean(1);
                        if (success) {
                            JOptionPane.showMessageDialog(this, "Comment deleted.");
                            loadComments();
                        } else {
                            JOptionPane.showMessageDialog(this, "You do not have permission to delete this comment.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting comment.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // For standalone testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CommentPage(1));
    }
}
