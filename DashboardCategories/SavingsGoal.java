package DashboardCategories;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class SavingsGoal {
    private final String id;
    private final String name;
    private final BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private final int timeframeMonths;
    private final Account linkedAccount;

    SavingsGoal(
            String id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            int timeframeMonths,
            Account linkedAccount) {
        this.id = requireText(id, "Goal ID");
        this.name = requireText(name, "Goal name");
        this.targetAmount = requirePositiveAmount(targetAmount, "Target amount");
        this.currentAmount = requireNonNegativeAmount(currentAmount, "Current amount");
        if (timeframeMonths <= 0) {
            throw new IllegalArgumentException("Timeframe must be greater than zero.");
        }
        this.timeframeMonths = timeframeMonths;
        this.linkedAccount = Objects.requireNonNull(linkedAccount, "Linked account is required.");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public int getTimeframeMonths() {
        return timeframeMonths;
    }

    public Account getLinkedAccount() {
        return linkedAccount;
    }

    public BigDecimal calculateProgress() {
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    public void addContribution(BigDecimal amount) {
        requirePositiveAmount(amount, "Contribution");
        currentAmount = currentAmount.add(amount).setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public String toString() {
        return name + " ($" + currentAmount.toPlainString() + " / $"
                + targetAmount.toPlainString() + ", " + calculateProgress() + "%)";
    }

    private static BigDecimal requirePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    fieldName + " must have no more than two decimal places.", exception);
        }
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or greater.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    fieldName + " must have no more than two decimal places.", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
