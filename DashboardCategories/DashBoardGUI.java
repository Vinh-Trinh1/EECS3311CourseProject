package DashboardCategories;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class DashBoardGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final DashboardController controller;
    private final JTextField accountEmailField = new JTextField(24);
    private final JTextField balanceField = new JTextField(12);
    private final JLabel balanceLabel = new JLabel("Current balance: Not saved");
    private final JLabel statusLabel = new JLabel(" ");

    public DashBoardGUI(DashboardController controller) {
        super("Finance Dashboard");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Account email:"));
        form.add(accountEmailField);
        form.add(new JLabel("Balance:"));
        form.add(balanceField);

        JButton saveButton = new JButton("Save balance");
        saveButton.addActionListener(event -> saveBalance());

        JPanel content = new JPanel(new BorderLayout(8, 12));
        content.add(form, BorderLayout.NORTH);
        content.add(saveButton, BorderLayout.CENTER);
        content.add(balanceLabel, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setSize(420, 190);
        setLocationRelativeTo(null);
    }

    private void saveBalance() {
        String accountEmail = accountEmailField.getText().trim();
        if (accountEmail.isEmpty()) {
            showError("Enter an account email.");
            accountEmailField.requestFocusInWindow();
            return;
        }

        final BigDecimal balance;
        try {
            balance = new BigDecimal(balanceField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Enter a valid balance, for example 125.50.");
            balanceField.requestFocusInWindow();
            return;
        }

        try {
            controller.updateBalance(accountEmail, balance);
            displayBalance(balance.setScale(2));
            statusLabel.setText("Balance saved.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    public void displayBalance(BigDecimal balance) {
        balanceLabel.setText("Current balance: $" + balance.toPlainString());
    }

    private void showError(String message) {
        statusLabel.setText("Could not save balance.");
        JOptionPane.showMessageDialog(
                this, message, "Unable to save balance", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseService databaseService = DatabaseService.getInstance();
                DashboardController controller = new DashboardController(databaseService);
                new DashBoardGUI(controller).setVisible(true);
            } catch (SQLException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not connect to the SQLite database: " + exception.getMessage(),
                        "Database error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}