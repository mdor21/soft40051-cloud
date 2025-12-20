package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.model.Role;
import com.ntu.cloudgui.app.model.User;
import com.ntu.cloudgui.app.db.UserRepository;
import com.ntu.cloudgui.app.session.SessionState;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserRepository userRepo = new UserRepository();
    private final LoggingService logger = LoggingService.getInstance();

    public boolean login(String username, String password) {
        User user = null;
        try {
            user = userRepo.findByUsername(username);
        } catch (Exception e) {
            logger.log("SYSTEM", "LOGIN_ERROR", e.getMessage(), false);
            return false;
        }

        boolean ok = false;
        if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            ok = true;
            SessionState.getInstance().setCurrentUser(user);
        }
        logger.log(username, "LOGIN", ok ? "Login successful" : "Login failed", ok);
        return ok;
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
        try {
            userRepo.save(u);
            logger.log(getCurrentUsername(), "CREATE_USER",
                    "Created user " + username + " with role " + role, true);
        } catch (Exception e) {
            logger.log(getCurrentUsername(), "CREATE_USER_ERROR", e.getMessage(), false);
        }
        return u;
    }

    public void deleteUser(String username) {
        if (!SessionState.getInstance().isAdmin()) {
            logger.log(getCurrentUsername(), "UNAUTHORISED", "Attempt to DELETE_USER", false);
            return;
        }

        try {
            User target = userRepo.findByUsername(username);
            if (target != null) {
                // You could also implement a deleteById in UserRepository
                // For now, update role or mark inactive
                logger.log(getCurrentUsername(), "DELETE_USER",
                        "Deleted user " + username, true);
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

        try {
            User target = userRepo.findByUsername(username);
            if (target != null) {
                target.setRole(newRole);
                userRepo.save(target);
                logger.log(getCurrentUsername(), "CHANGE_ROLE",
                        "Changed role of " + username + " to " + newRole, true);
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
