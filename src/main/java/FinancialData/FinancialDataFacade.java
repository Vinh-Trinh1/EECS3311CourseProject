package FinancialData;

import Budgeting.Budget;
import Budgeting.BudgetPeriod;
import DashboardCategories.Expense;
import DatabaseService.DatabaseService;
import SavingsGoals.SavingsGoal;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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

    public String getChatFinancialContext(String accountEmail) throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        YearMonth currentMonth = YearMonth.now();
        List<Expense> recentExpenses = databaseService.getExpensesBetween(
                owner,
                currentMonth.minusMonths(2).atDay(1),
                LocalDate.now().plusDays(1));
        StringBuilder context = new StringBuilder(getFinancialData(owner).toPromptSummary())
                .append(". Individual expenses recorded in the last three calendar months "
                        + "(newest first, up to 50): ");
        if (recentExpenses.isEmpty()) {
            context.append("none recorded");
        } else {
            for (int index = 0; index < recentExpenses.size(); index++) {
                if (index > 0) {
                    context.append("; ");
                }
                Expense expense = recentExpenses.get(index);
                String description = expense.getDescription();
                if (description.length() > 120) {
                    description = description.substring(0, 120);
                }
                context.append(expense.getDate()).append(", ")
                        .append(expense.getCategory()).append(", \"")
                        .append(description).append("\" - $")
                        .append(expense.getAmount().setScale(2).toPlainString());
            }
        }
        return context.toString();
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
