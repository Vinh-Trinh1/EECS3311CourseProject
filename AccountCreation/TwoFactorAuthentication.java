package AccountCreation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TwoFactorAuthentication implements TwoFactorService {
    private static final long CODE_LIFETIME_MILLIS = 5 * 60 * 1000L;

    private final EmailCodeDelivery emailDelivery;
    private final Clock clock;
    private final SecureRandom random;
    private final Map<String, VerificationCode> codes = new HashMap<>();

    public TwoFactorAuthentication(EmailCodeDelivery emailDelivery) {
        this(emailDelivery, Clock.systemUTC(), new SecureRandom());
    }

    TwoFactorAuthentication(EmailCodeDelivery emailDelivery, Clock clock, SecureRandom random) {
        this.emailDelivery = Objects.requireNonNull(emailDelivery, "Email delivery is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.random = Objects.requireNonNull(random, "Random source is required.");
    }

    @Override
    public synchronized void requestCode(String email) throws IOException {
        String recipient = normalizeEmail(email);
        codes.remove(recipient);
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
        emailDelivery.deliver(recipient, code);
        codes.put(recipient, new VerificationCode(hash(code), clock.millis() + CODE_LIFETIME_MILLIS));
    }

    @Override
    public synchronized boolean verifyCode(String email, String code) {
        String recipient = normalizeEmail(email);
        VerificationCode stored = codes.get(recipient);
        if (stored == null) {
            return false;
        }
        if (clock.millis() >= stored.expiresAtMillis) {
            codes.remove(recipient);
            return false;
        }
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        byte[] candidate = hash(code);
        if (!MessageDigest.isEqual(stored.codeHash, candidate)) {
            Arrays.fill(candidate, (byte) 0);
            return false;
        }
        Arrays.fill(candidate, (byte) 0);
        codes.remove(recipient);
        return true;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] hash(String code) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static final class VerificationCode {
        private final byte[] codeHash;
        private final long expiresAtMillis;

        private VerificationCode(byte[] codeHash, long expiresAtMillis) {
            this.codeHash = codeHash;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
