package DashboardCategories;

import DatabaseService.DatabaseService;
import java.time.DateTimeException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

public final class SpendingTrendsController {
    private final DatabaseService databaseService;

    public SpendingTrendsController(DatabaseService databaseService) {
        this.databaseService = Objects.requireNonNull(
                databaseService, "Database service is required.");
    }

    public SpendingTrends getTrends(
            String accountEmail, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Select a valid start and end date.");
        }
        final LocalDate currentEndExclusive;
        final LocalDate previousStart;
        try {
            currentEndExclusive = endDate.plusDays(1);
            long periodDays = ChronoUnit.DAYS.between(startDate, currentEndExclusive);
            previousStart = startDate.minusDays(periodDays);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "Date range is outside the supported reporting range.", exception);
        }
        Map<String, java.math.BigDecimal> current = databaseService.getExpenseTotalsBetween(
                accountEmail.trim(), startDate, currentEndExclusive);
        Map<String, java.math.BigDecimal> previous = databaseService.getExpenseTotalsBetween(
                accountEmail.trim(), previousStart, startDate);
        return new SpendingTrends(startDate, endDate, current, previous);
    }
}
