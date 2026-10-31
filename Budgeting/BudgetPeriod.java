package Budgeting;

public enum BudgetPeriod {
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String value;

    BudgetPeriod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
