
package AccountCreation;

import DashboardCategories.DatabaseService;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class LoginGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AccountController controller;
    private final JTextField loginIdentifierField = new JTextField(22);
    private final JPasswordField loginPasswordField = new JPasswordField(22);
    private final JTextField emailField = new JTextField(22);
    private final JTextField phoneField = new JTextField(22);
    private final JPasswordField createPasswordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);
    private final JComboBox<String> contactMethodField =
            new JComboBox<>(new String[] {"email", "phone"});

    public LoginGUI(AccountController controller) {
        super("Account Login");
        if (controller == null) {
            throw new IllegalArgumentException("Account controller is required.");
        }
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log in", createLoginPanel());
        tabs.addTab("Create account", createAccountPanel());
        add(tabs);
        setSize(380, 250);
        setLocationRelativeTo(null);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Email or phone:"));
        panel.add(loginIdentifierField);
        panel.add(new JLabel("Password:"));
        panel.add(loginPasswordField);

        JButton loginButton = new JButton("Log in");
        loginButton.addActionListener(event -> logIn());
        panel.add(new JLabel());
        panel.add(loginButton);
        return panel;
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);
        panel.add(new JLabel("Password:"));
        panel.add(createPasswordField);
        panel.add(new JLabel("Confirm password:"));
        panel.add(confirmPasswordField);
        panel.add(new JLabel("Preferred contact:"));
        panel.add(contactMethodField);

        JButton createButton = new JButton("Create account");
        createButton.addActionListener(event -> createAccount());
        panel.add(new JLabel());
        panel.add(createButton);
        return panel;
    }

    private void logIn() {
        char[] password = loginPasswordField.getPassword();
        try {
            if (!controller.verifyCredentials(loginIdentifierField.getText(), password)) {
                showError("Email, phone number, or password is incorrect.");
                return;
            }
            JOptionPane.showMessageDialog(
                    this,
                    "Password verified. Two-factor authentication is not configured, "
                            + "so access cannot continue.",
                    "Two-factor authentication unavailable",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        } finally {
            java.util.Arrays.fill(password, '\0');
            loginPasswordField.setText("");
        }
    }

    private void createAccount() {
        char[] password = createPasswordField.getPassword();
        char[] confirmation = confirmPasswordField.getPassword();
        try {
            controller.createAccount(new User(
                    emailField.getText(),
                    phoneField.getText(),
                    new String(password),
                    new String(confirmation),
                    (String) contactMethodField.getSelectedItem()));
            JOptionPane.showMessageDialog(
                    this, "Account created.", "Account created",
                    JOptionPane.INFORMATION_MESSAGE);
            createPasswordField.setText("");
            confirmPasswordField.setText("");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 19) {
                showError("That email address or phone number is already registered.");
            } else {
                showError("Could not create account: " + exception.getMessage());
            }
        } finally {
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Account error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AccountController controller =
                        new AccountController(DatabaseService.getInstance());
                new LoginGUI(controller).setVisible(true);
            } catch (SQLException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not connect to the SQLite database: " + exception.getMessage(),
                        "Database error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
