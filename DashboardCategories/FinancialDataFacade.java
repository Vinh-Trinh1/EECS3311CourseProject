package DashboardCategories;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;

public class FinancialDataFacade {
    private final DatabaseService databaseService;

    public FinancialDataFacade(DatabaseService databaseService) {
        this.databaseService = Objects.requireNonNull(
                databaseService, "Database service is required.");
    }

    public FinancialData getFinancialData(String accountEmail) throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        LocalDate end = YearMonth.now().atDay(1);
        LocalDate start = YearMonth.from(end).minusMonths(3).atDay(1);
        Map<String, java.math.BigDecimal> expenses =
                databaseService.getExpenseTotalsBetween(owner, start, end);
        for (Map.Entry<String, java.math.BigDecimal> entry : expenses.entrySet()) {
            entry.setValue(entry.getValue().divide(
                    java.math.BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP));
        }
        return new FinancialData(
                databaseService.getAccountBalance(owner),
                expenses,
                databaseService.getSavingsGoals(owner));
    }

    public Budget getBudget(String accountEmail, BudgetPeriod period) throws SQLException {
        return databaseService.getBudget(requireAccountEmail(accountEmail), period);
    }

    public Map<String, java.math.BigDecimal> getExpenses(
            String accountEmail, YearMonth month) throws SQLException {
        return databaseService.getMonthlyExpenseTotals(
                requireAccountEmail(accountEmail), Objects.requireNonNull(month, "Month is required."));
    }

    private static String requireAccountEmail(String accountEmail) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return accountEmail.trim();
    }
}
