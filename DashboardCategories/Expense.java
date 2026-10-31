package DashboardCategories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Expense {
    private final BigDecimal amount;
    private final LocalDate date;
    private final String description;
    private String category;

    public Expense(BigDecimal amount, LocalDate date, String description) {
        this.amount = Objects.requireNonNull(amount, "Expense amount is required.");
        this.date = Objects.requireNonNull(date, "Expense date is required.");
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Expense description is required.");
        }
        this.description = description.trim();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public void assignCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Expense category is required.");
        }
        this.category = category.trim();
    }
}
