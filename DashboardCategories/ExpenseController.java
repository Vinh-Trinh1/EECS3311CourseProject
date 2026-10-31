package DashboardCategories;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ExpenseController {
    private final DatabaseService databaseService;
    private final ExpenseCategorizerAI categorizer;

    public ExpenseController(DatabaseService databaseService) {
        this(databaseService, null);
    }

    public ExpenseController(
            DatabaseService databaseService, ExpenseCategorizerAI categorizer) {
        this.databaseService = Objects.requireNonNull(
                databaseService, "Database service is required.");
        this.categorizer = categorizer;
    }

    public Expense recordExpense(
            BigDecimal amount, String description, LocalDate date) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Expense amount must have no more than two decimal places.", exception);
        }
        return new Expense(amount, date, description);
    }

    public Optional<String> suggestCategory(Expense expense) throws IOException {
        Objects.requireNonNull(expense, "Expense is required.");
        if (categorizer == null) {
            return Optional.empty();
        }
        String suggestion = categorizer.categorizeExpense(expense);
        if (suggestion == null || suggestion.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(suggestion.trim());
    }

    public void assignCategory(String accountEmail, Expense expense, String category)
            throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        Objects.requireNonNull(expense, "Expense is required.");
        expense.assignCategory(category);
        databaseService.saveExpense(accountEmail.trim(), expense);
    }

    public List<Category> getCategories(String accountEmail) throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return databaseService.getCategories(accountEmail.trim());
    }

    public Map<String, BigDecimal> getMonthlyExpenseTotals(
            String accountEmail, YearMonth month) throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        Objects.requireNonNull(month, "Expense month is required.");
        return databaseService.getMonthlyExpenseTotals(accountEmail.trim(), month);
    }
}
