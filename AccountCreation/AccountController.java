package AccountCreation;

import DashboardCategories.DatabaseService;
import DashboardCategories.NonMatchingPasswordException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public class AccountController {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+()0-9 .-]{7,20}$");

    private final DatabaseService databaseService;

    public AccountController(DatabaseService databaseService) {
        if (databaseService == null) {
            throw new IllegalArgumentException("Database service is required.");
        }
        this.databaseService = databaseService;
    }

    public void createAccount(User user) throws SQLException {
        validateAccount(user);

        try {
            user.passwordMatch(user.getPassword(), user.getConfirmationOfPassword());
        } catch (NonMatchingPasswordException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }

        char[] password = user.getPassword().toCharArray();
        try {
            databaseService.createUser(
                    user.getEmail().trim(),
                    user.getPhoneNumber().trim(),
                    PasswordHasher.hash(password),
                    user.getPreferredContactMethod().trim().toLowerCase(Locale.ROOT));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public boolean verifyCredentials(String identifier, char[] password) throws SQLException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Email or phone number is required.");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password is required.");
        }

        String passwordHash = databaseService.findUserPasswordHash(identifier.trim());
        return passwordHash != null && PasswordHasher.verify(password, passwordHash);
    }

    private void validateAccount(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Account details are required.");
        }
        if (user.getEmail() == null
                || !EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (user.getPhoneNumber() == null
                || !PHONE_PATTERN.matcher(user.getPhoneNumber().trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (user.getConfirmationOfPassword() == null) {
            throw new IllegalArgumentException("Confirm your password.");
        }
        String contactMethod = user.getPreferredContactMethod();
        if (contactMethod == null
                || !("email".equalsIgnoreCase(contactMethod.trim())
                        || "phone".equalsIgnoreCase(contactMethod.trim()))) {
            throw new IllegalArgumentException("Choose email or phone as your contact method.");
        }
    }
}
