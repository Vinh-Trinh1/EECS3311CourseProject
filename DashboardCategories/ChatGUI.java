package DashboardCategories;

import java.awt.BorderLayout;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ChatGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AIController controller;
    private final String accountEmail;
    private final JTextArea questionArea = new JTextArea(4, 36);
    private final JTextArea answerArea = new JTextArea(12, 36);
    private final JLabel statusLabel = new JLabel("Offline AI demo");

    public ChatGUI(AIController controller, String accountEmail) {
        super("AI Finance Chatbox (Offline Demo)");
        if (controller == null || accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Controller and account email are required.");
        }
        this.controller = controller;
        this.accountEmail = accountEmail.trim();
        answerArea.setEditable(false);
        answerArea.setLineWrap(true);
        answerArea.setWrapStyleWord(true);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        JButton submitButton = new JButton("Ask");
        submitButton.addActionListener(event -> askQuestion());

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(new JScrollPane(answerArea), BorderLayout.CENTER);
        JPanel questionPanel = new JPanel(new BorderLayout(8, 8));
        questionPanel.add(new JLabel("Question:"), BorderLayout.NORTH);
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.CENTER);
        questionPanel.add(submitButton, BorderLayout.EAST);
        content.add(questionPanel, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 440);
        setLocationRelativeTo(null);
    }

    private void askQuestion() {
        try {
            String answer = controller.askQuestion(accountEmail, questionArea.getText());
            answerArea.append("You: " + questionArea.getText().trim()
                    + "\nAssistant: " + answer + "\n\n");
            questionArea.setText("");
            statusLabel.setText("Offline AI demo response");
        } catch (IllegalArgumentException | IOException | SQLException exception) {
            statusLabel.setText("Request failed.");
            JOptionPane.showMessageDialog(
                    this, exception.getMessage(), "Unable to answer question",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
