package DashboardCategories;

import java.math.BigDecimal;
import java.sql.SQLException;

public class DashboardController {
    private final DatabaseService databaseService;

    public DashboardController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void updateBalance(String accountEmail, BigDecimal balance)
            throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("Balance must be zero or greater.");
        }

        try {
            balance = balance.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Balance must have no more than two decimal places.", exception);
        }

        databaseService.saveBalance(accountEmail.trim(), balance);
    }
}
