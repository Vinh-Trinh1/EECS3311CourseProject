package Budgeting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BudgetComparison {
    private final BudgetPeriod period;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Map<String, BigDecimal> budgetAmounts;
    private final Map<String, BigDecimal> actualAmounts;

    public BudgetComparison(
            BudgetPeriod period,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, BigDecimal> budgetAmounts,
            Map<String, BigDecimal> actualAmounts) {
        if (period == null || startDate == null || endDate == null
                || !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("A valid comparison period is required.");
        }
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetAmounts = Collections.unmodifiableMap(
                new LinkedHashMap<>(budgetAmounts));
        this.actualAmounts = Collections.unmodifiableMap(
                new LinkedHashMap<>(actualAmounts));
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Map<String, BigDecimal> getBudgetAmounts() {
        return budgetAmounts;
    }

    public Map<String, BigDecimal> getActualAmounts() {
        return actualAmounts;
    }

    public BigDecimal getBudgetAmount(String category) {
        return budgetAmounts.getOrDefault(category, BigDecimal.ZERO.setScale(2));
    }

    public BigDecimal getActualAmount(String category) {
        return actualAmounts.getOrDefault(category, BigDecimal.ZERO.setScale(2));
    }

    public BigDecimal getBudgetTotal() {
        return total(budgetAmounts);
    }

    public BigDecimal getActualTotal() {
        return total(actualAmounts);
    }

    public BigDecimal getVariance(String category) {
        return getActualAmount(category).subtract(getBudgetAmount(category))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalVariance() {
        return getActualTotal().subtract(getBudgetTotal()).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal total(Map<String, BigDecimal> amounts) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal amount : amounts.values()) {
            result = result.add(amount);
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }
}
