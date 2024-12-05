import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private Connection connection;

    public LoginFrame(Connection connection) {
        this.connection = connection;
        setTitle("Bank Management System");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Username
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        // Password
        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        // Login button
        JButton loginButton = new JButton("Login");
        JButton createAccountButton = new JButton("Create Account");

        loginButton.addActionListener(e -> handleLogin());
        createAccountButton.addActionListener(e -> showCreateAccountDialog());

        panel.add(loginButton);
        panel.add(createAccountButton);

        add(panel);
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        try {
            String query = "SELECT user_id, full_name, user_type_id FROM Users WHERE username = ? AND password = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User(
                    username,
                    password,
                    rs.getString("full_name"),
                    "", // Address can be loaded if needed
                    getUserType(rs.getInt("user_type_id"))
                );
                user.setUserId(rs.getInt("user_id"));

                openDashboard(user);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private String getUserType(int userTypeId) {
        switch(userTypeId) {
            case 1: return "CUSTOMER";
            case 2: return "EMPLOYEE";
            case 3: return "ADMIN";
            default: return "CUSTOMER";
        }
    }

    private void openDashboard(User user) {
        this.dispose();
        
        switch(user.getUserType()) {
            case "CUSTOMER":
                new CustomerDashboard(user, connection).setVisible(true);
                break;
            case "EMPLOYEE":
                new EmployeeDashboard(user, connection).setVisible(true);
                break;
            case "ADMIN":
                new AdminDashboard(user, connection).setVisible(true);
                break;
        }
    }

    private void showCreateAccountDialog() {
        JDialog dialog = new JDialog(this, "Create Account", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        JTextField newUsername = new JTextField();
        panel.add(newUsername);

        panel.add(new JLabel("Password:"));
        JPasswordField newPassword = new JPasswordField();
        panel.add(newPassword);

        panel.add(new JLabel("Full Name:"));
        JTextField fullName = new JTextField();
        panel.add(fullName);

        panel.add(new JLabel("Address:"));
        JTextField address = new JTextField();
        panel.add(address);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            if (newUsername.getText().isEmpty() || new String(newPassword.getPassword()).isEmpty() ||
                fullName.getText().isEmpty() || address.getText().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields");
                return;
            }

            try {
                String insertQuery = "INSERT INTO Users (username, password, full_name, address, user_type_id) VALUES (?, ?, ?, ?, 1)";
                PreparedStatement pstmt = connection.prepareStatement(insertQuery);
                pstmt.setString(1, newUsername.getText());
                pstmt.setString(2, new String(newPassword.getPassword()));
                pstmt.setString(3, fullName.getText());
                pstmt.setString(4, address.getText());
                
                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(dialog, "Account created successfully!");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to create account");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage());
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        panel.add(createButton);
        panel.add(cancelButton);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/BankManagementSystem";
            String user = "root";
            String password = "database28";
            
            Connection conn = DriverManager.getConnection(url, user, password);
            
            // Launch the application with the connection
            SwingUtilities.invokeLater(() -> new LoginFrame(conn).setVisible(true));
            
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Connection Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}