package DashboardCategories;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
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
    private final JTextField categoryNameField = new JTextField(12);
    private final JTextField categoryBalanceField = new JTextField(12);
    private final JLabel balanceLabel = new JLabel("Current balance: Not saved");
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel categoriesLabel = new JLabel("Categories: None");

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

        JPanel categoryForm = new JPanel(new GridLayout(0, 2, 8, 8));
        categoryForm.add(new JLabel("Category name:"));
        categoryForm.add(categoryNameField);
        categoryForm.add(new JLabel("Category balance:"));
        categoryForm.add(categoryBalanceField);
        JButton categoryButton = new JButton("Add / update category");
        categoryButton.addActionListener(event -> saveCategory());
        JButton expenseButton = new JButton("Monthly expenses");
        expenseButton.addActionListener(event -> openExpenses());
        JButton savingsButton = new JButton("Savings goals");
        savingsButton.addActionListener(event -> openSavingsGoals());
        JButton chatButton = new JButton("AI finance chat");
        chatButton.addActionListener(event -> openChat());
        JButton budgetButton = new JButton("Suggested budget");
        budgetButton.addActionListener(event -> openBudget());

        JPanel dashboard = new JPanel(new BorderLayout(8, 12));
        dashboard.add(content, BorderLayout.NORTH);
        dashboard.add(categoryForm, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 1, 4, 4));
        actions.add(categoryButton);
        actions.add(categoriesLabel);
        actions.add(expenseButton);
        actions.add(savingsButton);
        actions.add(chatButton);
        actions.add(budgetButton);
        actions.add(balanceLabel);
        dashboard.add(actions, BorderLayout.SOUTH);

        add(dashboard, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setSize(520, 380);
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

    private void saveCategory() {
        String email = accountEmailField.getText().trim();
        final BigDecimal balance;
        try {
            balance = new BigDecimal(categoryBalanceField.getText().trim());
            controller.createCategory(email, categoryNameField.getText(), balance);
        } catch (NumberFormatException exception) {
            showError("Enter a valid category balance, for example -25.50.");
            categoryBalanceField.requestFocusInWindow();
            return;
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
            return;
        }

        try {
            refreshCategories(email);
            statusLabel.setText("Category saved.");
        } catch (SQLException exception) {
            showError("Category was saved, but the category list could not be refreshed: "
                    + exception.getMessage());
        }
    }

    private void refreshCategories(String email) throws SQLException {
        List<Category> categories = controller.getCategories(email);
        if (categories.isEmpty()) {
            categoriesLabel.setText("Categories: None");
            return;
        }
        StringBuilder text = new StringBuilder("Categories: ");
        for (int index = 0; index < categories.size(); index++) {
            if (index > 0) {
                text.append(", ");
            }
            Category category = categories.get(index);
            text.append(category.getName())
                    .append(" $")
                    .append(category.getBalance().setScale(2).toPlainString());
        }
        categoriesLabel.setText(text.toString());
    }

    private void openExpenses() {
        String email = accountEmailField.getText().trim();
        if (email.isEmpty()) {
            showError("Enter an account email before recording expenses.");
            accountEmailField.requestFocusInWindow();
            return;
        }
        try {
            ExpenseController expenseController = new ExpenseController(
                    DatabaseService.getInstance(),
                    new ExpenseCategorizerAdapter(AIServiceFactory.createDefaultService()));
            new ExpenseGUI(expenseController, email).setVisible(true);
        } catch (SQLException exception) {
            showError("Could not open monthly expenses: " + exception.getMessage());
        }
    }

    private void openSavingsGoals() {
        String email = requireAccountEmail("Enter an account email before managing savings goals.");
        if (email == null) {
            return;
        }
        try {
            SavingsController savingsController =
                    new SavingsController(DatabaseService.getInstance());
            new SavingsGoalGUI(savingsController, email).setVisible(true);
        } catch (SQLException exception) {
            showError("Could not open savings goals: " + exception.getMessage());
        }
    }

    private void openChat() {
        String email = requireAccountEmail("Enter an account email before opening the AI chat.");
        if (email == null) {
            return;
        }
        try {
            DatabaseService databaseService = DatabaseService.getInstance();
            AIController aiController = new AIController(
                    new FinancialDataFacade(databaseService),
                    AIServiceFactory.createDefaultService());
            new ChatGUI(aiController, email).setVisible(true);
        } catch (SQLException exception) {
            showError("Could not open the AI chat: " + exception.getMessage());
        }
    }

    private void openBudget() {
        String email = requireAccountEmail("Enter an account email before requesting a budget.");
        if (email == null) {
            return;
        }
        try {
            DatabaseService databaseService = DatabaseService.getInstance();
            AIService aiService = AIServiceFactory.createDefaultService();
            BudgetController budgetController = new BudgetController(
                    new FinancialDataFacade(databaseService),
                    new BudgetAI(aiService),
                    databaseService);
            new BudgetGUI(budgetController, email).setVisible(true);
        } catch (SQLException exception) {
            showError("Could not open budget recommendations: " + exception.getMessage());
        }
    }

    private String requireAccountEmail(String message) {
        String email = accountEmailField.getText().trim();
        if (email.isEmpty()) {
            showError(message);
            accountEmailField.requestFocusInWindow();
            return null;
        }
        return email;
    }

    private void showError(String message) {
        statusLabel.setText("Request failed.");
        JOptionPane.showMessageDialog(
                this, message, "Unable to complete request", JOptionPane.ERROR_MESSAGE);
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