package DashboardCategories;

import DatabaseService.DatabaseService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RecurringExpenseController {
    private final DatabaseService databaseService;

    public RecurringExpenseController(DatabaseService databaseService) {
        this.databaseService = Objects.requireNonNull(
                databaseService, "Database service is required.");
    }

    public RecurringExpense create(
            String accountEmail,
            BigDecimal amount,
            String description,
            String category,
            RecurrenceInterval interval,
            LocalDate nextDueDate,
            LocalDate endDate) throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        amount = validateAmount(amount);
        RecurringExpense recurrence = new RecurringExpense(
                UUID.randomUUID().toString(), amount, description, category,
                interval, nextDueDate, endDate, true);
        databaseService.saveRecurringExpense(owner, recurrence);
        return recurrence;
    }

    public RecurringExpense update(
            String accountEmail,
            String id,
            BigDecimal amount,
            String description,
            String category,
            RecurrenceInterval interval,
            LocalDate nextDueDate,
            LocalDate endDate) throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Recurring expense ID is required.");
        }
        RecurringExpense existing = null;
        for (RecurringExpense recurrence : databaseService.getRecurringExpenses(owner)) {
            if (recurrence.getId().equals(id.trim())) {
                existing = recurrence;
                break;
            }
        }
        if (existing == null) {
            throw new IllegalArgumentException("Recurring expense was not found.");
        }
        RecurringExpense updated = new RecurringExpense(
                existing.getId(), validateAmount(amount), description, category,
                interval, nextDueDate, endDate, existing.isActive());
        databaseService.updateRecurringExpense(owner, updated);
        return updated;
    }

    public List<RecurringExpense> getRecurringExpenses(String accountEmail)
            throws SQLException {
        return databaseService.getRecurringExpenses(requireAccountEmail(accountEmail));
    }

    public List<Category> getCategories(String accountEmail) throws SQLException {
        return databaseService.getCategories(requireAccountEmail(accountEmail));
    }

    public void setActive(String accountEmail, String id, boolean active)
            throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Recurring expense ID is required.");
        }
        if (active) {
            for (RecurringExpense recurrence : databaseService.getRecurringExpenses(owner)) {
                if (recurrence.getId().equals(id.trim())
                        && recurrence.getEndDate() != null
                        && recurrence.getNextDueDate().isAfter(recurrence.getEndDate())) {
                    throw new IllegalArgumentException(
                            "This recurring expense has ended and cannot be resumed.");
                }
            }
        }
        databaseService.setRecurringExpenseActive(owner, id.trim(), active);
    }

    public void delete(String accountEmail, String id) throws SQLException {
        requireAccountEmail(accountEmail);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Recurring expense ID is required.");
        }
        databaseService.deleteRecurringExpense(accountEmail.trim(), id.trim());
    }

    public int processDueExpenses(String accountEmail, LocalDate today) throws SQLException {
        return databaseService.processDueRecurringExpenses(
                requireAccountEmail(accountEmail), Objects.requireNonNull(today, "Date is required."));
    }

    private static String requireAccountEmail(String accountEmail) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return accountEmail.trim();
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Recurring expense amount must be greater than zero.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Recurring expense amount must have no more than two decimal places.",
                    exception);
        }
    }
}
