package FeatureIntegration;

import AIChat.AIController;
import AIIntegration.AIService;
import AIIntegration.AIServiceFactory;
import AIIntegration.AIServiceProxy;
import AIIntegration.ChatGPT5Response;
import AIIntegration.ChatGPTAdapter;
import Budgeting.Budget;
import Budgeting.BudgetAI;
import Budgeting.BudgetController;
import Budgeting.BudgetPeriod;
import DashboardCategories.DatabaseService;
import DashboardCategories.Expense;
import DashboardCategories.ExpenseCategorizerAdapter;
import DashboardCategories.ExpenseController;
import FinancialData.FinancialDataFacade;
import SavingsGoals.SavingsController;
import SavingsGoals.SavingsGoal;
import SavingsGoals.SavingsGoalBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AdvancedFeaturesTest {
    private DatabaseService databaseService;
    private String accountEmail;

    @Before
    public void setUp() throws SQLException {
        databaseService = DatabaseService.getInstance();
        accountEmail = "advanced-" + UUID.randomUUID() + "@example.com";
    }

    @After
    public void tearDown() throws SQLException {
        try {
            for (String table : new String[] {
                "savings_goals", "budgets", "monthly_expenses",
                "dashboard_categories", "account_balances"
            }) {
                try (PreparedStatement statement = databaseService.getConnection()
                        .prepareStatement("DELETE FROM " + table + " WHERE account_email = ?")) {
                    statement.setString(1, accountEmail);
                    statement.executeUpdate();
                }
            }
        } finally {
            databaseService.close();
        }
    }

    @Test
    public void savingsGoalsAreBuiltLinkedPersistedAndTrackContributions()
            throws SQLException {
        SavingsController controller = new SavingsController(databaseService);
        SavingsGoal goal = controller.createGoal(
                accountEmail, "Emergency fund", new BigDecimal("1000.00"),
                12, "Checking");
        SavingsGoal updated = controller.addContribution(
                accountEmail, goal.getId(), new BigDecimal("250.00"));

        assertEquals(new BigDecimal("25.00"), updated.calculateProgress());
        assertEquals(accountEmail, updated.getLinkedAccount().getOwnerUserId());
        SavingsGoal persisted = controller.getGoals(accountEmail).get(0);
        assertEquals(goal.getId(), persisted.getId());
        assertEquals(new BigDecimal("250.00"), persisted.getCurrentAmount());
        assertEquals(new BigDecimal("25.00"), persisted.calculateProgress());
    }

    @Test
    public void savingsGoalBuilderRejectsIncompleteGoals() {
        try {
            new SavingsGoalBuilder().build();
            fail("Expected the builder to reject missing goal information.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("required"));
        }
    }

    @Test
    public void budgetUsesThreeCompletedMonthsAndPersistsRecommendation()
            throws SQLException, IOException {
        ExpenseController expenses = new ExpenseController(databaseService);
        YearMonth currentMonth = YearMonth.now();
        for (int monthsAgo = 1; monthsAgo <= 3; monthsAgo++) {
            LocalDate date = currentMonth.minusMonths(monthsAgo).atDay(10);
            Expense expense = expenses.recordExpense(
                    new BigDecimal("10.00"), "grocery shop", date);
            expenses.assignCategory(accountEmail, expense, "Groceries");
        }

        AIService service = AIServiceFactory.createDefaultService();
        FinancialDataFacade facade = new FinancialDataFacade(databaseService);
        BudgetController controller = new BudgetController(
                facade, new BudgetAI(service), databaseService);
        Budget budget = controller.generateBudget(accountEmail, BudgetPeriod.MONTHLY);

        assertEquals(new BigDecimal("9.50"), budget.getCategoryAmounts().get("Groceries"));
        assertEquals("monthly", controller.getBudget(
                accountEmail, BudgetPeriod.MONTHLY).getPeriod().getValue());
        assertTrue(budget.getRecommendation().contains("offline demo"));
    }

    @Test
    public void chatUsesFacadeAndOfflineAdapterToAnswerFinancialQuestions()
            throws SQLException, IOException {
        databaseService.saveBalance(accountEmail, new BigDecimal("321.45"));
        FinancialDataFacade facade = new FinancialDataFacade(databaseService);
        AIController controller = new AIController(facade, AIServiceFactory.createDefaultService());

        String answer = controller.askQuestion(accountEmail, "What is my balance?");

        assertTrue(answer.contains("$321.45"));
        assertTrue(answer.contains("historical"));
    }

    @Test
    public void aiProxyCachesAndAdapterTranslatesProviderResponses() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        AIService service = new AIServiceProxy(new ChatGPTAdapter(prompt -> {
            calls.incrementAndGet();
            return new ChatGPT5Response(" adapted response ");
        }));

        assertEquals("adapted response", service.generateResponse("same prompt"));
        assertEquals("adapted response", service.generateResponse("same prompt"));
        assertEquals(1, calls.get());
        assertEquals(
                "Groceries",
                new ExpenseCategorizerAdapter(AIServiceFactory.createDefaultService())
                        .categorizeExpense(new Expense(
                                new BigDecimal("12.00"), LocalDate.now(), "grocery shop")));
    }
}
