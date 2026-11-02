package SavingsGoals;

import java.math.BigDecimal;

public class SavingsRecommendation {
    private final BigDecimal monthlyTarget;
    private final String advice;

    public SavingsRecommendation(BigDecimal monthlyTarget, String advice) {
        if (monthlyTarget == null || monthlyTarget.signum() < 0) {
            throw new IllegalArgumentException("Monthly savings target cannot be negative.");
        }
        try {
            this.monthlyTarget = monthlyTarget.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Monthly savings target must have no more than two decimal places.",
                    exception);
        }
        if (advice == null || advice.trim().isEmpty()) {
            throw new IllegalArgumentException("Savings advice is required.");
        }
        this.advice = advice.trim();
    }

    public BigDecimal getMonthlyTarget() {
        return monthlyTarget;
    }

    public String getAdvice() {
        return advice;
    }
}
