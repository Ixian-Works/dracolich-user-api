package dm.dracolich.user.dto.auth;

/** The token from the emailed reset link, and the new password. */
public record ResetPasswordRequest(String token, String password) { }
