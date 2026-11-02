package AccountCreation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PasswordHasherTest {

    @Test
    public void hashCanBeVerifiedWithTheOriginalPassword() {
        char[] password = "correct horse battery staple".toCharArray();

        String storedHash = PasswordHasher.hash(password);

        assertTrue(PasswordHasher.verify(password, storedHash));
        assertFalse(PasswordHasher.verify("wrong password".toCharArray(), storedHash));
    }

    @Test
    public void hashingTheSamePasswordUsesDifferentSalts() {
        char[] password = "same password".toCharArray();

        String firstHash = PasswordHasher.hash(password);
        String secondHash = PasswordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.verify(password, firstHash));
        assertTrue(PasswordHasher.verify(password, secondHash));
    }

    @Test
    public void verifyRejectsMalformedStoredHash() {
        try {
            PasswordHasher.verify("password".toCharArray(), "not-a-valid-hash");
            fail("Expected an IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Invalid stored password hash"));
        }
    }
}
