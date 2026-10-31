package DashboardCategories;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

public class DashboardController {
    private final DatabaseService databaseService;

    public DashboardController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void updateBalance(String accountEmail, BigDecimal balance)
            throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("Balance must be zero or greater.");
        }

        try {
            balance = balance.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Balance must have no more than two decimal places.", exception);
        }

        databaseService.saveBalance(accountEmail.trim(), balance);
    }

    public Category createCategory(String accountEmail, String name, BigDecimal balance)
            throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        validateCents(balance, "Category balance");

        Category category = new Category(name, balance.setScale(2));
        databaseService.saveCategory(owner, category);
        return category;
    }

    public List<Category> getCategories(String accountEmail) throws SQLException {
        return databaseService.getCategories(requireAccountEmail(accountEmail));
    }

    private static String requireAccountEmail(String accountEmail) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return accountEmail.trim();
    }

    private static void validateCents(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    fieldName + " must have no more than two decimal places.", exception);
        }
    }
}
