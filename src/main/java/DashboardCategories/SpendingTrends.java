package DashboardCategories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpendingTrends {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Map<String, BigDecimal> currentTotals;
    private final Map<String, BigDecimal> previousTotals;

    public SpendingTrends(
            LocalDate startDate,
            LocalDate endDate,
            Map<String, BigDecimal> currentTotals,
            Map<String, BigDecimal> previousTotals) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentTotals = Collections.unmodifiableMap(
                new LinkedHashMap<>(currentTotals));
        this.previousTotals = Collections.unmodifiableMap(
                new LinkedHashMap<>(previousTotals));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Map<String, BigDecimal> getCurrentTotals() {
        return currentTotals;
    }

    public Map<String, BigDecimal> getPreviousTotals() {
        return previousTotals;
    }
}
