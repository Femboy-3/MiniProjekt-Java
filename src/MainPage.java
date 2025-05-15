import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.sql.*;
import java.awt.*;
import java.awt.event.*;

public class MainPage {

    private JFrame frame;
    private JTable routesTable;
    private DefaultTableModel model;

    // DB connection
    private static final String DB_URL = "jdbc:postgresql://kolesarskepoti.c9286iewgnlt.eu-north-1.rds.amazonaws.com/kolesarskepoti";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Star.wars1#";

    public MainPage() {
        frame = new JFrame("Routes Table");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        String[] columnNames = {
                "ID", "Name", "Length", "Difficulty", "Duration", "Description", "Number of POIs",
                "Start Location", "End Location", "Date Created", "Actions"
        };

        model = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 10;
            }
        };

        routesTable = new JTable(model);
        routesTable.setRowHeight(40);

        for (int i = 0; i < routesTable.getColumnCount() - 1; i++) {
            TableColumn col = routesTable.getColumnModel().getColumn(i);
            col.setPreferredWidth(35);
        }
        // Set Actions column to auto-size to fit buttons:
        TableColumn actionsColumn = routesTable.getColumnModel().getColumn(10);
        actionsColumn.setPreferredWidth(220); // enough width for buttons

        JScrollPane scrollPane = new JScrollPane(routesTable);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");

        bottomPanel.add(logoutButton);
        if(UserSession.isAdmin) {
            JButton addRouteButton = new JButton("Add Route");
            addRouteButton.addActionListener(e -> new AddRoutePage());
            bottomPanel.add(addRouteButton);
        }
        frame.add(bottomPanel, BorderLayout.SOUTH);

        logoutButton.addActionListener(e -> {
            UserSession.clear();
            frame.dispose();
            new Login();
        });

        routesTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        routesTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));

        loadRoutesData();

        frame.setVisible(true);
    }

    public void loadRoutesData() {
        if (routesTable.isEditing()) {
            routesTable.getCellEditor().stopCellEditing();
        }
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT * FROM get_all_routes()";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    float length = rs.getFloat("length");
                    int difficulty = rs.getInt("difficulty");
                    float duration = rs.getFloat("duration");
                    String description = rs.getString("description");
                    int numOfPoi = rs.getInt("num_of_poi");
                    String startLocation = rs.getString("start_location_name");
                    String endLocation = rs.getString("end_location_name");
                    Timestamp dateCreated = rs.getTimestamp("date_created");

                    model.addRow(new Object[]{id, name, length, difficulty, duration, description, numOfPoi, startLocation, endLocation, dateCreated, "Actions"});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    class ButtonRenderer extends JPanel implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            JButton commentButton = new JButton("Comments");
            panel.add(commentButton);
            if (UserSession.isAdmin) {
                JButton updateButton = new JButton("Update");
                JButton deleteButton = new JButton("Delete");
                panel.add(updateButton);
                panel.add(deleteButton);
            }
            return panel;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton commentButton;
        private JButton updateButton;
        private JButton deleteButton;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            commentButton = new JButton("Comments");
            commentButton.addActionListener(e -> handleComments());
            panel.add(commentButton);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.removeAll();
            selectedRow = row;
            panel.add(commentButton);
            if (UserSession.isAdmin) {
                updateButton = new JButton("Update");
                deleteButton = new JButton("Delete");
                updateButton.addActionListener(e -> handleUpdate());
                deleteButton.addActionListener(e -> handleDelete());
                panel.add(updateButton);
                panel.add(deleteButton);
            }
            return panel;
        }

        private void handleComments() {
            int routeId = (int) model.getValueAt(selectedRow, 0);
            new CommentPage(routeId);
            fireEditingStopped();
        }

        private void handleUpdate() {
            int routeId = (int) model.getValueAt(selectedRow, 0);
            JOptionPane.showMessageDialog(null, "Open update page for route ID: " + routeId);
            fireEditingStopped();
        }

        private void handleDelete() {
            if (selectedRow < 0 || selectedRow >= model.getRowCount()) {
                JOptionPane.showMessageDialog(null, "No valid route selected.");
                return;
            }
            int routeId = (int) model.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete route ID: " + routeId + "?");
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                    String sql = "SELECT delete_route(?)";  // call your SQL function to delete route + linked POIs
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, routeId);
                        stmt.execute();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                // Stop editing BEFORE loading new data
                if (routesTable.isEditing()) {
                    routesTable.getCellEditor().stopCellEditing();
                }

                // Clear selection BEFORE loading new data
                routesTable.clearSelection();

                // Reload data
                loadRoutesData();

                // Reset selectedRow
                selectedRow = -1;
            }
            fireEditingStopped();
        }



        @Override
        public Object getCellEditorValue() {
            return "Actions";
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new MainPage());
    }
}
