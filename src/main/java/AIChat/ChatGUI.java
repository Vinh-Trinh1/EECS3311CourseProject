package AIChat;

import java.awt.BorderLayout;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

public class ChatGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AIController controller;
    private final String accountEmail;
    private final JTextArea questionArea = new JTextArea(4, 36);
    private final JTextArea answerArea = new JTextArea(12, 36);
    private final JLabel statusLabel = new JLabel("OpenAI API");
    private final JButton submitButton = new JButton("Ask");

    public ChatGUI(AIController controller, String accountEmail) {
        super("AI Finance Chatbox");
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
        final String question = questionArea.getText().trim();
        submitButton.setEnabled(false);
        statusLabel.setText("Waiting for OpenAI...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return controller.askQuestion(accountEmail, question);
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                try {
                    String answer = get();
                    answerArea.append("You: " + question
                            + "\nAssistant: " + answer + "\n\n");
                    questionArea.setText("");
                    statusLabel.setText("OpenAI response");
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
                this, message, "Unable to answer question", JOptionPane.ERROR_MESSAGE);
    }
}
