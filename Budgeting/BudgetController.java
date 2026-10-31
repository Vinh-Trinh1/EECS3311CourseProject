package Budgeting;

import DashboardCategories.DatabaseService;
import FinancialData.FinancialData;
import FinancialData.FinancialDataFacade;
import java.io.IOException;
import java.sql.SQLException;
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
}
