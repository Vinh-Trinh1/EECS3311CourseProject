package Budgeting;

import DatabaseService.DatabaseService;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import FinancialData.FinancialData;
import FinancialData.FinancialDataFacade;
import java.io.IOException;
import java.util.Objects;

public class BudgetController {
    private final FinancialDataFacade financialDataFacade;
    private final BudgetAI budgetAI;
    private final DatabaseService databaseService;

    public BudgetController(
            FinancialDataFacade financialDataFacade,
            BudgetAI budgetAI,
            DatabaseService databaseService) {
        this.financialDataFacade = Objects.requireNonNull(
                financialDataFacade, "Financial data facade is required.");
        this.budgetAI = Objects.requireNonNull(budgetAI, "Budget AI is required.");
        this.databaseService = Objects.requireNonNull(
                databaseService, "Database service is required.");
    }

    public Budget generateBudget(String accountEmail, BudgetPeriod period)
            throws IOException, SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        FinancialData data = financialDataFacade.getFinancialData(accountEmail.trim());
        Recommendation recommendation = budgetAI.analyzeFinances(data, period);
        Budget budget = Budget.createBudget(
                accountEmail,
                period,
                recommendation.getCategoryAmounts(),
                recommendation.getAdvice());
        databaseService.saveBudget(budget);
        return budget;
    }

    public Budget getBudget(String accountEmail, BudgetPeriod period) throws SQLException {
        return financialDataFacade.getBudget(accountEmail, period);
    }

    public BudgetComparison compareBudgetToActual(
            String accountEmail, BudgetPeriod period, LocalDate referenceDate)
            throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        if (period == null) {
            throw new IllegalArgumentException("Budget period is required.");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("Comparison date is required.");
        }

        Budget budget = getBudget(accountEmail.trim(), period);
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
                accountEmail.trim(), startDate, endDate);
        return new BudgetComparison(
                period,
                startDate,
                endDate.minusDays(1),
                budget.getCategoryAmounts(),
                actualAmounts);
    }
}
