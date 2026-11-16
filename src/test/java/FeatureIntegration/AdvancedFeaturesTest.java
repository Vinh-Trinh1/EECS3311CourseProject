package FeatureIntegration;

import AIChat.AIController;
import AIIntegration.AIService;
import AIIntegration.AIServiceProxy;
import AIIntegration.ChatGPT5Response;
import AIIntegration.ChatGPTAdapter;
import AIIntegration.OfflineChatGPT5;
import Budgeting.Budget;
import Budgeting.BudgetAI;
import Budgeting.BudgetComparison;
import Budgeting.BudgetController;
import Budgeting.BudgetPeriod;
import DashboardCategories.Expense;
import DashboardCategories.ExpenseCategorizerAdapter;
import DashboardCategories.ExpenseController;
import DatabaseService.DatabaseService;
import FinancialData.FinancialDataFacade;
import SavingsGoals.SavingsController;
import SavingsGoals.SavingsAI;
import SavingsGoals.SavingsGoal;
import SavingsGoals.SavingsGoalBuilder;
import SavingsGoals.SavingsRecommendation;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
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
            throws SQLException, IOException {
        SavingsController controller = new SavingsController(
                databaseService,
                new FinancialDataFacade(databaseService),
                new SavingsAI(offlineAiService()));
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

        SavingsRecommendation recommendation =
                controller.generateSavingsRecommendation(accountEmail, goal.getId());
        assertEquals(new BigDecimal("62.50"), recommendation.getMonthlyTarget());
        assertTrue(recommendation.getAdvice().contains("calculated monthly target: $62.50"));
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

        AIService service = offlineAiService();
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
    public void budgetComparisonReportsPerCategoryAndTotalVariance()
            throws SQLException, IOException {
        Map<String, BigDecimal> limits = new LinkedHashMap<>();
        limits.put("Groceries", new BigDecimal("100.00"));
        limits.put("Transportation", new BigDecimal("50.00"));
        databaseService.saveBudget(Budget.createBudget(
                accountEmail, BudgetPeriod.WEEKLY, limits, "Weekly test budget"));
        ExpenseController expenseController = new ExpenseController(databaseService);
        LocalDate referenceDate = LocalDate.of(2026, 11, 15);
        Expense groceries = expenseController.recordExpense(
                new BigDecimal("25.00"), "Groceries", referenceDate);
        expenseController.assignCategory(accountEmail, groceries, "Groceries");
        Expense transportation = expenseController.recordExpense(
                new BigDecimal("60.00"), "Transit", referenceDate);
        expenseController.assignCategory(accountEmail, transportation, "Transportation");

        BudgetController controller = new BudgetController(
                new FinancialDataFacade(databaseService),
                new BudgetAI(offlineAiService()),
                databaseService);
        BudgetComparison comparison = controller.compareBudgetToActual(
                accountEmail, BudgetPeriod.WEEKLY, referenceDate);

        assertEquals(LocalDate.of(2026, 11, 9), comparison.getStartDate());
        assertEquals(referenceDate, comparison.getEndDate());
        assertEquals(new BigDecimal("150.00"), comparison.getBudgetTotal());
        assertEquals(new BigDecimal("85.00"), comparison.getActualTotal());
        assertEquals(new BigDecimal("10.00"), comparison.getVariance("Transportation"));
        assertEquals(new BigDecimal("-65.00"), comparison.getTotalVariance());
    }

    @Test
    public void chatUsesFacadeAndOfflineAdapterToAnswerFinancialQuestions()
            throws SQLException, IOException {
        databaseService.saveBalance(accountEmail, new BigDecimal("321.45"));
        FinancialDataFacade facade = new FinancialDataFacade(databaseService);
        AIController controller = new AIController(facade, offlineAiService());

        String answer = controller.askQuestion(accountEmail, "What is my balance?");

        assertTrue(answer.contains("$321.45"));
        assertTrue(answer.contains("historical"));
    }

    @Test
    public void chatIncludesTrackedSavingsWhenAskedHowMuchWasSaved()
            throws SQLException, IOException {
        SavingsController savingsController = new SavingsController(
                databaseService,
                new FinancialDataFacade(databaseService),
                new SavingsAI(offlineAiService()));
        SavingsGoal goal = savingsController.createGoal(
                accountEmail, "Emergency fund", new BigDecimal("1000.00"),
                12, "Checking");
        savingsController.addContribution(
                accountEmail, goal.getId(), new BigDecimal("250.00"));
        AIController controller = new AIController(
                new FinancialDataFacade(databaseService), offlineAiService());

        String answer = controller.askQuestion(accountEmail, "How much have I saved?");

        assertTrue(answer.contains("Total saved across tracked savings goals: $250.00"));
        assertTrue(answer.contains("Emergency fund saved $250.00 of $1000.00"));
    }

    @Test
    public void chatContextIncludesCurrentMonthExpenseDescriptionAndAmount()
            throws SQLException, IOException {
        ExpenseController expenseController = new ExpenseController(databaseService);
        Expense expense = expenseController.recordExpense(
                new BigDecimal("300.00"), "Lexus car payment", LocalDate.now());
        expenseController.assignCategory(accountEmail, expense, "Transportation");
        AIController controller = new AIController(
                new FinancialDataFacade(databaseService), offlineAiService());

        String answer = controller.askQuestion(
                accountEmail, "Am I spending too much on my current car?");

        assertTrue(answer.contains("Lexus car payment"));
        assertTrue(answer.contains("$300.00"));
    }

    @Test
    public void chatUsesSavingsGoalContextForQuestionsWithoutFinancialKeywords()
            throws SQLException, IOException {
        SavingsController savingsController = new SavingsController(
                databaseService,
                new FinancialDataFacade(databaseService),
                new SavingsAI(offlineAiService()));
        savingsController.createGoal(
                accountEmail, "Lexus", new BigDecimal("70000.00"), 24, "Checking");
        AIController controller = new AIController(
                new FinancialDataFacade(databaseService), offlineAiService());

        String answer = controller.askQuestion(accountEmail, "How much does the Lexus cost?");

        assertTrue(answer.contains("Lexus saved $0.00 of $70000.00"));
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
                new ExpenseCategorizerAdapter(offlineAiService())
                        .categorizeExpense(new Expense(
                                new BigDecimal("12.00"), LocalDate.now(), "grocery shop")));
    }

    private static AIService offlineAiService() {
        return new AIServiceProxy(new ChatGPTAdapter(new OfflineChatGPT5()));
    }
}
