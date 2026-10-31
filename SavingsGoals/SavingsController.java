package SavingsGoals;

import DashboardCategories.Account;
import DashboardCategories.DatabaseService;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class SavingsController {
    private final DatabaseService databaseService;

    public SavingsController(DatabaseService databaseService) {
        this.databaseService = Objects.requireNonNull(
                databaseService, "Database service is required.");
    }

    public SavingsGoal createGoal(
            String accountEmail,
            String goalName,
            BigDecimal targetAmount,
            int timeframeMonths,
            String linkedAccountName) throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        if (linkedAccountName == null || linkedAccountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Linked account name is required.");
        }
        Account account = new Account(
                owner,
                owner,
                linkedAccountName,
                databaseService.getAccountBalance(owner));
        SavingsGoal goal = new SavingsGoalBuilder()
                .setGoalDetails(goalName, targetAmount, timeframeMonths)
                .setLinkedAccount(account)
                .build();
        databaseService.saveSavingsGoal(owner, goal);
        return goal;
    }

    public List<SavingsGoal> getGoals(String accountEmail) throws SQLException {
        return databaseService.getSavingsGoals(requireAccountEmail(accountEmail));
    }

    public SavingsGoal addContribution(
            String accountEmail, String goalId, BigDecimal contribution) throws SQLException {
        String owner = requireAccountEmail(accountEmail);
        if (goalId == null || goalId.trim().isEmpty()) {
            throw new IllegalArgumentException("Select a savings goal.");
        }
        if (contribution == null || contribution.signum() <= 0) {
            throw new IllegalArgumentException("Contribution must be greater than zero.");
        }
        try {
            contribution = contribution.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Contribution must have no more than two decimal places.", exception);
        }

        SavingsGoal goal = null;
        for (SavingsGoal candidate : databaseService.getSavingsGoals(owner)) {
            if (candidate.getId().equals(goalId)) {
                goal = candidate;
                break;
            }
        }
        if (goal == null) {
            throw new IllegalArgumentException("Savings goal was not found.");
        }
        goal.addContribution(contribution);
        databaseService.updateSavingsGoalProgress(owner, goal.getId(), goal.getCurrentAmount());
        return goal;
    }

    private static String requireAccountEmail(String accountEmail) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        return accountEmail.trim();
    }
}
