package DashboardCategories;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SavingsGoalGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final SavingsController controller;
    private final String accountEmail;
    private final JTextField nameField = new JTextField(16);
    private final JTextField targetField = new JTextField(10);
    private final JTextField timeframeField = new JTextField(6);
    private final JTextField linkedAccountField = new JTextField("Primary", 12);
    private final JTextField contributionField = new JTextField(10);
    private final JComboBox<SavingsGoal> goalsField = new JComboBox<>();
    private final JLabel statusLabel = new JLabel(" ");

    public SavingsGoalGUI(SavingsController controller, String accountEmail)
            throws SQLException {
        super("Savings Goal Tracker");
        if (controller == null || accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Controller and account email are required.");
        }
        this.controller = controller;
        this.accountEmail = accountEmail.trim();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Goal name:"));
        form.add(nameField);
        form.add(new JLabel("Target amount:"));
        form.add(targetField);
        form.add(new JLabel("Timeframe (months):"));
        form.add(timeframeField);
        form.add(new JLabel("Linked account:"));
        form.add(linkedAccountField);
        JButton createButton = new JButton("Create goal");
        createButton.addActionListener(event -> createGoal());

        JPanel progress = new JPanel(new GridLayout(0, 2, 8, 8));
        progress.add(new JLabel("Savings goal:"));
        progress.add(goalsField);
        progress.add(new JLabel("Contribution:"));
        progress.add(contributionField);
        JButton contributeButton = new JButton("Add contribution");
        contributeButton.addActionListener(event -> addContribution());

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(form, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 1, 8, 8));
        actions.add(createButton);
        actions.add(progress);
        actions.add(contributeButton);
        content.add(actions, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(440, 300);
        setLocationRelativeTo(null);
        refreshGoals();
    }

    private void createGoal() {
        try {
            BigDecimal target = new BigDecimal(targetField.getText().trim());
            int months = Integer.parseInt(timeframeField.getText().trim());
            SavingsGoal goal = controller.createGoal(
                    accountEmail,
                    nameField.getText(),
                    target,
                    months,
                    linkedAccountField.getText());
            refreshGoals();
            selectGoal(goal.getId());
            statusLabel.setText("Savings goal created.");
        } catch (NumberFormatException exception) {
            showError("Enter a valid target amount and whole-number timeframe.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void addContribution() {
        SavingsGoal goal = (SavingsGoal) goalsField.getSelectedItem();
        if (goal == null) {
            showError("Create or select a savings goal first.");
            return;
        }
        try {
            SavingsGoal updated = controller.addContribution(
                    accountEmail, goal.getId(), new BigDecimal(contributionField.getText().trim()));
            refreshGoals();
            selectGoal(updated.getId());
            statusLabel.setText("Contribution saved. Goal progress: "
                    + updated.calculateProgress() + "%.");
        } catch (NumberFormatException exception) {
            showError("Enter a valid contribution amount.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void refreshGoals() throws SQLException {
        List<SavingsGoal> goals = controller.getGoals(accountEmail);
        goalsField.setModel(new DefaultComboBoxModel<>(goals.toArray(new SavingsGoal[0])));
    }

    private void selectGoal(String id) {
        for (int index = 0; index < goalsField.getItemCount(); index++) {
            if (goalsField.getItemAt(index).getId().equals(id)) {
                goalsField.setSelectedIndex(index);
                return;
            }
        }
    }

    private void showError(String message) {
        statusLabel.setText("Request failed.");
        JOptionPane.showMessageDialog(
                this, message, "Unable to complete request", JOptionPane.ERROR_MESSAGE);
    }
}
