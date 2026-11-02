package DashboardCategories;

import DatabaseService.DatabaseService;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecurringAndTrendsTest {
    private DatabaseService databaseService;
    private String accountEmail;
    private RecurringExpenseController recurringController;
    private SpendingTrendsController trendsController;
    private ExpenseController expenseController;

    @Before
    public void setUp() throws SQLException {
        databaseService = DatabaseService.getInstance();
        accountEmail = "recurring-" + UUID.randomUUID() + "@example.com";
        recurringController = new RecurringExpenseController(databaseService);
        trendsController = new SpendingTrendsController(databaseService);
        expenseController = new ExpenseController(databaseService);
    }

    @After
    public void tearDown() throws SQLException {
        if (databaseService == null) {
            return;
        }
        try {
            for (String table : new String[] {
                "recurring_expenses", "monthly_expenses", "dashboard_categories"
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
    public void weeklyRecurrenceCatchesUpAndDoesNotDuplicateOccurrences() throws SQLException {
        recurringController.create(
                accountEmail, new BigDecimal("25.50"), "Internet", "Bills",
                RecurrenceInterval.WEEKLY, LocalDate.of(2026, 10, 1), null);

        assertEquals(3, recurringController.processDueExpenses(
                accountEmail, LocalDate.of(2026, 10, 15)));
        assertEquals(0, recurringController.processDueExpenses(
                accountEmail, LocalDate.of(2026, 10, 15)));
        assertEquals(
                new BigDecimal("76.50"),
                databaseService.getExpenseTotalsBetween(
                        accountEmail, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 16))
                        .get("Bills"));
        assertEquals(
                LocalDate.of(2026, 10, 22),
                recurringController.getRecurringExpenses(accountEmail).get(0).getNextDueDate());
    }

    @Test
    public void monthlyEndOfMonthRecurrenceRemainsAtMonthEnd() throws SQLException {
        recurringController.create(
                accountEmail, new BigDecimal("1000.00"), "Rent", "Housing",
                RecurrenceInterval.MONTHLY, LocalDate.of(2026, 1, 31), null);

        assertEquals(3, recurringController.processDueExpenses(
                accountEmail, LocalDate.of(2026, 3, 31)));
        RecurringExpense recurrence = recurringController
                .getRecurringExpenses(accountEmail).get(0);

        assertEquals(LocalDate.of(2026, 4, 30), recurrence.getNextDueDate());
    }

    @Test
    public void pausedRecurringExpenseDoesNotRecordDueExpenses() throws SQLException {
        RecurringExpense recurrence = recurringController.create(
                accountEmail, new BigDecimal("15.00"), "Music", "Entertainment",
                RecurrenceInterval.MONTHLY, LocalDate.of(2026, 10, 1), null);
        recurringController.setActive(accountEmail, recurrence.getId(), false);

        assertEquals(0, recurringController.processDueExpenses(
                accountEmail, LocalDate.of(2026, 10, 30)));
        assertTrue(databaseService.getExpenseTotalsBetween(
                accountEmail, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)).isEmpty());
        assertFalse(recurringController.getRecurringExpenses(accountEmail).get(0).isActive());
    }

    @Test
    public void recurrenceCanBeEditedWithoutChangingItsActiveState() throws SQLException {
        RecurringExpense original = recurringController.create(
                accountEmail, new BigDecimal("15.00"), "Music", "Entertainment",
                RecurrenceInterval.MONTHLY, LocalDate.of(2026, 10, 1), null);
        recurringController.setActive(accountEmail, original.getId(), false);

        recurringController.update(
                accountEmail, original.getId(), new BigDecimal("20.50"),
                "Video subscription", "Entertainment", RecurrenceInterval.MONTHLY,
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 1));

        RecurringExpense updated = recurringController
                .getRecurringExpenses(accountEmail).get(0);
        assertEquals(new BigDecimal("20.50"), updated.getAmount());
        assertEquals("Video subscription", updated.getDescription());
        assertEquals(LocalDate.of(2026, 11, 1), updated.getNextDueDate());
        assertEquals(LocalDate.of(2027, 1, 1), updated.getEndDate());
        assertFalse(updated.isActive());
    }

    @Test
    public void recurrenceEndDateIsInclusiveAndStopsFutureOccurrences() throws SQLException {
        recurringController.create(
                accountEmail, new BigDecimal("8.00"), "Streaming", "Entertainment",
                RecurrenceInterval.WEEKLY,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8));

        assertEquals(2, recurringController.processDueExpenses(
                accountEmail, LocalDate.of(2026, 10, 20)));
        RecurringExpense recurrence = recurringController
                .getRecurringExpenses(accountEmail).get(0);
        assertFalse(recurrence.isActive());
        assertEquals(LocalDate.of(2026, 10, 15), recurrence.getNextDueDate());
        try {
            recurringController.setActive(accountEmail, recurrence.getId(), true);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ended"));
            return;
        }
        throw new AssertionError("An ended recurrence should not be resumable.");
    }

    @Test
    public void spendingTrendsCompareSameLengthAdjacentPeriods() throws SQLException {
        Expense previous = expenseController.recordExpense(
                new BigDecimal("10.00"), "Earlier groceries", LocalDate.of(2026, 10, 3));
        expenseController.assignCategory(accountEmail, previous, "Groceries");
        Expense current = expenseController.recordExpense(
                new BigDecimal("15.00"), "Current groceries", LocalDate.of(2026, 10, 10));
        expenseController.assignCategory(accountEmail, current, "Groceries");
        Expense anotherCurrent = expenseController.recordExpense(
                new BigDecimal("4.00"), "Bus fare", LocalDate.of(2026, 10, 12));
        expenseController.assignCategory(accountEmail, anotherCurrent, "Transport");

        SpendingTrends trends = trendsController.getTrends(
                accountEmail, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 14));

        assertEquals(new BigDecimal("15.00"), trends.getCurrentTotals().get("Groceries"));
        assertEquals(new BigDecimal("4.00"), trends.getCurrentTotals().get("Transport"));
        assertEquals(new BigDecimal("10.00"), trends.getPreviousTotals().get("Groceries"));
        assertFalse(trends.getPreviousTotals().containsKey("Transport"));
    }

    @Test
    public void spendingTrendsRejectInvalidDateRange() {
        try {
            trendsController.getTrends(
                    accountEmail, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 14));
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("valid"));
            return;
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
        throw new AssertionError("Expected invalid date range to be rejected.");
    }

    @Test
    public void recurrenceManagementIsScopedToAccount() throws SQLException {
        recurringController.create(
                accountEmail, new BigDecimal("10.00"), "Phone bill", "Bills",
                RecurrenceInterval.MONTHLY, LocalDate.of(2026, 11, 1), null);

        List<RecurringExpense> otherAccount =
                recurringController.getRecurringExpenses("other-" + accountEmail);
        Map<String, BigDecimal> otherTrends = databaseService.getExpenseTotalsBetween(
                "other-" + accountEmail, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));

        assertTrue(otherAccount.isEmpty());
        assertTrue(otherTrends.isEmpty());
    }
}
