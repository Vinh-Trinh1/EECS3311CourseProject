package Budgeting;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

public class BudgetGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final BudgetController controller;
    private final String accountEmail;
    private final JComboBox<BudgetPeriod> periodField =
            new JComboBox<>(BudgetPeriod.values());
    private final JTextArea resultArea = new JTextArea(16, 36);
    private final JLabel statusLabel = new JLabel("OpenAI API");
    private final JButton requestButton = new JButton("Generate budget recommendation");
    private final JButton compareButton = new JButton("Compare budget vs actual");

    public BudgetGUI(BudgetController controller, String accountEmail) {
        super("Suggested Budget");
        if (controller == null || accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Controller and account email are required.");
        }
        this.controller = controller;
        this.accountEmail = accountEmail.trim();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        requestButton.addActionListener(event -> requestBudget());
        compareButton.addActionListener(event -> compareBudget());
        JPanel request = new JPanel(new GridLayout(0, 2, 8, 8));
        request.add(new JLabel("Budget period:"));
        request.add(periodField);
        request.add(requestButton);
        request.add(compareButton);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(request, BorderLayout.NORTH);
        content.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
    }

    private void requestBudget() {
        final BudgetPeriod period = (BudgetPeriod) periodField.getSelectedItem();
        requestButton.setEnabled(false);
        statusLabel.setText("Waiting for OpenAI...");
        new SwingWorker<Budget, Void>() {
            @Override
            protected Budget doInBackground() throws Exception {
                return controller.generateBudget(accountEmail, period);
            }

            @Override
            protected void done() {
                requestButton.setEnabled(true);
                try {
                    displayBudget(get());
                    statusLabel.setText("Budget recommendation saved.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The request was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showError(cause == null ? exception.getMessage() : cause.getMessage());
                }
            }
        }.execute();
    }

    private void showError(String message) {
        statusLabel.setText("Request failed.");
        JOptionPane.showMessageDialog(
                this, message, "Unable to generate budget", JOptionPane.ERROR_MESSAGE);
    }

    private void compareBudget() {
        final BudgetPeriod period = (BudgetPeriod) periodField.getSelectedItem();
        requestButton.setEnabled(false);
        compareButton.setEnabled(false);
        statusLabel.setText("Comparing saved budget with recorded expenses...");
        new SwingWorker<BudgetComparison, Void>() {
            @Override
            protected BudgetComparison doInBackground() throws Exception {
                return controller.compareBudgetToActual(accountEmail, period, LocalDate.now());
            }

            @Override
            protected void done() {
                requestButton.setEnabled(true);
                compareButton.setEnabled(true);
                try {
                    displayComparison(get());
                    statusLabel.setText("Budget comparison complete.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The comparison was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showError(cause == null ? exception.getMessage() : cause.getMessage());
                }
            }
        }.execute();
    }

    private void displayComparison(BudgetComparison comparison) {
        StringBuilder text = new StringBuilder("Budget vs. actual (")
                .append(comparison.getPeriod().getValue()).append("): ")
                .append(comparison.getStartDate()).append(" through ")
                .append(comparison.getEndDate()).append("\n\n");
        Set<String> categories = new LinkedHashSet<>(comparison.getBudgetAmounts().keySet());
        categories.addAll(comparison.getActualAmounts().keySet());
        if (categories.isEmpty()) {
            text.append("No budget categories or expenses are recorded for this period.\n");
        } else {
            text.append(String.format("%-18s %12s %12s %12s%n",
                    "Category", "Budget", "Actual", "Variance"));
            for (String category : categories) {
                BigDecimal planned = comparison.getBudgetAmount(category);
                BigDecimal actual = comparison.getActualAmount(category);
                BigDecimal variance = comparison.getVariance(category);
                text.append(String.format("%-18s $%11s $%11s %s$%10s%n",
                        category,
                        planned.toPlainString(),
                        actual.toPlainString(),
                        variance.signum() > 0 ? "+" : "",
                        variance.toPlainString()));
            }
            BigDecimal totalVariance = comparison.getTotalVariance();
            text.append(String.format("%-18s $%11s $%11s %s$%10s%n",
                    "TOTAL",
                    comparison.getBudgetTotal().toPlainString(),
                    comparison.getActualTotal().toPlainString(),
                    totalVariance.signum() > 0 ? "+" : "",
                    totalVariance.toPlainString()));
            text.append("\nPositive variance means spending is over budget; "
                    + "negative means under budget.");
        }
        resultArea.setText(text.toString());
    }

    private void displayBudget(Budget budget) {
        StringBuilder text = new StringBuilder()
                .append(budget.getPeriod().getValue()).append(" suggested limits\n");
        if (budget.getCategoryAmounts().isEmpty()) {
            text.append("No recent expenses are available to calculate category limits.\n");
        } else {
            for (Map.Entry<String, BigDecimal> entry : budget.getCategoryAmounts().entrySet()) {
                text.append(entry.getKey()).append(": $")
                        .append(entry.getValue().toPlainString()).append('\n');
            }
            text.append("Total: $").append(budget.getTotalAmount().toPlainString()).append('\n');
        }
        text.append("\n").append(budget.getRecommendation())
                .append("\n\nAI-generated information; not financial advice.");
        resultArea.setText(text.toString());
    }
}
