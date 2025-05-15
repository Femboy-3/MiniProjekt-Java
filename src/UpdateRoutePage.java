import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UpdateRoutePage {
    private JFrame frame;
    private JTextField nameField, lengthField, difficultyField, durationField;
    private JTextArea descriptionArea;
    private JComboBox<String> startCityComboBox, endCityComboBox;
    private JList<String> poiList;
    private DefaultListModel<String> poiListModel;

    private int routeId;

    public UpdateRoutePage(int routeId) {
        this.routeId = routeId;

        frame = new JFrame("Update Route");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 700);
        frame.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0 - Route Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Route Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        // Row 1 - Length
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Length:"), gbc);
        gbc.gridx = 1;
        lengthField = new JTextField(20);
        formPanel.add(lengthField, gbc);

        // Row 2 - Difficulty
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Difficulty:"), gbc);
        gbc.gridx = 1;
        difficultyField = new JTextField(20);
        formPanel.add(difficultyField, gbc);

        // Row 3 - Duration
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Duration:"), gbc);
        gbc.gridx = 1;
        durationField = new JTextField(20);
        formPanel.add(durationField, gbc);

        // Row 4 - Description (textarea)
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

        // Row 7 - POI List
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

        frame.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton updateButton = new JButton("Update Route");
        JButton backButton = new JButton("Back to Main Page");

        updateButton.addActionListener(e -> updateRoute());
        backButton.addActionListener(e -> goBackToMainPage());

        buttonPanel.add(updateButton);
        buttonPanel.add(backButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Load data after UI ready
        populatePoiList();
        loadRouteData();

        frame.setVisible(true);
    }

    private void populateCityComboBox(JComboBox<String> comboBox) {
        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
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
        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            String query = "SELECT id, pointname FROM pointsofinterest";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                poiListModel.clear();
                while (rs.next()) {
                    poiListModel.addElement(rs.getInt("id") + ": " + rs.getString("pointname"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRouteData() {
        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            String query = "SELECT * FROM get_route_by_id(?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, routeId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    nameField.setText(rs.getString("name"));
                    lengthField.setText(String.valueOf(rs.getFloat("length")));
                    difficultyField.setText(String.valueOf(rs.getInt("difficulty")));
                    durationField.setText(String.valueOf(rs.getFloat("duration")));
                    descriptionArea.setText(rs.getString("description"));
                    startCityComboBox.setSelectedItem(rs.getString("start_location_name"));
                    endCityComboBox.setSelectedItem(rs.getString("end_location_name"));
                }
            }

            String poiQuery = "SELECT poi_ids FROM get_route_pois(?)";
            try (PreparedStatement stmt = conn.prepareStatement(poiQuery)) {
                stmt.setInt(1, routeId);
                ResultSet rs = stmt.executeQuery();
                List<Integer> poiIds = new ArrayList<>();
                while (rs.next()) {
                    poiIds.add(rs.getInt("poi_ids"));
                }
                for (int i = 0; i < poiListModel.getSize(); i++) {
                    String element = poiListModel.getElementAt(i);
                    int poiId = Integer.parseInt(element.split(":")[0]);
                    if (poiIds.contains(poiId)) {
                        poiList.addSelectionInterval(i, i);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateRoute() {
        try (Connection conn = DriverManager.getConnection(DataBaseConnection.DB_URL, DataBaseConnection.USER, DataBaseConnection.PASSWORD)) {
            String query = "SELECT update_route(?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, routeId);
                stmt.setString(2, nameField.getText());
                stmt.setFloat(3, Float.parseFloat(lengthField.getText()));
                stmt.setInt(4, Integer.parseInt(difficultyField.getText()));
                stmt.setFloat(5, Float.parseFloat(durationField.getText()));
                stmt.setString(6, descriptionArea.getText());
                stmt.setString(7, (String) startCityComboBox.getSelectedItem());
                stmt.setString(8, (String) endCityComboBox.getSelectedItem());

                List<String> selectedPOIs = poiList.getSelectedValuesList();
                Integer[] poiIds = selectedPOIs.stream()
                        .map(s -> Integer.parseInt(s.split(":")[0]))
                        .toArray(Integer[]::new);
                Array poiArray = conn.createArrayOf("INTEGER", poiIds);
                stmt.setArray(9, poiArray);

                stmt.executeQuery();
                JOptionPane.showMessageDialog(null, "Route updated successfuly");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error updating route: " + e.getMessage());
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
        SwingUtilities.invokeLater(() -> new UpdateRoutePage(11)); // example route id
    }
}
