package AccountCreation;

import DashboardCategories.DatabaseService;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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

    @Test
    public void loginCodeIsDeliveredToVerifiedAccountAndAcceptsPhoneIdentifier()
            throws Exception {
        controller.createAccount(createUser("correct horse"));
        AtomicReference<String> deliveredCode = new AtomicReference<>();
        TwoFactorService twoFactor = new TwoFactorAuthenticationProxy(
                new TwoFactorAuthentication(
                        (recipient, code) -> {
                            assertTrue(recipient.equalsIgnoreCase(email));
                            deliveredCode.set(code);
                        },
                        Clock.systemUTC(),
                        new SecureRandom()));
        AccountController twoFactorController =
                new AccountController(databaseService, twoFactor);

        assertTrue(email.equalsIgnoreCase(twoFactorController.requestLoginCode(
                phoneNumber, "correct horse".toCharArray())));
        assertTrue(deliveredCode.get().matches("\\d{6}"));
        assertTrue(twoFactorController.verifyLoginCode(phoneNumber, deliveredCode.get()));
        assertFalse(twoFactorController.verifyLoginCode(phoneNumber, deliveredCode.get()));
    }

    @Test
    public void verificationCodeExpiresAndProxyLocksAfterThreeFailures() throws Exception {
        MutableClock clock = new MutableClock();
        AtomicReference<String> deliveredCode = new AtomicReference<>();
        TwoFactorAuthentication authentication = new TwoFactorAuthentication(
                (recipient, code) -> deliveredCode.set(code), clock, new SecureRandom());
        authentication.requestCode(email);
        assertTrue(authentication.verifyCode(email, deliveredCode.get()));
        assertFalse(authentication.verifyCode(email, deliveredCode.get()));

        authentication.requestCode(email);
        clock.advanceMillis(6 * 60 * 1000L);
        assertFalse(authentication.verifyCode(email, deliveredCode.get()));

        TwoFactorService alwaysReject = new TwoFactorService() {
            @Override
            public void requestCode(String recipient) {
            }

            @Override
            public boolean verifyCode(String recipient, String code) {
                return false;
            }
        };
        TwoFactorAuthenticationProxy proxy = new TwoFactorAuthenticationProxy(alwaysReject, clock);
        assertFalse(proxy.verifyCode(email, "000000"));
        assertFalse(proxy.verifyCode(email, "000000"));
        try {
            proxy.verifyCode(email, "000000");
            fail("Expected the third invalid code to lock verification.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("15 minutes"));
        }
        try {
            proxy.requestCode(email);
            fail("Expected code requests to be blocked during lockout.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("locked"));
        }
    }

    @Test
    public void knockDeliveryRequiresEnvironmentConfiguration() throws Exception {
        try {
            new KnockEmailCodeDelivery("", "").deliver(email, "123456");
            fail("Expected missing Knock configuration to block delivery.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("KNOCK_API_KEY"));
        }
    }

    private User createUser(String password) {
        return new User(email, phoneNumber, password, password, "email");
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        private void advanceMillis(long milliseconds) {
            instant = instant.plusMillis(milliseconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
