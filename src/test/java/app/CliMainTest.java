package app;

import AIIntegration.AIService;
import AccountCreation.AccountController;
import AccountCreation.User;
import Budgeting.Budget;
import Budgeting.BudgetPeriod;
import DashboardCategories.Expense;
import DashboardCategories.ExpenseController;
import DatabaseService.DatabaseService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CliMainTest {
    private final String email = "cli-" + UUID.randomUUID() + "@example.com";
    private final String phone = "+1" + String.format("%010d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 1000000000L));

    @After
    public void tearDown() throws Exception {
        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            try (PreparedStatement statement = databaseService.getConnection()
                    .prepareStatement("DELETE FROM monthly_expenses WHERE account_email = ?")) {
                statement.setString(1, email);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = databaseService.getConnection()
                    .prepareStatement("DELETE FROM budgets WHERE account_email = ?")) {
                statement.setString(1, email);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = databaseService.getConnection()
                    .prepareStatement("DELETE FROM account_balances WHERE account_email = ?")) {
                statement.setString(1, email);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = databaseService.getConnection()
                    .prepareStatement("DELETE FROM account_users WHERE email = ?")) {
                statement.setString(1, email);
                statement.executeUpdate();
            }
        } finally {
            databaseService.close();
        }
    }

    @Test
    public void printsHelpForCliRequests() {
        assertEquals(0, CliMain.run(new String[] {"--help"}));
    }

    @Test
    public void createsAccountViaCliCommand() throws Exception {
        int exitCode = CliMain.run(new String[] {
            "account",
            "create",
            "--email",
            email,
            "--phone",
            phone,
            "--password",
            "CorrectHorse!",
            "--contact",
            "email"
        });

        assertEquals(0, exitCode);

        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            assertNotNull(databaseService.findUserEmail(email));
        } finally {
            databaseService.close();
        }
    }

    @Test
    public void assistantChatRetrievesFinancialContextAndKeepsConversationMemory()
            throws Exception {
        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            new AccountController(databaseService).createAccount(
                    new User(email, phone, "CorrectHorse!", "CorrectHorse!", "email"));
            databaseService.saveBalance(email, new BigDecimal("321.45"));
            Map<String, BigDecimal> weeklyLimits = new LinkedHashMap<>();
            weeklyLimits.put("Groceries", new BigDecimal("100.00"));
            databaseService.saveBudget(Budget.createBudget(
                    email, BudgetPeriod.WEEKLY, weeklyLimits, "Keep grocery spending on track."));
            Map<String, BigDecimal> monthlyLimits = new LinkedHashMap<>();
            monthlyLimits.put("Groceries", new BigDecimal("200.00"));
            databaseService.saveBudget(Budget.createBudget(
                    email, BudgetPeriod.MONTHLY, monthlyLimits, "Stay within the monthly limit."));
            ExpenseController expenseController = new ExpenseController(databaseService);
            Expense expense = expenseController.recordExpense(
                    new BigDecimal("25.00"), "grocery shopping", LocalDate.now());
            expenseController.assignCategory(email, expense, "Groceries");
        } finally {
            databaseService.close();
        }

        final List<String> prompts = new ArrayList<>();
        AIService testAiService = prompt -> {
            prompts.add(prompt);
            return "Demo assistant response.";
        };
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        String questions = "What is my balance?\nAnd remind me what you said.\nexit\n";

        int exitCode = CliMain.run(
                new String[] {"assistant", "chat", "--email", email},
                new ByteArrayInputStream(questions.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(outputBytes),
                new PrintStream(errorBytes),
                testAiService);

        assertEquals(0, exitCode);
        assertEquals(2, prompts.size());
        assertTrue(prompts.get(0).contains("Financial context:"));
        assertTrue(prompts.get(0).contains("$321.45"));
        assertTrue(prompts.get(0).contains("weekly budget"));
        assertTrue(prompts.get(0).contains("Groceries $100.00"));
        assertTrue(prompts.get(0).contains("recommendation: Keep grocery spending on track."));
        assertTrue(prompts.get(0).contains("Current weekly comparison"));
        assertTrue(prompts.get(0).contains("Groceries budget $100.00, actual $25.00"));
        assertTrue(prompts.get(0).contains("variance $-75.00"));
        assertTrue(prompts.get(0).contains("monthly budget"));
        assertTrue(prompts.get(0).contains("Current monthly comparison"));
        assertTrue(prompts.get(0).contains("Groceries budget $200.00, actual $25.00"));
        assertTrue(prompts.get(0).contains("variance $-175.00"));
        assertTrue(prompts.get(1).contains("Recent conversation:"));
        assertTrue(prompts.get(1).contains("Demo assistant response."));
        assertTrue(outputBytes.toString("UTF-8").contains("Demo assistant response."));
        assertEquals("", errorBytes.toString("UTF-8"));
    }
}
