package DashboardCategories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private static final String DATABASE_URL = "jdbc:sqlite:finance.db";
    private static DatabaseService instance;

    private final Connection connection;

    private DatabaseService() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS account_balances ("
                            + "account_email TEXT PRIMARY KEY NOT NULL, "
                            + "balance_cents INTEGER NOT NULL "
                            + "CHECK (balance_cents >= 0))");
        }
    }

    public static synchronized DatabaseService getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        synchronized (DatabaseService.class) {
            connection.close();
            if (instance == this) {
                instance = null;
            }
        }
    }

    public void saveBalance(String accountEmail, BigDecimal balance)
            throws SQLException {
        long balanceCents = balance.movePointRight(2).longValueExact();
        String sql = "INSERT INTO account_balances (account_email, balance_cents) "
                + "VALUES (?, ?) "
                + "ON CONFLICT(account_email) DO UPDATE SET "
                + "balance_cents = excluded.balance_cents";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail);
            statement.setLong(2, balanceCents);
            statement.executeUpdate();
        }
    }
}
