package SavingsGoals;

import AIChat.AgentMemory;
import AIChat.FinanceAgent;
import AIIntegration.AIService;
import FinancialData.FinancialData;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

public class SavingsAI extends FinanceAgent {
    public SavingsAI(AIService aiService) {
        this(aiService, new AgentMemory());
    }

    public SavingsAI(AIService aiService, AgentMemory memory) {
        super(aiService, memory);
    }

    public SavingsRecommendation analyzeSavingsGoal(
            SavingsGoal goal, FinancialData financialData) throws IOException {
        Objects.requireNonNull(goal, "Savings goal is required.");
        Objects.requireNonNull(financialData, "Financial data is required.");
        BigDecimal monthlyTarget = calculateMonthlyTarget(goal);

        StringBuilder context = new StringBuilder()
                .append("Goal: ").append(goal.getName())
                .append("; remaining amount: $")
                .append(goal.getTargetAmount().subtract(goal.getCurrentAmount())
                        .max(BigDecimal.ZERO).setScale(2).toPlainString())
                .append("; timeframe: ").append(goal.getTimeframeMonths())
                .append(" months; calculated monthly target: $")
                .append(monthlyTarget.toPlainString())
                .append("; recorded account balance: $")
                .append(financialData.getAccountBalance().setScale(2, RoundingMode.HALF_UP)
                        .toPlainString())
                .append("; historical average monthly expenses by category: ");
        if (financialData.getExpensesByCategory().isEmpty()) {
            context.append("none recorded");
        } else {
            for (Map.Entry<String, BigDecimal> entry
                    : financialData.getExpensesByCategory().entrySet()) {
                context.append(entry.getKey()).append(" $")
                        .append(entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString())
                        .append("; ");
            }
        }

        String advice = run(
                "Explain a cautious plan for this savings goal. Use the calculated monthly "
                        + "target and recorded historical data only. Do not assume income or "
                        + "that the recorded balance is disposable.",
                context.toString());
        return new SavingsRecommendation(monthlyTarget, advice);
    }

    public BigDecimal calculateMonthlyTarget(SavingsGoal goal) {
        Objects.requireNonNull(goal, "Savings goal is required.");
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount())
                .max(BigDecimal.ZERO);
        return remaining.divide(
                BigDecimal.valueOf(goal.getTimeframeMonths()), 2, RoundingMode.CEILING);
    }
}
