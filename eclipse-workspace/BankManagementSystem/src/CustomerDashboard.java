import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class CustomerDashboard extends JFrame {
    private User currentUser;
    private Connection connection;

    public CustomerDashboard(User user, Connection connection) {
        this.currentUser = user;
        this.connection = connection;
        setTitle("Customer Dashboard");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getFullName());
        panel.add(welcomeLabel);

        // Buttons for different actions
        JButton viewAccountsButton = new JButton("View Accounts");
        JButton requestAccountButton = new JButton("Request New Account");
        JButton transactionButton = new JButton("Make Transaction");
        JButton appointmentButton = new JButton("Book Appointment");
        JButton logoutButton = new JButton("Logout");

        // Add action listeners
        viewAccountsButton.addActionListener(e -> showAccounts());
        requestAccountButton.addActionListener(e -> requestAccount());
        transactionButton.addActionListener(e -> makeTransaction());
        appointmentButton.addActionListener(e -> bookAppointment());
        logoutButton.addActionListener(e -> logout());

        // Add buttons to panel
        panel.add(viewAccountsButton);
        panel.add(requestAccountButton);
        panel.add(transactionButton);
        panel.add(appointmentButton);
        panel.add(logoutButton);

        add(panel);
    }

    private void showAccounts() {
        JDialog dialog = new JDialog(this, "My Accounts", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);

        try {
            String query = "SELECT account_id, balance, is_approved FROM BankAccounts WHERE user_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            String[] columns = {"Account ID", "Balance", "Status"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("account_id"),
                    String.format("$%.2f", rs.getDouble("balance")),
                    rs.getBoolean("is_approved") ? "Approved" : "Pending"
                };
                model.addRow(row);
            }

            JTable accountTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(accountTable);
            dialog.add(scrollPane);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving accounts: " + e.getMessage());
        }

        dialog.setVisible(true);
    }

    private void requestAccount() {
        try {
            String insertQuery = "INSERT INTO BankAccounts (user_id, balance, is_approved) VALUES (?, 0.00, false)";
            PreparedStatement pstmt = connection.prepareStatement(insertQuery);
            pstmt.setInt(1, currentUser.getUserId());
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, 
                    "Account request submitted successfully!\nPlease wait for employee approval.");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to submit account request.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error requesting account: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void makeTransaction() {
        try {
            String query = "SELECT account_id, balance FROM BankAccounts WHERE user_id = ? AND is_approved = true";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            DefaultComboBoxModel<String> accountModel = new DefaultComboBoxModel<>();
            while (rs.next()) {
                accountModel.addElement("Account " + rs.getInt("account_id") + 
                                      " (Balance: $" + rs.getDouble("balance") + ")");
            }

            if (accountModel.getSize() == 0) {
                JOptionPane.showMessageDialog(this, 
                    "You don't have any approved accounts to make transactions.",
                    "No Accounts",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog(this, "Make Transaction", true);
            dialog.setSize(400, 250);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            panel.add(new JLabel("Select Account:"));
            JComboBox<String> accountCombo = new JComboBox<>(accountModel);
            panel.add(accountCombo);

            panel.add(new JLabel("Transaction Type:"));
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Deposit", "Withdrawal"});
            panel.add(typeCombo);

            panel.add(new JLabel("Amount:"));
            JTextField amountField = new JTextField();
            panel.add(amountField);

            JButton submitButton = new JButton("Submit");
            JButton cancelButton = new JButton("Cancel");

            submitButton.addActionListener(e -> {
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    if (amount <= 0) {
                        JOptionPane.showMessageDialog(dialog, "Please enter a valid amount.");
                        return;
                    }

                    // Get the selected account ID from the combo box selection
                    String selectedAccount = (String) accountCombo.getSelectedItem();
                    int accountId = Integer.parseInt(selectedAccount.split(" ")[1]);

                    // Check if sufficient funds for withdrawal
                    if (typeCombo.getSelectedItem().equals("Withdrawal")) {
                        String balanceQuery = "SELECT balance FROM BankAccounts WHERE account_id = ?";
                        PreparedStatement balanceStmt = connection.prepareStatement(balanceQuery);
                        balanceStmt.setInt(1, accountId);
                        ResultSet balanceRs = balanceStmt.executeQuery();
                        
                        if (balanceRs.next()) {
                            double currentBalance = balanceRs.getDouble("balance");
                            if (amount > currentBalance) {
                                JOptionPane.showMessageDialog(dialog, "Insufficient funds!");
                                return;
                            }
                        }
                    }

                    // Update balance
                    String updateQuery = "UPDATE BankAccounts SET balance = balance " + 
                                       (typeCombo.getSelectedItem().equals("Deposit") ? "+" : "-") + 
                                       "? WHERE account_id = ?";
                    
                    PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                    updateStmt.setDouble(1, amount);
                    updateStmt.setInt(2, accountId);
                    
                    int result = updateStmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(dialog, 
                            typeCombo.getSelectedItem() + " of $" + amount + " successful!");
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Transaction failed!");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a valid amount.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Error processing transaction: " + ex.getMessage());
                }
            });

            cancelButton.addActionListener(e -> dialog.dispose());

            panel.add(submitButton);
            panel.add(cancelButton);

            dialog.add(panel);
            dialog.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error accessing accounts: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bookAppointment() {
        JDialog dialog = new JDialog(this, "Book Appointment", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Date (YYYY-MM-DD):"));
        JTextField dateField = new JTextField();
        panel.add(dateField);

        panel.add(new JLabel("Time:"));
        String[] times = {"09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00"};
        JComboBox<String> timeCombo = new JComboBox<>(times);
        panel.add(timeCombo);

        panel.add(new JLabel("Purpose:"));
        String[] purposes = {"Account Inquiry", "Loan Consultation", "Investment Planning", "Other"};
        JComboBox<String> purposeCombo = new JComboBox<>(purposes);
        panel.add(purposeCombo);

        JButton bookButton = new JButton("Book");
        JButton cancelButton = new JButton("Cancel");

        bookButton.addActionListener(e -> {
            try {
                String insertQuery = "INSERT INTO Appointments (customer_id, appointment_date, status) VALUES (?, ?, 'PENDING')";
                PreparedStatement pstmt = connection.prepareStatement(insertQuery);
                pstmt.setInt(1, currentUser.getUserId());
                pstmt.setString(2, dateField.getText() + " " + timeCombo.getSelectedItem());
                
                int result = pstmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(dialog, "Appointment booked successfully!");
                    dialog.dispose();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, 
                    "Error booking appointment: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        panel.add(bookButton);
        panel.add(cancelButton);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void logout() {
        dispose();
        new LoginFrame(connection).setVisible(true);
    }
}