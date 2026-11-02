package DashboardCategories;

import AIIntegration.AIService;
import java.io.IOException;
import java.util.Objects;

public class ExpenseCategorizerAdapter implements ExpenseCategorizerAI {
    private final AIService aiService;

    public ExpenseCategorizerAdapter(AIService aiService) {
        this.aiService = Objects.requireNonNull(aiService, "AI service is required.");
    }

    @Override
    public String categorizeExpense(Expense expense) throws IOException {
        Objects.requireNonNull(expense, "Expense is required.");
        String prompt = "Categorize expense: " + expense.getDescription()
                + ". Return one category name only from Groceries, Car payment, Bills, Housing, "
                + "Transportation, Healthcare, Entertainment, Other.";
        return aiService.generateResponse(prompt);
    }
}
