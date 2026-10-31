package DashboardCategories;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Budget {
    private final String accountEmail;
    private final BudgetPeriod period;
    private final Map<String, BigDecimal> categoryAmounts;
    private final String recommendation;
    private final LocalDateTime generatedAt;

    public Budget(
            String accountEmail,
            BudgetPeriod period,
            Map<String, BigDecimal> categoryAmounts,
            String recommendation,
            LocalDateTime generatedAt) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        if (period == null) {
            throw new IllegalArgumentException("Budget period is required.");
        }
        if (recommendation == null || recommendation.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget recommendation is required.");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Budget generation time is required.");
        }
        if (categoryAmounts == null) {
            throw new IllegalArgumentException("Budget categories are required.");
        }
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : categoryAmounts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()
                    || entry.getValue() == null || entry.getValue().signum() < 0) {
                throw new IllegalArgumentException("Budget categories must have valid amounts.");
            }
            try {
                amounts.put(
                        entry.getKey().trim(),
                        entry.getValue().setScale(2, RoundingMode.UNNECESSARY));
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Budget amounts must have no more than two decimal places.", exception);
            }
        }
        this.accountEmail = accountEmail.trim();
        this.period = period;
        this.categoryAmounts = Collections.unmodifiableMap(amounts);
        this.recommendation = recommendation.trim();
        this.generatedAt = generatedAt;
    }

    public static Budget createBudget(
            String accountEmail,
            BudgetPeriod period,
            Map<String, BigDecimal> categoryAmounts,
            String recommendation) {
        return new Budget(
                accountEmail, period, categoryAmounts, recommendation, LocalDateTime.now());
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public Map<String, BigDecimal> getCategoryAmounts() {
        return categoryAmounts;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : categoryAmounts.values()) {
            total = total.add(amount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
