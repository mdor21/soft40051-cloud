package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.db.SessionCacheRepository;
import com.ntu.cloudgui.app.model.Role;
import com.ntu.cloudgui.app.model.User;
import com.ntu.cloudgui.app.db.UserRepository;
import com.ntu.cloudgui.app.session.SessionState;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserRepository userRepo = new UserRepository();
    private final SessionCacheRepository sessionCacheRepo = new SessionCacheRepository();
    private final LoggingService logger = LoggingService.getInstance();
    private final Gson gson = new Gson();

    public boolean login(String username, String password) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Username: " + username);

        User user = null;
        boolean online = DatabaseManager.isMysqlConnected();

        try {
            if (online) {
                System.out.println("Online mode: authenticating with remote DB.");
                user = userRepo.findByUsername(username);
                if (user != null) {
                    // Cache the user data locally for offline access
                    sessionCacheRepo.cacheUser(user);
                }
            } else {
                System.out.println("Offline mode: authenticating with local cache.");
                user = sessionCacheRepo.findCachedUserByUsername(username);
            }
        } catch (Exception e) {
            System.err.println("ERROR finding user: " + e.getMessage());
            logger.log("SYSTEM", "LOGIN_ERROR", e.getMessage(), false);
            return false;
        }

        if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            SessionState.getInstance().setCurrentUser(user);
            logger.log(username, "LOGIN", "Login successful (Mode: " + (online ? "Online" : "Offline") + ")", true);
            System.out.println("Login successful.");
            System.out.println("===================");
            return true;
        } else {
            logger.log(username, "LOGIN", "Login failed (Mode: " + (online ? "Online" : "Offline") + ")", false);
            System.out.println("Login failed.");
            System.out.println("===================");
            return false;
        }
    }

    public void logout() {
        User u = SessionState.getInstance().getCurrentUser();
        if (u != null) {
            logger.log(u.getUsername(), "LOGOUT", "User logged out", true);
        }
        SessionState.getInstance().setCurrentUser(null);
    }

    public User createUser(String username, String plainPassword, Role role) {
        if (!SessionState.getInstance().isAdmin()) {
            logger.log(getCurrentUsername(), "UNAUTHORISED", "Attempt to CREATE_USER", false);
            return null;
        }

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setRole(role);

        boolean online = DatabaseManager.isMysqlConnected();

        try {
            if (online) {
                userRepo.save(u);
                // After saving, re-fetch to get the server-generated timestamp
                User savedUser = userRepo.findByUsername(username);
                if (savedUser != null) {
                    sessionCacheRepo.cacheUser(savedUser);
                }
                logger.log(getCurrentUsername(), "CREATE_USER", "Created user " + username, true);
            } else {
                // Offline: queue the creation
                String payload = gson.toJson(u);
                sessionCacheRepo.queueOperation(SessionCacheRepository.OP_USER_CREATE, u.getUsername(), payload);
                logger.log(getCurrentUsername(), "CREATE_USER_OFFLINE", "Queued creation for user " + username, true);
            }
        } catch (Exception e) {
            logger.log(getCurrentUsername(), "CREATE_USER_ERROR", e.getMessage(), false);
            return null;
        }
        return u;
    }

    public void deleteUser(String username) {
        if (!SessionState.getInstance().isAdmin()) {
            logger.log(getCurrentUsername(), "UNAUTHORISED", "Attempt to DELETE_USER", false);
            return;
        }

        boolean online = DatabaseManager.isMysqlConnected();

        try {
            User target = online ? userRepo.findByUsername(username) : null;
            if (target == null && !online) {
                 // Offline, we don't have the full user object, but we can queue the delete.
                 // The sync service will need to handle potential conflicts (e.g., if the user was modified).
                 String payload = "{\"username\":\"" + username + "\"}";
                 sessionCacheRepo.queueOperation(SessionCacheRepository.OP_USER_DELETE, username, payload);
                 logger.log(getCurrentUsername(), "DELETE_USER_OFFLINE", "Queued deletion for user " + username, true);
                 return;
            }

            if (target != null) {
                if (online) {
                    userRepo.deleteByUsername(username);
                    logger.log(getCurrentUsername(), "DELETE_USER", "Deleted user " + username, true);
                 } else {
                    String payload = gson.toJson(target);
                    sessionCacheRepo.queueOperation(SessionCacheRepository.OP_USER_DELETE, username, payload);
                    logger.log(getCurrentUsername(), "DELETE_USER_OFFLINE", "Queued deletion for user " + username, true);
                 }
            }
        } catch (Exception e) {
            logger.log(getCurrentUsername(), "DELETE_USER_ERROR", e.getMessage(), false);
        }
    }

    public void changeRole(String username, Role newRole) {
        if (!SessionState.getInstance().isAdmin()) {
            logger.log(getCurrentUsername(), "UNAUTHORISED", "Attempt to CHANGE_ROLE", false);
            return;
        }

        boolean online = DatabaseManager.isMysqlConnected();

        try {
            User target = online ? userRepo.findByUsername(username) : sessionCacheRepo.findCachedUserByUsername(username);

            if (target != null) {
                target.setRole(newRole);
                if (online) {
                    userRepo.save(target);
                    User updatedUser = userRepo.findByUsername(username); // Re-fetch to get new timestamp
                    if (updatedUser != null) {
                        sessionCacheRepo.cacheUser(updatedUser);
                        // Update session state if the current user was modified
                        if (SessionState.getInstance().getCurrentUser().getUsername().equals(username)) {
                            SessionState.getInstance().setCurrentUser(updatedUser);
                        }
                    }
                    logger.log(getCurrentUsername(), "CHANGE_ROLE", "Changed role of " + username + " to " + newRole, true);
                } else {
                    String payload = gson.toJson(target);
                    sessionCacheRepo.queueOperation(SessionCacheRepository.OP_USER_UPDATE, username, payload);
                    logger.log(getCurrentUsername(), "CHANGE_ROLE_OFFLINE", "Queued role change for user " + username, true);
                }
            }
        } catch (Exception e) {
            logger.log(getCurrentUsername(), "CHANGE_ROLE_ERROR", e.getMessage(), false);
        }
    }

    public boolean changePassword(long currentUserId, String oldPassword, String newPassword) {
        if (!DatabaseManager.isMysqlConnected()) {
            logger.log(getCurrentUsername(), "CHANGE_PASSWORD_OFFLINE",
                "Password change requires MySQL connection", false);
            return false;
        }

        String username = getCurrentUsername();
        try {
            User user = userRepo.findByUsername(username);
            if (user == null || user.getId() == null || user.getId() != currentUserId) {
                logger.log(username, "CHANGE_PASSWORD_ERROR", "User not found", false);
                return false;
            }

            if (!BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
                logger.log(username, "CHANGE_PASSWORD_INVALID", "Old password mismatch", false);
                return false;
            }

            String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            userRepo.updatePasswordHash(user.getId(), hash);
            user.setPasswordHash(hash);
            sessionCacheRepo.cacheUser(user);
            SessionState.getInstance().setCurrentUser(user);
            logger.log(username, "CHANGE_PASSWORD", "Password updated", true);
            return true;
        } catch (Exception e) {
            logger.log(username, "CHANGE_PASSWORD_ERROR", e.getMessage(), false);
            return false;
        }
    }

    public boolean resetPassword(String targetUsername, String newPassword) {
        if (!SessionState.getInstance().isAdmin()) {
            logger.log(getCurrentUsername(), "UNAUTHORISED", "Attempt to RESET_PASSWORD", false);
            return false;
        }
        if (!DatabaseManager.isMysqlConnected()) {
            logger.log(getCurrentUsername(), "RESET_PASSWORD_OFFLINE",
                "Password reset requires MySQL connection", false);
            return false;
        }

        try {
            User target = userRepo.findByUsername(targetUsername);
            if (target == null || target.getId() == null) {
                logger.log(getCurrentUsername(), "RESET_PASSWORD_ERROR",
                    "User not found: " + targetUsername, false);
                return false;
            }

            String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            userRepo.updatePasswordHash(target.getId(), hash);
            target.setPasswordHash(hash);
            sessionCacheRepo.cacheUser(target);
            logger.log(getCurrentUsername(), "RESET_PASSWORD",
                "Reset password for " + targetUsername, true);
            return true;
        } catch (Exception e) {
            logger.log(getCurrentUsername(), "RESET_PASSWORD_ERROR", e.getMessage(), false);
            return false;
        }
    }

    private String getCurrentUsername() {
        User u = SessionState.getInstance().getCurrentUser();
        return u == null ? "SYSTEM" : u.getUsername();
    }
}
