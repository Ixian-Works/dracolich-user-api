package dm.dracolich.user.dto.auth;

/** A signed-in player's current password, and the new one. */
public record ChangePasswordRequest(String currentPassword, String newPassword) { }
