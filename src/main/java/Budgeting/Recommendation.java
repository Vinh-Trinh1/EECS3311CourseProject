package Budgeting;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Recommendation {
    private final Map<String, BigDecimal> categoryAmounts;
    private final String advice;

    public Recommendation(Map<String, BigDecimal> categoryAmounts, String advice) {
        if (categoryAmounts == null) {
            throw new IllegalArgumentException("Recommended budget amounts are required.");
        }
        if (advice == null || advice.trim().isEmpty()) {
            throw new IllegalArgumentException("Recommendation advice is required.");
        }
        this.categoryAmounts = Collections.unmodifiableMap(
                new LinkedHashMap<>(categoryAmounts));
        this.advice = advice.trim();
    }

    public Map<String, BigDecimal> getCategoryAmounts() {
        return categoryAmounts;
    }

    public String getAdvice() {
        return advice;
    }
}
