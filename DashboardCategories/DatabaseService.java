package DashboardCategories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS dashboard_categories ("
                            + "account_email TEXT NOT NULL COLLATE NOCASE, "
                            + "name TEXT NOT NULL COLLATE NOCASE, "
                            + "balance_cents INTEGER NOT NULL, "
                            + "PRIMARY KEY (account_email, name))");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS monthly_expenses ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "account_email TEXT NOT NULL COLLATE NOCASE, "
                            + "amount_cents INTEGER NOT NULL CHECK (amount_cents > 0), "
                            + "expense_date TEXT NOT NULL, "
                            + "description TEXT NOT NULL, "
                            + "category TEXT NOT NULL)");
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

    public void saveCategory(String accountEmail, Category category) throws SQLException {
        long balanceCents = category.getBalance().movePointRight(2).longValueExact();
        String sql = "INSERT INTO dashboard_categories "
                + "(account_email, name, balance_cents) VALUES (?, ?, ?) "
                + "ON CONFLICT(account_email, name) DO UPDATE SET "
                + "balance_cents = excluded.balance_cents";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail.trim());
            statement.setString(2, category.getName());
            statement.setLong(3, balanceCents);
            statement.executeUpdate();
        }
    }

    public List<Category> getCategories(String accountEmail) throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT name, balance_cents FROM dashboard_categories "
                + "WHERE account_email = ? COLLATE NOCASE ORDER BY name COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(new Category(
                            resultSet.getString("name"),
                            BigDecimal.valueOf(resultSet.getLong("balance_cents"), 2)));
                }
            }
        }
        return categories;
    }

    public void saveExpense(String accountEmail, Expense expense) throws SQLException {
        if (expense.getCategory() == null) {
            throw new IllegalArgumentException("Assign a category before saving the expense.");
        }
        long amountCents = expense.getAmount().movePointRight(2).longValueExact();
        String sql = "INSERT INTO monthly_expenses "
                + "(account_email, amount_cents, expense_date, description, category) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail.trim());
            statement.setLong(2, amountCents);
            statement.setString(3, expense.getDate().toString());
            statement.setString(4, expense.getDescription());
            statement.setString(5, expense.getCategory());
            statement.executeUpdate();
        }
    }

    public Map<String, BigDecimal> getMonthlyExpenseTotals(
            String accountEmail, YearMonth month) throws SQLException {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        String sql = "SELECT category, SUM(amount_cents) AS total_cents "
                + "FROM monthly_expenses "
                + "WHERE account_email = ? COLLATE NOCASE "
                + "AND expense_date >= ? AND expense_date < ? "
                + "GROUP BY category ORDER BY category COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail.trim());
            statement.setString(2, month.atDay(1).toString());
            statement.setString(3, month.plusMonths(1).atDay(1).toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    totals.put(
                            resultSet.getString("category"),
                            BigDecimal.valueOf(resultSet.getLong("total_cents"), 2));
                }
            }
        }
        return totals;
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
