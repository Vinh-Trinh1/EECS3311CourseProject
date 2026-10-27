package AccountCreation;

import DashboardCategories.NonMatchingPasswordException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UserTest {

    @Test
    public void passwordMatchReturnsTrueForMatchingPasswords() throws NonMatchingPasswordException {
        User user = createUser();

        assertTrue(user.passwordMatch("correct horse", "correct horse"));
    }

    @Test
    public void passwordMatchThrowsForDifferentPasswords() {
        User user = createUser();

        try {
            user.passwordMatch("first password", "different password");
            fail("Expected a NonMatchingPasswordException.");
        } catch (NonMatchingPasswordException exception) {
            assertEquals(
                    "Passwords do not match. Please make sure that your passwords match.",
                    exception.getMessage());
        }
    }

    @Test
    public void gettersReturnAccountDetailsAndPreferredContactCanBeUpdated() {
        User user = createUser();

        assertEquals("sam@example.com", user.getEmail());
        assertEquals("555-0100", user.getPhoneNumber());
        assertEquals("initial password", user.getPassword());
        assertEquals("email", user.getPreferredContactMethod());

        user.setPreferredContactMethod("phone");

        assertEquals("phone", user.getPreferredContactMethod());
    }

    private User createUser() {
        return new User(
                "sam@example.com",
                "555-0100",
                "initial password",
                "initial password",
                "email");
    }
}
