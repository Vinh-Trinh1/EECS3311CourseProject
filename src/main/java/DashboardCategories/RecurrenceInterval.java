package DashboardCategories;

public enum RecurrenceInterval {
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String value;

    RecurrenceInterval(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RecurrenceInterval fromValue(String value) {
        for (RecurrenceInterval interval : values()) {
            if (interval.value.equals(value)) {
                return interval;
            }
        }
        throw new IllegalArgumentException("Unknown recurrence interval: " + value);
    }
}
