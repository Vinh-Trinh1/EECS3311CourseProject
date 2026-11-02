package DashboardCategories;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
    private final String accountId;
    private final String ownerUserId;
    private final String name;
    private BigDecimal balance;

    public Account(String accountId, String ownerUserId, String name, BigDecimal balance) {
        this.accountId = requireText(accountId, "Account ID");
        this.ownerUserId = requireText(ownerUserId, "Owner user ID");
        this.name = requireText(name, "Account name");
        this.balance = Objects.requireNonNull(balance, "Balance is required.");
    }

    
    public String getAccountId() {
        return accountId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = Objects.requireNonNull(balance, "Balance is required.");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
