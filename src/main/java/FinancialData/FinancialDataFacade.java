package FinancialData;

import Budgeting.Budget;
import Budgeting.BudgetComparison;
import Budgeting.BudgetPeriod;
import DashboardCategories.Expense;
import DatabaseService.DatabaseService;
import SavingsGoals.SavingsGoal;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        appendBudgetContext(context, owner, LocalDate.now());
        return context.toString();
    }

    public Budget getBudget(String accountEmail, BudgetPeriod period) throws SQLException {
        return databaseService.getBudget(requireAccountEmail(accountEmail), period);
    }

    public BudgetComparison getBudgetComparison(
            String accountEmail, BudgetPeriod period, LocalDate referenceDate)
            throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        if (period == null) {
            throw new IllegalArgumentException("Budget period is required.");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("Comparison date is required.");
        }

        Budget budget = getBudget(owner, period);
        if (budget == null) {
            throw new IllegalStateException(
                    "No " + period.getValue() + " budget has been saved yet. "
                            + "Generate a budget recommendation first.");
        }

        LocalDate startDate;
        LocalDate endDate;
        if (period == BudgetPeriod.WEEKLY) {
            startDate = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = startDate.plusWeeks(1);
        } else {
            YearMonth month = YearMonth.from(referenceDate);
            startDate = month.atDay(1);
            endDate = month.plusMonths(1).atDay(1);
        }

        Map<String, BigDecimal> actualAmounts = databaseService.getExpenseTotalsBetween(
                owner, startDate, endDate);
        return new BudgetComparison(
                period,
                startDate,
                endDate.minusDays(1),
                budget.getCategoryAmounts(),
                actualAmounts);
    }

    public Map<String, java.math.BigDecimal> getExpenses(
            String accountEmail, YearMonth month) throws SQLException {
        return databaseService.getMonthlyExpenseTotals(
                requireAccountEmail(accountEmail), Objects.requireNonNull(month, "Month is required."));
    }

    private void appendBudgetContext(StringBuilder context, String owner, LocalDate today)
            throws SQLException {
        context.append(". Saved budgets and current-period comparisons: ");
        boolean foundBudget = false;
        for (BudgetPeriod period : BudgetPeriod.values()) {
            Budget budget = getBudget(owner, period);
            if (budget == null) {
                continue;
            }
            if (foundBudget) {
                context.append("; ");
            }
            foundBudget = true;
            context.append(period.getValue()).append(" budget (generated ")
                    .append(budget.getGeneratedAt()).append("): ");
            if (budget.getCategoryAmounts().isEmpty()) {
                context.append("no category limits");
            } else {
                boolean first = true;
                for (Map.Entry<String, BigDecimal> entry
                        : budget.getCategoryAmounts().entrySet()) {
                    if (!first) {
                        context.append(", ");
                    }
                    context.append(entry.getKey()).append(" $")
                            .append(entry.getValue().toPlainString());
                    first = false;
                }
            }
            context.append("; total $").append(budget.getTotalAmount().toPlainString())
                    .append("; recommendation: ").append(budget.getRecommendation());

            BudgetComparison comparison = getBudgetComparison(owner, period, today);
            context.append(". Current ").append(period.getValue())
                    .append(" comparison (").append(comparison.getStartDate())
                    .append(" through ").append(comparison.getEndDate()).append("): ");
            appendBudgetComparison(context, comparison);
        }
        if (!foundBudget) {
            context.append("no saved weekly or monthly budgets");
        }
    }

    private static void appendBudgetComparison(
            StringBuilder context, BudgetComparison comparison) {
        Set<String> categories = new LinkedHashSet<>(comparison.getBudgetAmounts().keySet());
        categories.addAll(comparison.getActualAmounts().keySet());
        if (categories.isEmpty()) {
            context.append("no budget categories or expenses recorded");
        } else {
            boolean first = true;
            for (String category : categories) {
                if (!first) {
                    context.append(", ");
                }
                BigDecimal variance = comparison.getVariance(category);
                context.append(category)
                        .append(" budget $").append(comparison.getBudgetAmount(category).toPlainString())
                        .append(", actual $").append(comparison.getActualAmount(category).toPlainString())
                        .append(", variance ")
                        .append(variance.signum() > 0 ? "+" : "")
                        .append("$").append(variance.toPlainString());
                first = false;
            }
            BigDecimal totalVariance = comparison.getTotalVariance();
            context.append("; total budget $").append(comparison.getBudgetTotal().toPlainString())
                    .append(", actual $").append(comparison.getActualTotal().toPlainString())
                    .append(", variance ")
                    .append(totalVariance.signum() > 0 ? "+" : "")
                    .append("$").append(totalVariance.toPlainString());
        }
        context.append(". Positive variance means spending is over budget; "
                + "negative means under budget");
    }

    private static String requireAccountEmail(String accountEmail) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return accountEmail.trim();
    }
}
