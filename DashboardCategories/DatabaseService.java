package DashboardCategories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS savings_goals ("
                            + "id TEXT PRIMARY KEY NOT NULL, "
                            + "account_email TEXT NOT NULL COLLATE NOCASE, "
                            + "name TEXT NOT NULL, "
                            + "target_cents INTEGER NOT NULL CHECK (target_cents > 0), "
                            + "current_cents INTEGER NOT NULL DEFAULT 0 "
                            + "CHECK (current_cents >= 0), "
                            + "timeframe_months INTEGER NOT NULL "
                            + "CHECK (timeframe_months > 0), "
                            + "linked_account_id TEXT NOT NULL, "
                            + "linked_account_name TEXT NOT NULL, "
                            + "linked_account_balance_cents INTEGER NOT NULL)");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS budgets ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "account_email TEXT NOT NULL COLLATE NOCASE, "
                            + "period TEXT NOT NULL CHECK (period IN ('weekly', 'monthly')), "
                            + "recommendation TEXT NOT NULL, "
                            + "generated_at TEXT NOT NULL, "
                            + "UNIQUE (account_email, period))");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS budget_lines ("
                            + "budget_id INTEGER NOT NULL REFERENCES budgets(id) ON DELETE CASCADE, "
                            + "category TEXT NOT NULL, "
                            + "amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0), "
                            + "PRIMARY KEY (budget_id, category))");
            statement.executeUpdate("PRAGMA foreign_keys = ON");
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
        return getExpenseTotalsBetween(
                accountEmail, month.atDay(1), month.plusMonths(1).atDay(1));
    }

    public Map<String, BigDecimal> getExpenseTotalsBetween(
            String accountEmail, LocalDate startInclusive, LocalDate endExclusive)
            throws SQLException {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        if (startInclusive == null || endExclusive == null
                || !startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Expense date range is invalid.");
        }
        String sql = "SELECT category, SUM(amount_cents) AS total_cents "
                + "FROM monthly_expenses "
                + "WHERE account_email = ? COLLATE NOCASE "
                + "AND expense_date >= ? AND expense_date < ? "
                + "GROUP BY category ORDER BY category COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail.trim());
            statement.setString(2, startInclusive.toString());
            statement.setString(3, endExclusive.toString());
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

    public BigDecimal getAccountBalance(String accountEmail) throws SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        String sql = "SELECT balance_cents FROM account_balances "
                + "WHERE account_email = ? COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountEmail.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? BigDecimal.valueOf(resultSet.getLong("balance_cents"), 2)
                        : BigDecimal.ZERO.setScale(2);
            }
        }
    }

    public void saveSavingsGoal(String accountEmail, SavingsGoal goal) throws SQLException {
        String sql = "INSERT INTO savings_goals "
                + "(id, account_email, name, target_cents, current_cents, timeframe_months, "
                + "linked_account_id, linked_account_name, linked_account_balance_cents) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, goal.getId());
            statement.setString(2, requireAccountEmail(accountEmail));
            statement.setString(3, goal.getName());
            statement.setLong(4, toCents(goal.getTargetAmount()));
            statement.setLong(5, toCents(goal.getCurrentAmount()));
            statement.setInt(6, goal.getTimeframeMonths());
            statement.setString(7, goal.getLinkedAccount().getAccountId());
            statement.setString(8, goal.getLinkedAccount().getName());
            statement.setLong(9, toCents(goal.getLinkedAccount().getBalance()));
            statement.executeUpdate();
        }
    }

    public List<SavingsGoal> getSavingsGoals(String accountEmail) throws SQLException {
        List<SavingsGoal> goals = new ArrayList<>();
        String sql = "SELECT id, name, target_cents, current_cents, timeframe_months, "
                + "linked_account_id, linked_account_name, linked_account_balance_cents "
                + "FROM savings_goals WHERE account_email = ? COLLATE NOCASE "
                + "ORDER BY name COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requireAccountEmail(accountEmail));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Account linkedAccount = new Account(
                            resultSet.getString("linked_account_id"),
                            accountEmail.trim(),
                            resultSet.getString("linked_account_name"),
                            BigDecimal.valueOf(
                                    resultSet.getLong("linked_account_balance_cents"), 2));
                    goals.add(new SavingsGoal(
                            resultSet.getString("id"),
                            resultSet.getString("name"),
                            BigDecimal.valueOf(resultSet.getLong("target_cents"), 2),
                            BigDecimal.valueOf(resultSet.getLong("current_cents"), 2),
                            resultSet.getInt("timeframe_months"),
                            linkedAccount));
                }
            }
        }
        return goals;
    }

    public void updateSavingsGoalProgress(
            String accountEmail, String goalId, BigDecimal currentAmount) throws SQLException {
        String sql = "UPDATE savings_goals SET current_cents = ? "
                + "WHERE account_email = ? COLLATE NOCASE AND id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, toCents(currentAmount));
            statement.setString(2, requireAccountEmail(accountEmail));
            statement.setString(3, goalId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Savings goal was not found.");
            }
        }
    }

    public void saveBudget(Budget budget) throws SQLException {
        String upsert = "INSERT INTO budgets "
                + "(account_email, period, recommendation, generated_at) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(account_email, period) DO UPDATE SET "
                + "recommendation = excluded.recommendation, "
                + "generated_at = excluded.generated_at";
        String findId = "SELECT id FROM budgets WHERE account_email = ? COLLATE NOCASE "
                + "AND period = ?";
        String clearLines = "DELETE FROM budget_lines WHERE budget_id = ?";
        String insertLine = "INSERT INTO budget_lines (budget_id, category, amount_cents) "
                + "VALUES (?, ?, ?)";
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement statement = connection.prepareStatement(upsert)) {
                statement.setString(1, budget.getAccountEmail());
                statement.setString(2, budget.getPeriod().getValue());
                statement.setString(3, budget.getRecommendation());
                statement.setString(4, budget.getGeneratedAt().toString());
                statement.executeUpdate();
            }
            long budgetId;
            try (PreparedStatement statement = connection.prepareStatement(findId)) {
                statement.setString(1, budget.getAccountEmail());
                statement.setString(2, budget.getPeriod().getValue());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("The saved budget could not be retrieved.");
                    }
                    budgetId = resultSet.getLong("id");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(clearLines)) {
                statement.setLong(1, budgetId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(insertLine)) {
                for (Map.Entry<String, BigDecimal> entry : budget.getCategoryAmounts().entrySet()) {
                    statement.setLong(1, budgetId);
                    statement.setString(2, entry.getKey());
                    statement.setLong(3, toCents(entry.getValue()));
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public Budget getBudget(String accountEmail, BudgetPeriod period) throws SQLException {
        String sql = "SELECT id, recommendation, generated_at FROM budgets "
                + "WHERE account_email = ? COLLATE NOCASE AND period = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requireAccountEmail(accountEmail));
            statement.setString(2, period.getValue());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                long budgetId = resultSet.getLong("id");
                Map<String, BigDecimal> amounts = new LinkedHashMap<>();
                try (PreparedStatement lines = connection.prepareStatement(
                        "SELECT category, amount_cents FROM budget_lines "
                                + "WHERE budget_id = ? ORDER BY category COLLATE NOCASE")) {
                    lines.setLong(1, budgetId);
                    try (ResultSet lineResults = lines.executeQuery()) {
                        while (lineResults.next()) {
                            amounts.put(
                                    lineResults.getString("category"),
                                    BigDecimal.valueOf(lineResults.getLong("amount_cents"), 2));
                        }
                    }
                }
                return new Budget(
                        accountEmail.trim(),
                        period,
                        amounts,
                        resultSet.getString("recommendation"),
                        java.time.LocalDateTime.parse(resultSet.getString("generated_at")));
            }
        }
    }

    private static String requireAccountEmail(String accountEmail) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return accountEmail.trim();
    }

    private static long toCents(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be zero or greater.");
        }
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY)
                .movePointRight(2).longValueExact();
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
