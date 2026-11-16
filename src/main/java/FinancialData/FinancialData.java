package FinancialData;

import SavingsGoals.SavingsGoal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinancialData {
    private final BigDecimal accountBalance;
    private final Map<String, BigDecimal> expensesByCategory;
    private final List<SavingsGoal> savingsGoals;

    public FinancialData(
            BigDecimal accountBalance,
            Map<String, BigDecimal> expensesByCategory,
            List<SavingsGoal> savingsGoals) {
        if (accountBalance == null) {
            throw new IllegalArgumentException("Account balance is required.");
        }
        this.accountBalance = accountBalance;
        this.expensesByCategory = Collections.unmodifiableMap(
                new LinkedHashMap<>(expensesByCategory));
        this.savingsGoals = Collections.unmodifiableList(new java.util.ArrayList<>(savingsGoals));
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public Map<String, BigDecimal> getExpensesByCategory() {
        return expensesByCategory;
    }

    public List<SavingsGoal> getSavingsGoals() {
        return savingsGoals;
    }

    public String toPromptSummary() {
        StringBuilder summary = new StringBuilder()
                .append("Recorded account balance: $")
                .append(accountBalance.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .append(". Expenses for the most recent three completed months: ");
        if (expensesByCategory.isEmpty()) {
            summary.append("none recorded");
        } else {
            boolean first = true;
            for (Map.Entry<String, BigDecimal> expense : expensesByCategory.entrySet()) {
                if (!first) {
                    summary.append(", ");
                }
                summary.append(expense.getKey()).append(" $")
                        .append(expense.getValue().setScale(2).toPlainString());
                first = false;
            }
        }
        if (!savingsGoals.isEmpty()) {
            BigDecimal totalSaved = BigDecimal.ZERO;
            for (SavingsGoal goal : savingsGoals) {
                totalSaved = totalSaved.add(goal.getCurrentAmount());
            }
            summary.append(". Total saved across tracked savings goals: $")
                    .append(totalSaved.setScale(2).toPlainString())
                    .append(". Savings goals: ");
            for (int index = 0; index < savingsGoals.size(); index++) {
                if (index > 0) {
                    summary.append("; ");
                }
                SavingsGoal goal = savingsGoals.get(index);
                summary.append(goal.getName()).append(" saved $")
                        .append(goal.getCurrentAmount().setScale(2).toPlainString())
                        .append(" of $")
                        .append(goal.getTargetAmount().setScale(2).toPlainString())
                        .append(" (")
                        .append(goal.calculateProgress()).append("%)");
            }
        }
        return summary.toString();
    }
}
