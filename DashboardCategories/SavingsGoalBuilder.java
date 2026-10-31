package DashboardCategories;

import java.math.BigDecimal;
import java.util.UUID;

public class SavingsGoalBuilder {
    private String name;
    private BigDecimal targetAmount;
    private int timeframeMonths;
    private Account linkedAccount;

    public SavingsGoalBuilder setGoalDetails(
            String name, BigDecimal targetAmount, int timeframeMonths) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal name is required.");
        }
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new IllegalArgumentException("Target amount must be greater than zero.");
        }
        if (timeframeMonths <= 0) {
            throw new IllegalArgumentException("Timeframe must be greater than zero.");
        }
        final BigDecimal normalizedTarget;
        try {
            normalizedTarget = targetAmount.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Target amount must have no more than two decimal places.", exception);
        }
        this.name = name.trim();
        this.targetAmount = normalizedTarget;
        this.timeframeMonths = timeframeMonths;
        return this;
    }

    public SavingsGoalBuilder setLinkedAccount(Account linkedAccount) {
        if (linkedAccount == null) {
            throw new IllegalArgumentException("Linked account is required.");
        }
        this.linkedAccount = linkedAccount;
        return this;
    }

    public SavingsGoal build() {
        if (name == null || targetAmount == null || timeframeMonths <= 0 || linkedAccount == null) {
            throw new IllegalStateException("Goal details and a linked account are required.");
        }
        return new SavingsGoal(
                UUID.randomUUID().toString(),
                name,
                targetAmount,
                BigDecimal.ZERO.setScale(2),
                timeframeMonths,
                linkedAccount);
    }
}
