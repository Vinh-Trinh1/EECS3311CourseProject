package DashboardCategories;

import java.io.IOException;

@FunctionalInterface
public interface ExpenseCategorizerAI {
    String categorizeExpense(Expense expense) throws IOException;
}
