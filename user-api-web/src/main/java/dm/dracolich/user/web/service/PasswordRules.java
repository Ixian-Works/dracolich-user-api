package dm.dracolich.user.web.service;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.user.dto.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What a password must be, for registration, a reset and a change: at least 8 characters; at most
 * 72 bytes in UTF-8, since BCrypt ignores or refuses anything longer; and not one of the most common
 * passwords ({@code passwords/common.txt}). A refusal is 400 with DMD018 and a message saying which.
 */
public final class PasswordRules {

    public static final int MIN_CHARACTERS = 8;
    public static final int MAX_BYTES = 72;

    private static final Set<String> COMMON = load();

    private PasswordRules() {
    }

    /** Throws the refusal when the password breaks a rule. */
    public static void check(String password) {
        String problem = problem(password);
        if (problem != null) {
            throw new ResponseException(problem, List.of(new ApiError(ErrorCodes.DMD018)), HttpStatus.BAD_REQUEST);
        }
    }

    static String problem(String password) {
        if (password == null || password.codePointCount(0, password.length()) < MIN_CHARACTERS) {
            return "Password must be at least " + MIN_CHARACTERS + " characters.";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return "Password must be at most " + MAX_BYTES + " bytes.";
        }
        if (COMMON.contains(password.toLowerCase(Locale.ROOT))) {
            return "That password is too common. Choose another.";
        }
        return null;
    }

    private static Set<String> load() {
        try (InputStream in = PasswordRules.class.getResourceAsStream("/passwords/common.txt")) {
            if (in == null) return Set.of();
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
