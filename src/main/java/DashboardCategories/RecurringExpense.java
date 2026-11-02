package DashboardCategories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class RecurringExpense {
    private final String id;
    private final BigDecimal amount;
    private final String description;
    private final String category;
    private final RecurrenceInterval interval;
    private final LocalDate nextDueDate;
    private final LocalDate endDate;
    private final boolean active;
    private final int monthlyAnchorDay;
    private final boolean monthEndAnchor;

    public RecurringExpense(
            String id,
            BigDecimal amount,
            String description,
            String category,
            RecurrenceInterval interval,
            LocalDate nextDueDate,
            LocalDate endDate,
            boolean active) {
        this(id, amount, description, category, interval, nextDueDate, endDate, active,
                Objects.requireNonNull(nextDueDate, "Next due date is required.").getDayOfMonth(),
                nextDueDate.equals(YearMonth.from(nextDueDate).atEndOfMonth()));
    }

    public RecurringExpense(
            String id,
            BigDecimal amount,
            String description,
            String category,
            RecurrenceInterval interval,
            LocalDate nextDueDate,
            LocalDate endDate,
            boolean active,
            int monthlyAnchorDay,
            boolean monthEndAnchor) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Recurring expense ID is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Recurring expense amount must be greater than zero.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Recurring expense description is required.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Recurring expense category is required.");
        }
        if (monthlyAnchorDay < 1 || monthlyAnchorDay > 31) {
            throw new IllegalArgumentException("Monthly anchor day must be between 1 and 31.");
        }
        this.id = id.trim();
        this.amount = amount;
        this.description = description.trim();
        this.category = category.trim();
        this.interval = Objects.requireNonNull(interval, "Recurrence interval is required.");
        this.nextDueDate = Objects.requireNonNull(nextDueDate, "Next due date is required.");
        if (active && endDate != null && endDate.isBefore(nextDueDate)) {
            throw new IllegalArgumentException("End date must be on or after the next due date.");
        }
        this.endDate = endDate;
        this.active = active;
        this.monthlyAnchorDay = monthlyAnchorDay;
        this.monthEndAnchor = monthEndAnchor;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public RecurrenceInterval getInterval() {
        return interval;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public int getMonthlyAnchorDay() {
        return monthlyAnchorDay;
    }

    public boolean isMonthEndAnchor() {
        return monthEndAnchor;
    }

    public LocalDate nextDateAfter(LocalDate dueDate) {
        if (interval == RecurrenceInterval.WEEKLY) {
            return dueDate.plusWeeks(1);
        }
        YearMonth nextMonth = YearMonth.from(dueDate).plusMonths(1);
        int day = monthEndAnchor
                ? nextMonth.lengthOfMonth()
                : Math.min(monthlyAnchorDay, nextMonth.lengthOfMonth());
        return nextMonth.atDay(day);
    }
}
