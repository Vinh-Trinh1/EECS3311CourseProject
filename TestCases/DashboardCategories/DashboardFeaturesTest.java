package DashboardCategories;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DashboardFeaturesTest {
    private DatabaseService databaseService;
    private DashboardController dashboardController;
    private ExpenseController expenseController;
    private String accountEmail;

    @Before
    public void setUp() throws SQLException {
        databaseService = DatabaseService.getInstance();
        dashboardController = new DashboardController(databaseService);
        expenseController = new ExpenseController(databaseService);
        accountEmail = "feature-test-" + UUID.randomUUID() + "@example.com";
    }

    @After
    public void tearDown() throws SQLException {
        try {
            for (String table : new String[] {
                "monthly_expenses", "dashboard_categories", "account_balances"
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
    public void categoriesCanBeCreatedUpdatedAndAllowNegativeBalances() throws SQLException {
        dashboardController.createCategory(
                accountEmail, "Checking", new BigDecimal("125.50"));
        dashboardController.createCategory(
                accountEmail, "checking", new BigDecimal("-12.25"));

        List<Category> categories = dashboardController.getCategories(accountEmail);
        assertEquals(1, categories.size());
        assertEquals("Checking", categories.get(0).getName());
        assertEquals(new BigDecimal("-12.25"), categories.get(0).getBalance());
        assertTrue(dashboardController.getCategories(
                "different-" + accountEmail).isEmpty());
    }

    @Test
    public void categoryGroupAggregatesNestedCategoryBalances() {
        Category checking = new Category("Checking", new BigDecimal("100.00"));
        Category savings = new Category("Savings", new BigDecimal("-25.00"));
        CategoryGroup accounts = new CategoryGroup("Accounts");
        CategoryGroup total = new CategoryGroup("Total");
        accounts.add(checking);
        accounts.add(savings);
        total.add(accounts);

        assertEquals(new BigDecimal("75.00"), total.getBalance());
        checking.updateBalance(new BigDecimal("80.00"));
        assertEquals(new BigDecimal("55.00"), total.getBalance());
        assertTrue(accounts.remove(savings));
        assertEquals(new BigDecimal("80.00"), total.getBalance());
    }

    @Test
    public void expenseIsSavedOnlyAfterCategoryAssignmentAndMonthlyTotalsAreScoped()
            throws SQLException {
        Expense expense = expenseController.recordExpense(
                new BigDecimal("12.35"), "Weekly grocery trip", LocalDate.of(2026, 10, 4));
        assertFalse(expenseController.getMonthlyExpenseTotals(
                accountEmail, YearMonth.of(2026, 10)).containsKey("Groceries"));

        expenseController.assignCategory(accountEmail, expense, "Groceries");
        Expense nextExpense = expenseController.recordExpense(
                new BigDecimal("7.65"), "Produce", LocalDate.of(2026, 10, 20));
        expenseController.assignCategory(accountEmail, nextExpense, "Groceries");
        Expense nextMonth = expenseController.recordExpense(
                new BigDecimal("9.00"), "Later shop", LocalDate.of(2026, 11, 1));
        expenseController.assignCategory(accountEmail, nextMonth, "Groceries");

        Map<String, BigDecimal> totals = expenseController.getMonthlyExpenseTotals(
                accountEmail, YearMonth.of(2026, 10));
        assertEquals(new BigDecimal("20.00"), totals.get("Groceries"));
        assertTrue(expenseController.getMonthlyExpenseTotals(
                "different-" + accountEmail, YearMonth.of(2026, 10)).isEmpty());
    }

    @Test
    public void categorizerCanBeInjectedAndFallsBackToManualWhenUnavailable()
            throws Exception {
        Expense expense = expenseController.recordExpense(
                new BigDecimal("12.00"), "Grocery store", LocalDate.now());
        assertFalse(expenseController.suggestCategory(expense).isPresent());

        ExpenseController aiController = new ExpenseController(
                databaseService, item -> "Groceries");
        assertEquals("Groceries", aiController.suggestCategory(expense).get());
    }

    @Test
    public void categoryAndExpenseAmountsRejectFractionalCents() throws SQLException {
        try {
            dashboardController.createCategory(
                    accountEmail, "Checking", new BigDecimal("1.001"));
            fail("Expected category balance validation.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("two decimal places"));
        }

        try {
            expenseController.recordExpense(
                    new BigDecimal("1.001"), "Test expense", LocalDate.now());
            fail("Expected expense amount validation.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("two decimal places"));
        }
    }
}
