package DashboardCategories;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class SpendingTrendsGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final SpendingTrendsController controller;
    private final String accountEmail;
    private final JTextField startDateField =
            new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 12);
    private final JTextField endDateField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextArea reportArea = new JTextArea(18, 54);
    private final JLabel statusLabel = new JLabel(" ");

    public SpendingTrendsGUI(
            SpendingTrendsController controller, String accountEmail) throws SQLException {
        super("Spending Trends");
        if (controller == null) {
            throw new IllegalArgumentException("Spending trends controller is required.");
        }
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        this.controller = controller;
        this.accountEmail = accountEmail.trim();

        JPanel dateForm = new JPanel(new GridLayout(0, 2, 8, 8));
        dateForm.add(new JLabel("Start date (YYYY-MM-DD):"));
        dateForm.add(startDateField);
        dateForm.add(new JLabel("End date (YYYY-MM-DD):"));
        dateForm.add(endDateField);
        JButton viewButton = new JButton("View trends");
        viewButton.addActionListener(event -> refreshReport());

        reportArea.setEditable(false);
        reportArea.setFont(new java.awt.Font(
                java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(dateForm, BorderLayout.NORTH);
        content.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        content.add(viewButton, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);
        refreshReport();
    }

    private void refreshReport() {
        final LocalDate startDate;
        final LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateField.getText().trim());
            endDate = LocalDate.parse(endDateField.getText().trim());
        } catch (DateTimeParseException exception) {
            showError("Enter valid dates in YYYY-MM-DD format.");
            return;
        }

        try {
            SpendingTrends trends =
                    controller.getTrends(accountEmail, startDate, endDate);
            reportArea.setText(formatReport(trends));
            statusLabel.setText("Showing recorded expenses and previous-period comparison.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private String formatReport(SpendingTrends trends) {
        StringBuilder report = new StringBuilder()
                .append("Spending from ").append(trends.getStartDate())
                .append(" through ").append(trends.getEndDate()).append('\n')
                .append("Comparison period: the immediately preceding period of equal length.")
                .append("\n\n");
        Map<String, BigDecimal> current = trends.getCurrentTotals();
        Map<String, BigDecimal> previous = trends.getPreviousTotals();
        if (current.isEmpty()) {
            report.append("No expenses recorded in the selected period.\n");
        } else {
            Set<String> categories = new TreeSet<>();
            categories.addAll(current.keySet());
            categories.addAll(previous.keySet());
            BigDecimal currentTotal = BigDecimal.ZERO;
            BigDecimal previousTotal = BigDecimal.ZERO;
            report.append(String.format("%-22s %12s %12s %14s%n",
                    "Category", "Current", "Previous", "Change"));
            report.append("----------------------------------------------------------------\n");
            for (String category : categories) {
                BigDecimal currentAmount = current.containsKey(category)
                        ? current.get(category) : BigDecimal.ZERO;
                BigDecimal previousAmount = previous.containsKey(category)
                        ? previous.get(category) : BigDecimal.ZERO;
                currentTotal = currentTotal.add(currentAmount);
                previousTotal = previousTotal.add(previousAmount);
                report.append(String.format(
                        "%-22s $%11s $%11s %14s%n",
                        category,
                        currentAmount.setScale(2).toPlainString(),
                        previousAmount.setScale(2).toPlainString(),
                        formatChange(currentAmount, previousAmount)));
            }
            report.append("----------------------------------------------------------------\n");
            report.append(String.format(
                    "%-22s $%11s $%11s %14s%n",
                    "TOTAL",
                    currentTotal.setScale(2).toPlainString(),
                    previousTotal.setScale(2).toPlainString(),
                    formatChange(currentTotal, previousTotal)));
        }
        return report.toString();
    }

    private String formatChange(BigDecimal current, BigDecimal previous) {
        BigDecimal difference = current.subtract(previous);
        if (previous.signum() == 0) {
            return difference.signum() == 0
                    ? "$0.00 (no change)"
                    : "$" + difference.setScale(2).toPlainString() + " (no baseline)";
        }
        BigDecimal percent = difference.multiply(new BigDecimal("100"))
                .divide(previous, 1, RoundingMode.HALF_UP);
        return String.format(
                "%s$%s (%s%%)",
                difference.signum() > 0 ? "+" : "",
                difference.setScale(2).toPlainString(),
                percent.setScale(1, RoundingMode.HALF_UP).toPlainString());
    }

    private void showError(String message) {
        statusLabel.setText("Could not load spending trends.");
        JOptionPane.showMessageDialog(
                this, message, "Spending trends error", JOptionPane.ERROR_MESSAGE);
    }
}
