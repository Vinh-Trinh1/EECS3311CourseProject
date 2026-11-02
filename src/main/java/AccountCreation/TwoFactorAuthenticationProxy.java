package AccountCreation;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TwoFactorAuthenticationProxy implements TwoFactorService {
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_MILLIS = 15 * 60 * 1000L;

    private final TwoFactorService authentication;
    private final Clock clock;
    private final Map<String, AttemptState> attempts = new HashMap<>();

    public TwoFactorAuthenticationProxy(TwoFactorService authentication) {
        this(authentication, Clock.systemUTC());
    }

    TwoFactorAuthenticationProxy(TwoFactorService authentication, Clock clock) {
        this.authentication = Objects.requireNonNull(
                authentication, "Two-factor authentication is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    @Override
    public synchronized void requestCode(String email) throws IOException {
        String recipient = normalizeEmail(email);
        checkLockout(recipient);
        authentication.requestCode(recipient);
    }

    @Override
    public synchronized boolean verifyCode(String email, String code) {
        String recipient = normalizeEmail(email);
        checkLockout(recipient);
        AttemptState state = attempts.computeIfAbsent(recipient, ignored -> new AttemptState());
        if (authentication.verifyCode(recipient, code)) {
            attempts.remove(recipient);
            return true;
        }
        state.failedAttempts++;
        if (state.failedAttempts >= MAX_ATTEMPTS) {
            state.lockedUntilMillis = clock.millis() + LOCKOUT_MILLIS;
            throw new IllegalStateException(
                    "Too many incorrect verification codes. Try again in 15 minutes.");
        }
        return false;
    }

    private void checkLockout(String email) {
        AttemptState state = attempts.get(email);
        if (state == null || state.lockedUntilMillis == 0) {
            return;
        }
        if (state.lockedUntilMillis <= clock.millis()) {
            attempts.remove(email);
            return;
        }
        throw new IllegalStateException(
                "Verification is temporarily locked. Try again in 15 minutes.");
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptState {
        private int failedAttempts;
        private long lockedUntilMillis;
    }
}
