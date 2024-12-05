import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class EmployeeDashboard extends JFrame {
    private User currentUser;
    private Connection connection;

    public EmployeeDashboard(User user, Connection connection) {
        this.currentUser = user;
        this.connection = connection;
        setTitle("Employee Dashboard");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getFullName());
        panel.add(welcomeLabel);

        // Create buttons
        JButton viewCustomersButton = new JButton("View Customers");
        JButton manageAccountsButton = new JButton("Manage Bank Accounts");
        JButton approveRequestsButton = new JButton("Account Requests");
        JButton appointmentsButton = new JButton("View Appointments");
        JButton logoutButton = new JButton("Logout");

        // Add action listeners
        viewCustomersButton.addActionListener(e -> viewCustomers());
        manageAccountsButton.addActionListener(e -> manageAccounts());
        approveRequestsButton.addActionListener(e -> handleRequests());
        appointmentsButton.addActionListener(e -> viewAppointments());
        logoutButton.addActionListener(e -> logout());

        // Add buttons
        panel.add(viewCustomersButton);
        panel.add(manageAccountsButton);
        panel.add(approveRequestsButton);
        panel.add(appointmentsButton);
        panel.add(logoutButton);

        add(panel);
    }

    private void logout() {
        dispose();
        new LoginFrame(connection).setVisible(true);
    }
    
    private void viewCustomers() {
        JDialog dialog = new JDialog(this, "Customer List", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        try {
            String query = "SELECT user_id, username, full_name, address FROM Users WHERE user_type_id = 1";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"ID", "Username", "Full Name", "Address"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("address")
                };
                model.addRow(row);
            }

            JTable customerTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(customerTable);
            dialog.add(scrollPane);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving customers: " + e.getMessage());
        }

        dialog.setVisible(true);
    }

    private void manageAccounts() {
        JDialog dialog = new JDialog(this, "Manage Bank Accounts", true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);

        try {
            String query = "SELECT ba.account_id, u.full_name, ba.balance, ba.is_approved " +
                          "FROM BankAccounts ba " +
                          "JOIN Users u ON ba.user_id = u.user_id " +
                          "ORDER BY u.full_name";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"Account ID", "Customer Name", "Balance", "Status"};
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

            JPanel buttonPanel = new JPanel();
            JButton deleteButton = new JButton("Delete Account");
            deleteButton.addActionListener(e -> {
                int row = accountTable.getSelectedRow();
                if (row >= 0) {
                    int accountId = (int) accountTable.getValueAt(row, 0);
                    deleteAccount(accountId);
                    model.removeRow(row);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please select an account to delete.");
                }
            });
            buttonPanel.add(deleteButton);

            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving accounts: " + e.getMessage());
        }

        dialog.setVisible(true);
    }

    private void deleteAccount(int accountId) {
        try {
            String query = "DELETE FROM BankAccounts WHERE account_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, accountId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Account deleted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting account: " + e.getMessage());
        }
    }

    private void handleRequests() {
        JDialog dialog = new JDialog(this, "Account Requests", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        try {
            String query = "SELECT ba.account_id, u.full_name, ba.balance " +
                          "FROM BankAccounts ba " +
                          "JOIN Users u ON ba.user_id = u.user_id " +
                          "WHERE ba.is_approved = false";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"Account ID", "Customer Name", "Initial Balance"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("account_id"),
                    rs.getString("full_name"),
                    String.format("$%.2f", rs.getDouble("balance"))
                };
                model.addRow(row);
            }

            JTable requestTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(requestTable);

            JPanel buttonPanel = new JPanel();
            JButton approveButton = new JButton("Approve");
            JButton declineButton = new JButton("Decline");

            approveButton.addActionListener(e -> {
                int row = requestTable.getSelectedRow();
                if (row >= 0) {
                    approveAccount((int) requestTable.getValueAt(row, 0));
                    model.removeRow(row);
                }
            });

            declineButton.addActionListener(e -> {
                int row = requestTable.getSelectedRow();
                if (row >= 0) {
                    deleteAccount((int) requestTable.getValueAt(row, 0));
                    model.removeRow(row);
                }
            });

            buttonPanel.add(approveButton);
            buttonPanel.add(declineButton);

            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving requests: " + e.getMessage());
        }

        dialog.setVisible(true);
    }

    private void approveAccount(int accountId) {
        try {
            String query = "UPDATE BankAccounts SET is_approved = true WHERE account_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, accountId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Account approved successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error approving account: " + e.getMessage());
        }
    }

    private void viewAppointments() {
        JDialog dialog = new JDialog(this, "View Appointments", true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        try {
            String query = "SELECT a.appointment_id, u.full_name, a.appointment_date, a.status " +
                          "FROM Appointments a " +
                          "JOIN Users u ON a.customer_id = u.user_id " +
                          "ORDER BY a.appointment_date";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"ID", "Customer", "Date/Time", "Status"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            JTable appointmentTable = new JTable(model);
            appointmentTable.setDefaultEditor(Object.class, null);  // Makes all cells non-editable

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("appointment_id"),
                    rs.getString("full_name"),
                    rs.getString("appointment_date"),
                    rs.getString("status")
                };
                model.addRow(row);
            }

            JScrollPane scrollPane = new JScrollPane(appointmentTable);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            // Button Panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JButton confirmButton = new JButton("Confirm Appointment");
            JButton denyButton = new JButton("Deny Appointment");

            confirmButton.addActionListener(e -> {
                int selectedRow = appointmentTable.getSelectedRow();
                if (selectedRow != -1) {
                    int appointmentId = (int) appointmentTable.getValueAt(selectedRow, 0);
                    updateAppointmentStatus(appointmentId, "CONFIRMED");
                    model.setValueAt("CONFIRMED", selectedRow, 3);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please select an appointment");
                }
            });

            denyButton.addActionListener(e -> {
                int selectedRow = appointmentTable.getSelectedRow();
                if (selectedRow != -1) {
                    int appointmentId = (int) appointmentTable.getValueAt(selectedRow, 0);
                    updateAppointmentStatus(appointmentId, "CANCELLED");
                    model.setValueAt("CANCELLED", selectedRow, 3);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please select an appointment");
                }
            });

            buttonPanel.add(confirmButton);
            buttonPanel.add(denyButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving appointments: " + e.getMessage());
        }

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void updateAppointmentStatus(int appointmentId, String status) {
        try {
            String query = "UPDATE Appointments SET status = ? WHERE appointment_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, status);
            pstmt.setInt(2, appointmentId);
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Appointment status updated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update appointment status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating appointment: " + e.getMessage());
        }
    }
}