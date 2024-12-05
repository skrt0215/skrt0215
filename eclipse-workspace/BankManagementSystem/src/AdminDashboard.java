import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class AdminDashboard extends JFrame {
    private User currentUser;
    private Connection connection;

    public AdminDashboard(User user, Connection connection) {
        this.currentUser = user;
        this.connection = connection;
        setTitle("Admin Dashboard");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel welcomeLabel = new JLabel("Welcome, Administrator " + currentUser.getFullName());
        panel.add(welcomeLabel);

        // Create buttons
        JButton createEmployeeButton = new JButton("Create Employee Account");
        JButton manageUsersButton = new JButton("Manage Users");
        JButton manageAccountsButton = new JButton("Manage Bank Accounts");
        JButton systemSettingsButton = new JButton("System Settings");
        JButton logoutButton = new JButton("Logout");

        // Add action listeners
        createEmployeeButton.addActionListener(e -> createEmployee());
        manageUsersButton.addActionListener(e -> manageUsers());
        manageAccountsButton.addActionListener(e -> manageAccounts());
        systemSettingsButton.addActionListener(e -> systemSettings());
        logoutButton.addActionListener(e -> logout());

        // Add buttons
        panel.add(createEmployeeButton);
        panel.add(manageUsersButton);
        panel.add(manageAccountsButton);
        panel.add(systemSettingsButton);
        panel.add(logoutButton);

        add(panel);
    }

    private void createEmployee() {
        JDialog dialog = new JDialog(this, "Create Employee Account", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        JTextField username = new JTextField();
        panel.add(username);

        panel.add(new JLabel("Password:"));
        JPasswordField password = new JPasswordField();
        panel.add(password);

        panel.add(new JLabel("Full Name:"));
        JTextField fullName = new JTextField();
        panel.add(fullName);

        panel.add(new JLabel("Address:"));
        JTextField address = new JTextField();
        panel.add(address);

        JButton createButton = new JButton("Create");
        JButton cancelButton = new JButton("Cancel");

        createButton.addActionListener(e -> {
            if (username.getText().isEmpty() || password.getPassword().length == 0 || 
                fullName.getText().isEmpty() || address.getText().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields");
                return;
            }

            try {
                String query = "INSERT INTO Users (username, password, full_name, address, user_type_id) VALUES (?, ?, ?, ?, 2)";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, username.getText());
                pstmt.setString(2, new String(password.getPassword()));
                pstmt.setString(3, fullName.getText());
                pstmt.setString(4, address.getText());
                
                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(dialog, "Employee account created successfully!");
                    dialog.dispose();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error creating employee: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        panel.add(createButton);
        panel.add(cancelButton);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void manageUsers() {
        JDialog dialog = new JDialog(this, "Manage Users", true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());

        try {
            String query = "SELECT u.user_id, u.username, u.full_name, ut.type_name FROM Users u " +
                          "JOIN UserTypes ut ON u.user_type_id = ut.user_type_id";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"ID", "Username", "Full Name", "User Type"};
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("type_name")
                };
                model.addRow(row);
            }

            JTable userTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(userTable);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            // Button Panel
            JPanel buttonPanel = new JPanel();
            JButton deleteButton = new JButton("Delete User");
            JButton resetPasswordButton = new JButton("Reset Password");

            deleteButton.addActionListener(e -> {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow != -1) {
                    int userId = (int) userTable.getValueAt(selectedRow, 0);
                    int confirm = JOptionPane.showConfirmDialog(dialog, 
                        "Are you sure you want to delete this user?", 
                        "Confirm Delete", 
                        JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        deleteUser(userId);
                        model.removeRow(selectedRow);
                    }
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please select a user to delete");
                }
            });

            resetPasswordButton.addActionListener(e -> {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow != -1) {
                    int userId = (int) userTable.getValueAt(selectedRow, 0);
                    resetUserPassword(userId);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please select a user");
                }
            });

            buttonPanel.add(deleteButton);
            buttonPanel.add(resetPasswordButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving users: " + e.getMessage());
        }

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void deleteUser(int userId) {
        try {
            String query = "DELETE FROM Users WHERE user_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User deleted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting user: " + e.getMessage());
        }
    }

    private void resetUserPassword(int userId) {
        String newPassword = JOptionPane.showInputDialog(this, "Enter new password:");
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            try {
                String query = "UPDATE Users SET password = ? WHERE user_id = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, newPassword);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Password reset successfully");
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error resetting password: " + e.getMessage());
            }
        }
    }

    private void manageAccounts() {
        JDialog dialog = new JDialog(this, "Manage Bank Accounts", true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());

        try {
            String query = "SELECT ba.account_id, u.full_name, ba.balance, ba.is_approved " +
                          "FROM BankAccounts ba JOIN Users u ON ba.user_id = u.user_id";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"Account ID", "Owner", "Balance", "Status"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("account_id"),
                    rs.getString("full_name"),
                    String.format("$%.2f", rs.getDouble("balance")),
                    rs.getBoolean("is_approved") ? "Approved" : "Pending"
                };
                model.addRow(row);
            }

            JTable accountTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(accountTable);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton deleteButton = new JButton("Delete Account");

            deleteButton.addActionListener(e -> {
                int selectedRow = accountTable.getSelectedRow();
                if (selectedRow != -1) {
                    int accountId = (int) accountTable.getValueAt(selectedRow, 0);
                    deleteAccount(accountId);
                    model.removeRow(selectedRow);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please select an account");
                }
            });

            buttonPanel.add(deleteButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving accounts: " + e.getMessage());
        }

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void deleteAccount(int accountId) {
        try {
            String query = "DELETE FROM BankAccounts WHERE account_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, accountId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Account deleted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting account: " + e.getMessage());
        }
    }

    private void systemSettings() {
        JDialog dialog = new JDialog(this, "System Settings", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton backupButton = new JButton("Backup Database");
        JButton restoreButton = new JButton("Restore Database");
        
        backupButton.addActionListener(e -> 
            JOptionPane.showMessageDialog(dialog, "Backup functionality to be implemented"));
        
        restoreButton.addActionListener(e -> 
            JOptionPane.showMessageDialog(dialog, "Restore functionality to be implemented"));

        panel.add(backupButton);
        panel.add(restoreButton);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void logout() {
        dispose();
        new LoginFrame(connection).setVisible(true);
    }
}