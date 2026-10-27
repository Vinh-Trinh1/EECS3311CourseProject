package DashboardCategories;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DatabaseServiceTest {
    private DatabaseService databaseService;
    private String accountEmail;

    @Before
    public void setUp() throws SQLException {
        databaseService = DatabaseService.getInstance();
        accountEmail = "test-" + UUID.randomUUID() + "@example.com";
    }

    @After
    public void tearDown() throws SQLException {
        try (PreparedStatement statement = databaseService.getConnection().prepareStatement(
                "DELETE FROM account_balances WHERE account_email = ?")) {
            statement.setString(1, accountEmail);
            statement.executeUpdate();
        } finally {
            databaseService.close();
        }
    }

    @Test
    public void saveBalanceStoresTheAmountInCents() throws SQLException {
        databaseService.saveBalance(accountEmail, new BigDecimal("125.34"));

        assertEquals(12534L, readBalanceCents());
    }

    @Test
    public void saveBalanceUpdatesAnExistingAccount() throws SQLException {
        databaseService.saveBalance(accountEmail, new BigDecimal("20.00"));
        databaseService.saveBalance(accountEmail, new BigDecimal("87.65"));

        assertEquals(8765L, readBalanceCents());
    }

    @Test
    public void saveBalanceRejectsAmountsWithFractionsOfACent() {
        assertThrows(
                ArithmeticException.class,
                () -> databaseService.saveBalance(accountEmail, new BigDecimal("1.001")));
    }

    @Test
    public void saveBalanceRejectsNegativeBalances() {
        assertThrows(
                SQLException.class,
                () -> databaseService.saveBalance(accountEmail, new BigDecimal("-0.01")));
    }

    private long readBalanceCents() throws SQLException {
        try (PreparedStatement statement = databaseService.getConnection().prepareStatement(
                "SELECT balance_cents FROM account_balances WHERE account_email = ?")) {
            statement.setString(1, accountEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("Expected a saved balance for " + accountEmail);
                }
                return resultSet.getLong("balance_cents");
            }
        }
    }
}
