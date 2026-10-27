package DashboardCategories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS account_users ("
                            + "email TEXT PRIMARY KEY COLLATE NOCASE NOT NULL, "
                            + "phone_number TEXT UNIQUE NOT NULL, "
                            + "password_hash TEXT NOT NULL, "
                            + "preferred_contact_method TEXT NOT NULL "
                            + "CHECK (preferred_contact_method IN ('email', 'phone')))");
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

    public void createUser(
            String email, String phoneNumber, String passwordHash, String preferredContactMethod)
            throws SQLException {
        String sql = "INSERT INTO account_users "
                + "(email, phone_number, password_hash, preferred_contact_method) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, phoneNumber);
            statement.setString(3, passwordHash);
            statement.setString(4, preferredContactMethod);
            statement.executeUpdate();
        }
    }

    public String findUserPasswordHash(String identifier) throws SQLException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Email or phone number is required.");
        }

        String sql = identifier.contains("@")
                ? "SELECT password_hash FROM account_users WHERE email = ? COLLATE NOCASE"
                : "SELECT password_hash FROM account_users WHERE phone_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("password_hash") : null;
            }
        }
    }
}
