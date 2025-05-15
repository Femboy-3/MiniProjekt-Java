import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.List;

public class AddRoutePage {
    private JFrame frame;
    private JTextField nameField, lengthField, difficultyField, durationField;
    private JTextArea descriptionArea;
    private JComboBox<String> startCityComboBox, endCityComboBox;
    private JList<String> poiList;
    private DefaultListModel<String> poiListModel;

    private static final String DB_URL = "jdbc:postgresql://kolesarskepoti.c9286iewgnlt.eu-north-1.rds.amazonaws.com/kolesarskepoti";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Star.wars1#";

    public AddRoutePage() {
        frame = new JFrame("Add New Route");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 700);
        frame.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0 - Route Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Route Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        // Row 1 - Length
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Length (km):"), gbc);
        gbc.gridx = 1;
        lengthField = new JTextField(20);
        formPanel.add(lengthField, gbc);

        // Row 2 - Difficulty
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Difficulty (1-5):"), gbc);
        gbc.gridx = 1;
        difficultyField = new JTextField(20);
        formPanel.add(difficultyField, gbc);

        // Row 3 - Duration
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Duration (hours):"), gbc);
        gbc.gridx = 1;
        durationField = new JTextField(20);
        formPanel.add(durationField, gbc);

        // Row 4 - Description
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descriptionArea = new JTextArea(3, 20);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        formPanel.add(descScroll, gbc);

        // Row 5 - Start City
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Start City:"), gbc);
        gbc.gridx = 1;
        startCityComboBox = new JComboBox<>();
        populateCityComboBox(startCityComboBox);
        formPanel.add(startCityComboBox, gbc);

        // Row 6 - End City
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("End City:"), gbc);
        gbc.gridx = 1;
        endCityComboBox = new JComboBox<>();
        populateCityComboBox(endCityComboBox);
        formPanel.add(endCityComboBox, gbc);

        // Row 7 - POI list
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Select POIs:"), gbc);
        gbc.gridx = 1;
        poiListModel = new DefaultListModel<>();
        poiList = new JList<>(poiListModel);
        poiList.setVisibleRowCount(5);
        poiList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        poiList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = poiList.locationToIndex(e.getPoint());
                if (index != -1) {
                    if (poiList.isSelectedIndex(index)) {
                        poiList.removeSelectionInterval(index, index);
                    } else {
                        poiList.addSelectionInterval(index, index);
                    }
                }
            }
        });

        JScrollPane poiScroll = new JScrollPane(poiList);
        poiScroll.setPreferredSize(new Dimension(200, 100));
        formPanel.add(poiScroll, gbc);
        populatePoiList();

        frame.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addRouteButton = new JButton("Add Route");
        JButton backButton = new JButton("Back to Main Page");

        addRouteButton.addActionListener(e -> addRoute());
        backButton.addActionListener(e -> goBackToMainPage());

        buttonPanel.add(addRouteButton);
        buttonPanel.add(backButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void populateCityComboBox(JComboBox<String> comboBox) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT name FROM citys";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    comboBox.addItem(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populatePoiList() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT id, pointname FROM pointsofinterest";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("pointname");
                    poiListModel.addElement(id + ": " + name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addRoute() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT insert_route(?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, nameField.getText());
                stmt.setFloat(2, Float.parseFloat(lengthField.getText()));
                stmt.setInt(3, Integer.parseInt(difficultyField.getText()));
                stmt.setFloat(4, Float.parseFloat(durationField.getText()));
                stmt.setString(5, descriptionArea.getText());
                stmt.setString(6, (String) startCityComboBox.getSelectedItem());
                stmt.setString(7, (String) endCityComboBox.getSelectedItem());

                List<String> selectedPOIs = poiList.getSelectedValuesList();
                Integer[] poiIds = selectedPOIs.stream()
                        .map(s -> Integer.parseInt(s.split(":")[0]))
                        .toArray(Integer[]::new);
                Array poiArray = conn.createArrayOf("INTEGER", poiIds);
                stmt.setArray(8, poiArray);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int routeId = rs.getInt(1);
                    JOptionPane.showMessageDialog(frame, "Route inserted with ID: " + routeId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error adding route: " + e.getMessage());
        }
    }

    private void goBackToMainPage() {
        frame.dispose();
    }

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(AddRoutePage::new);
    }
}
