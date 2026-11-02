package DashboardCategories;

import java.math.BigDecimal;
import java.util.Objects;

public class Category implements CategoryComponent {
    private final String name;
    private BigDecimal balance;

    public Category(String name, BigDecimal balance) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        this.name = name.trim();
        this.balance = Objects.requireNonNull(balance, "Category balance is required.");
    }

    public String getName() {
        return name;
    }

    @Override
    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public void updateBalance(BigDecimal amount) {
        balance = Objects.requireNonNull(amount, "Category balance is required.");
    }

    @Override
    public String toString() {
        return name + ": $" + balance.toPlainString();
    }
}
