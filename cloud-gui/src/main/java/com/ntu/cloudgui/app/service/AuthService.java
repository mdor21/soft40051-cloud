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
                sessionCacheRepo.cacheUser(u); // Keep cache in sync
                logger.log(getCurrentUsername(), "CREATE_USER", "Created user " + username, true);
            } else {
                // Offline: queue the creation
                String payload = gson.toJson(u);
                sessionCacheRepo.queueOperation("CREATE", "USER", u.getUsername(), payload);
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
            if (online) {
                userRepo.deleteByUsername(username);
                // Also remove from cache if exists
                logger.log(getCurrentUsername(), "DELETE_USER", "Deleted user " + username, true);
            } else {
                String payload = "{\"username\":\"" + username + "\"}";
                sessionCacheRepo.queueOperation("DELETE", "USER", username, payload);
                logger.log(getCurrentUsername(), "DELETE_USER_OFFLINE", "Queued deletion for user " + username, true);
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
            User target;
            if (online) {
                target = userRepo.findByUsername(username);
                 if (target != null) {
                    target.setRole(newRole);
                    userRepo.save(target);
                    sessionCacheRepo.cacheUser(target); // Update cache
                    logger.log(getCurrentUsername(), "CHANGE_ROLE", "Changed role of " + username + " to " + newRole, true);
                }
            } else {
                 // Offline, we can't fetch the user, so we queue the operation based on what we know.
                 // The payload needs enough info for the sync service to perform the update.
                String payload = "{\"username\":\"" + username + "\", \"role\":\"" + newRole.name() + "\"}";
                sessionCacheRepo.queueOperation("UPDATE", "USER", username, payload);
                logger.log(getCurrentUsername(), "CHANGE_ROLE_OFFLINE", "Queued role change for user " + username, true);
            }
        } catch (Exception e) {
            logger.log(getCurrentUsername(), "CHANGE_ROLE_ERROR", e.getMessage(), false);
        }
    }

    private String getCurrentUsername() {
        User u = SessionState.getInstance().getCurrentUser();
        return u == null ? "SYSTEM" : u.getUsername();
    }
}
