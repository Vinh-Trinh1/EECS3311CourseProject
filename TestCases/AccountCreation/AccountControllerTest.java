package AccountCreation;

import DashboardCategories.DatabaseService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AccountControllerTest {
    private DatabaseService databaseService;
    private AccountController controller;
    private String email;
    private String phoneNumber;

    @Before
    public void setUp() throws SQLException {
        databaseService = DatabaseService.getInstance();
        controller = new AccountController(databaseService);
        String unique = UUID.randomUUID().toString();
        email = "test-" + unique + "@example.com";
        phoneNumber = "+1" + Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits())
                .substring(0, 10);
    }

    @After
    public void tearDown() throws SQLException {
        if (databaseService == null) {
            return;
        }
        try (PreparedStatement statement = databaseService.getConnection().prepareStatement(
                "DELETE FROM account_users WHERE email = ?")) {
            statement.setString(1, email);
            statement.executeUpdate();
        } finally {
            databaseService.close();
        }
    }

    @Test
    public void createsAccountWithAHashAndVerifiesCredentials() throws SQLException {
        controller.createAccount(createUser("correct horse"));

        String storedHash = databaseService.findUserPasswordHash(email);
        assertTrue(storedHash.startsWith("pbkdf2-sha256$"));
        assertFalse(storedHash.equals("correct horse"));
        assertTrue(controller.verifyCredentials(email, "correct horse".toCharArray()));
        assertTrue(controller.verifyCredentials(phoneNumber, "correct horse".toCharArray()));
        assertFalse(controller.verifyCredentials(email, "wrong password".toCharArray()));
        assertFalse(controller.verifyCredentials("missing@example.com", "password".toCharArray()));
    }

    @Test
    public void rejectsMismatchedPasswords() throws SQLException {
        try {
            controller.createAccount(new User(
                    email, phoneNumber, "first password", "different password", "email"));
            fail("Expected account creation to reject mismatched passwords.");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("Passwords do not match"));
        }
    }

    private User createUser(String password) {
        return new User(email, phoneNumber, password, password, "email");
    }
}
