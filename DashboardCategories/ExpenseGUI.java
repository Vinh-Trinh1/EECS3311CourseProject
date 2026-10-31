package DashboardCategories;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ExpenseGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String[] PREDEFINED_CATEGORIES = {
        "Groceries", "Car payment", "Bills", "Housing", "Transportation",
        "Healthcare", "Entertainment", "Other"
    };

    private final ExpenseController controller;
    private final String accountEmail;
    private final JTextField amountField = new JTextField(12);
    private final JTextField descriptionField = new JTextField(20);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JComboBox<String> categoryField = new JComboBox<>(PREDEFINED_CATEGORIES);
    private final JTextArea monthlyTotals = new JTextArea(8, 32);
    private final JLabel statusLabel = new JLabel(" ");

    public ExpenseGUI(ExpenseController controller, String accountEmail) throws SQLException {
        super("Monthly Expenses");
        this.controller = controller;
        this.accountEmail = accountEmail;

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Amount:"));
        form.add(amountField);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(dateField);
        form.add(new JLabel("Category:"));
        form.add(categoryField);
        addUserCategories();
        categoryField.setSelectedIndex(-1);

        JButton saveButton = new JButton("Categorize and save expense");
        saveButton.addActionListener(event -> saveExpense());
        monthlyTotals.setEditable(false);
        monthlyTotals.setLineWrap(true);
        monthlyTotals.setWrapStyleWord(true);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(form, BorderLayout.NORTH);
        content.add(new JScrollPane(monthlyTotals), BorderLayout.CENTER);
        content.add(saveButton, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(440, 380);
        setLocationRelativeTo(null);
        refreshMonthlyTotals(YearMonth.now());
    }

    private void addUserCategories() throws SQLException {
        for (Category category : controller.getCategories(accountEmail)) {
            if (findCategory(category.getName()) == null) {
                categoryField.addItem(category.getName());
            }
        }
    }

    private String findCategory(String category) {
        for (int index = 0; index < categoryField.getItemCount(); index++) {
            String available = categoryField.getItemAt(index);
            if (available.equalsIgnoreCase(category)) {
                return available;
            }
        }
        return null;
    }

    private void saveExpense() {
        final BigDecimal amount;
        final LocalDate date;
        try {
            amount = new BigDecimal(amountField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Enter a valid expense amount, for example 12.50.");
            amountField.requestFocusInWindow();
            return;
        }
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException exception) {
            showError("Enter a valid date in YYYY-MM-DD format.");
            dateField.requestFocusInWindow();
            return;
        }

        try {
            Expense expense = controller.recordExpense(
                    amount, descriptionField.getText(), date);
            Optional<String> suggestion;
            boolean categorizerFailed = false;
            try {
                suggestion = controller.suggestCategory(expense);
            } catch (IOException exception) {
                suggestion = Optional.empty();
                categorizerFailed = true;
                JOptionPane.showMessageDialog(
                        this,
                        "AI categorization failed: " + exception.getMessage()
                                + "\nChoose a category manually.",
                        "Manual category selection",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (!suggestion.isPresent() && !categorizerFailed) {
                JOptionPane.showMessageDialog(
                        this,
                        "No AI category suggestion is available. Choose a category manually.",
                        "Manual category selection",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (suggestion.isPresent()) {
                String suggestedCategory = suggestion.get();
                String availableCategory = findCategory(suggestedCategory);
                if (availableCategory == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            "AI could not match this expense to an available category. "
                                    + "Choose one manually.",
                            "Manual category selection",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "The suggested category is \"" + availableCategory
                                    + "\". Is this correct?",
                            "Confirm expense category",
                            JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        categoryField.setSelectedItem(availableCategory);
                    }
                }
            }

            String selectedCategory = (String) categoryField.getSelectedItem();
            if (selectedCategory == null) {
                showError("Choose a category before saving the expense.");
                return;
            }
            controller.assignCategory(accountEmail, expense, selectedCategory);
            categoryField.setSelectedIndex(-1);
            statusLabel.setText("Expense saved.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
            return;
        }

        try {
            refreshMonthlyTotals(YearMonth.from(date));
        } catch (SQLException exception) {
            statusLabel.setText("Expense saved; monthly totals could not be refreshed.");
            JOptionPane.showMessageDialog(
                    this,
                    "Expense was saved, but monthly totals could not be refreshed: "
                            + exception.getMessage(),
                    "Monthly totals unavailable",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshMonthlyTotals(YearMonth month) throws SQLException {
        Map<String, BigDecimal> totals =
                controller.getMonthlyExpenseTotals(accountEmail, month);
        StringBuilder text = new StringBuilder("Expenses for ").append(month).append('\n');
        if (totals.isEmpty()) {
            text.append("No expenses recorded this month.");
        } else {
            BigDecimal monthlyTotal = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
                text.append(entry.getKey())
                        .append(": $")
                        .append(entry.getValue().setScale(2).toPlainString())
                        .append('\n');
                monthlyTotal = monthlyTotal.add(entry.getValue());
            }
            text.append("Total: $").append(monthlyTotal.setScale(2).toPlainString());
        }
        monthlyTotals.setText(text.toString());
    }

    private void showError(String message) {
        statusLabel.setText("Could not save expense.");
        JOptionPane.showMessageDialog(
                this, message, "Unable to save expense", JOptionPane.ERROR_MESSAGE);
    }
}
