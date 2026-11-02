package DashboardCategories;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public final class RecurringExpensesGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String[] PREDEFINED_CATEGORIES = {
        "Groceries", "Car payment", "Bills", "Housing", "Transportation",
        "Healthcare", "Entertainment", "Other"
    };

    private final RecurringExpenseController controller;
    private final String accountEmail;
    private final JTextField amountField = new JTextField(12);
    private final JTextField descriptionField = new JTextField(18);
    private final JTextField nextDueField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField endDateField = new JTextField(12);
    private final JComboBox<String> categoryField = new JComboBox<>(PREDEFINED_CATEGORIES);
    private final JComboBox<RecurrenceInterval> intervalField =
            new JComboBox<>(RecurrenceInterval.values());
    private final DefaultListModel<RecurringExpense> recurrenceModel = new DefaultListModel<>();
    private final JList<RecurringExpense> recurrenceList = new JList<>(recurrenceModel);
    private final JLabel statusLabel = new JLabel(" ");

    public RecurringExpensesGUI(
            RecurringExpenseController controller, String accountEmail) throws SQLException {
        super("Recurring Expenses");
        if (controller == null) {
            throw new IllegalArgumentException("Recurring expense controller is required.");
        }
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        this.controller = controller;
        this.accountEmail = accountEmail.trim();
        addUserCategories();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Amount:"));
        form.add(amountField);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Category:"));
        form.add(categoryField);
        form.add(new JLabel("Repeats:"));
        form.add(intervalField);
        form.add(new JLabel("Next due date (YYYY-MM-DD):"));
        form.add(nextDueField);
        form.add(new JLabel("End date (optional):"));
        form.add(endDateField);

        JButton addButton = new JButton("Add recurring expense");
        addButton.addActionListener(event -> addRecurrence());
        JButton toggleButton = new JButton("Pause / resume selected");
        toggleButton.addActionListener(event -> toggleSelected());
        JButton updateButton = new JButton("Update selected");
        updateButton.addActionListener(event -> updateSelected());
        JButton deleteButton = new JButton("Delete selected");
        deleteButton.addActionListener(event -> deleteSelected());

        recurrenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recurrenceList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedForEditing();
            }
        });
        recurrenceList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof RecurringExpense) {
                    RecurringExpense recurrence = (RecurringExpense) value;
                    setText(String.format(
                            "%s | $%s | %s %s | next: %s | end: %s | %s",
                            recurrence.getDescription(),
                            recurrence.getAmount().setScale(2).toPlainString(),
                            recurrence.getCategory(),
                            recurrence.getInterval().getValue(),
                            recurrence.getNextDueDate(),
                            recurrence.getEndDate() == null
                                    ? "none" : recurrence.getEndDate(),
                            recurrence.isActive() ? "active" : "paused"));
                }
                return this;
            }
        });

        JPanel actions = new JPanel(new GridLayout(1, 0, 8, 8));
        actions.add(addButton);
        actions.add(updateButton);
        actions.add(toggleButton);
        actions.add(deleteButton);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(form, BorderLayout.NORTH);
        content.add(new JScrollPane(recurrenceList), BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(920, 450);
        setLocationRelativeTo(null);

        int created = controller.processDueExpenses(this.accountEmail, LocalDate.now());
        refreshRecurrences();
        if (created > 0) {
            statusLabel.setText(created + " due expense(s) recorded.");
        }
    }

    private void addUserCategories() throws SQLException {
        for (Category category : controller.getCategories(accountEmail)) {
            boolean present = false;
            for (int index = 0; index < categoryField.getItemCount(); index++) {
                if (categoryField.getItemAt(index).equalsIgnoreCase(category.getName())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                categoryField.addItem(category.getName());
            }
        }
    }

    private void addRecurrence() {
        final BigDecimal amount;
        final LocalDate nextDueDate;
        final LocalDate endDate;
        try {
            amount = new BigDecimal(amountField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Enter a valid amount, for example 45.00.");
            amountField.requestFocusInWindow();
            return;
        }
        try {
            nextDueDate = LocalDate.parse(nextDueField.getText().trim());
            String endDateText = endDateField.getText().trim();
            endDate = endDateText.isEmpty() ? null : LocalDate.parse(endDateText);
        } catch (DateTimeParseException exception) {
            showError("Enter valid dates in YYYY-MM-DD format.");
            nextDueField.requestFocusInWindow();
            return;
        }
        String category = (String) categoryField.getSelectedItem();
        try {
            controller.create(
                    accountEmail, amount, descriptionField.getText(), category,
                    (RecurrenceInterval) intervalField.getSelectedItem(),
                    nextDueDate, endDate);
            amountField.setText("");
            descriptionField.setText("");
            endDateField.setText("");
            statusLabel.setText("Recurring expense added.");
            refreshRecurrences();
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void toggleSelected() {
        RecurringExpense selected = recurrenceList.getSelectedValue();
        if (selected == null) {
            showError("Select a recurring expense first.");
            return;
        }
        try {
            controller.setActive(accountEmail, selected.getId(), !selected.isActive());
            refreshRecurrences();
            statusLabel.setText(selected.isActive()
                    ? "Recurring expense paused."
                    : "Recurring expense resumed.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void updateSelected() {
        RecurringExpense selected = recurrenceList.getSelectedValue();
        if (selected == null) {
            showError("Select a recurring expense first.");
            return;
        }
        final BigDecimal amount;
        final LocalDate nextDueDate;
        final LocalDate endDate;
        try {
            amount = new BigDecimal(amountField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Enter a valid amount, for example 45.00.");
            amountField.requestFocusInWindow();
            return;
        }
        try {
            nextDueDate = LocalDate.parse(nextDueField.getText().trim());
            String endDateText = endDateField.getText().trim();
            endDate = endDateText.isEmpty() ? null : LocalDate.parse(endDateText);
        } catch (DateTimeParseException exception) {
            showError("Enter valid dates in YYYY-MM-DD format.");
            nextDueField.requestFocusInWindow();
            return;
        }
        try {
            controller.update(
                    accountEmail, selected.getId(), amount, descriptionField.getText(),
                    (String) categoryField.getSelectedItem(),
                    (RecurrenceInterval) intervalField.getSelectedItem(),
                    nextDueDate, endDate);
            refreshRecurrences();
            statusLabel.setText("Recurring expense updated.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void loadSelectedForEditing() {
        RecurringExpense selected = recurrenceList.getSelectedValue();
        if (selected == null) {
            return;
        }
        amountField.setText(selected.getAmount().toPlainString());
        descriptionField.setText(selected.getDescription());
        categoryField.setSelectedItem(selected.getCategory());
        intervalField.setSelectedItem(selected.getInterval());
        nextDueField.setText(selected.getNextDueDate().toString());
        endDateField.setText(
                selected.getEndDate() == null ? "" : selected.getEndDate().toString());
    }

    private void deleteSelected() {
        RecurringExpense selected = recurrenceList.getSelectedValue();
        if (selected == null) {
            showError("Select a recurring expense first.");
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(
                this, "Delete \"" + selected.getDescription() + "\"?",
                "Delete recurring expense", JOptionPane.YES_NO_OPTION);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.delete(accountEmail, selected.getId());
            refreshRecurrences();
            statusLabel.setText("Recurring expense deleted.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void refreshRecurrences() throws SQLException {
        List<RecurringExpense> recurrences = controller.getRecurringExpenses(accountEmail);
        recurrenceModel.clear();
        for (RecurringExpense recurrence : recurrences) {
            recurrenceModel.addElement(recurrence);
        }
    }

    private void showError(String message) {
        statusLabel.setText("Unable to update recurring expenses.");
        JOptionPane.showMessageDialog(
                this, message, "Recurring expense error", JOptionPane.ERROR_MESSAGE);
    }
}
