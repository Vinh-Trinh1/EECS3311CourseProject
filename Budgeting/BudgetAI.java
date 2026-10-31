package Budgeting;

import AIIntegration.AIService;
import FinancialData.FinancialData;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class BudgetAI {
    private static final BigDecimal MONTHS_PER_WEEK = new BigDecimal("4.345");
    private final AIService aiService;

    public BudgetAI(AIService aiService) {
        this.aiService = Objects.requireNonNull(aiService, "AI service is required.");
    }

    public Recommendation analyzeFinances(FinancialData financialData, BudgetPeriod period)
            throws IOException {
        Objects.requireNonNull(financialData, "Financial data is required.");
        Objects.requireNonNull(period, "Budget period is required.");
        Map<String, BigDecimal> baseline = calculateBaseline(financialData, period);
        StringBuilder prompt = new StringBuilder("Budget recommendation for ")
                .append(period.getValue()).append(" spending. Recent average expenses: ");
        if (baseline.isEmpty()) {
            prompt.append("none recorded");
        } else {
            for (Map.Entry<String, BigDecimal> amount : baseline.entrySet()) {
                prompt.append(amount.getKey()).append(" $")
                        .append(amount.getValue().toPlainString()).append("; ");
            }
        }
        prompt.append("Recorded balance: $")
                .append(financialData.getAccountBalance().setScale(2).toPlainString())
                .append(". Provide cautious, concise guidance and do not assume income.");
        String advice = aiService.generateResponse(prompt.toString());
        return new Recommendation(baseline, advice);
    }

    public Map<String, BigDecimal> calculateBaseline(
            FinancialData financialData, BudgetPeriod period) {
        Objects.requireNonNull(financialData, "Financial data is required.");
        Objects.requireNonNull(period, "Budget period is required.");
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry
                : financialData.getExpensesByCategory().entrySet()) {
            BigDecimal monthlyAverage = entry.getValue();
            BigDecimal periodAverage = period == BudgetPeriod.MONTHLY
                    ? monthlyAverage
                    : monthlyAverage.divide(MONTHS_PER_WEEK, 2, RoundingMode.HALF_UP);
            amounts.put(
                    entry.getKey(),
                    periodAverage.multiply(new BigDecimal("0.95"))
                            .setScale(2, RoundingMode.HALF_UP));
        }
        return Collections.unmodifiableMap(amounts);
    }
}
