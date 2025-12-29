package com.ntu.cloudgui.app.session;

import com.ntu.cloudgui.app.model.Role;
import com.ntu.cloudgui.app.model.User;
import com.ntu.cloudgui.app.db.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

public class SessionState {

    private static final SessionState INSTANCE = new SessionState();

    private User currentUser;
    private boolean online = true; // later: detect MySQL availability

    private SessionState() {}

    public static SessionState getInstance() {
        return INSTANCE;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Ensure a default admin user exists in the database.
     * If not present, create it with username "admin" and password "admin".
     */
    public void ensureDefaultAdmin() {
        UserRepository repo = new UserRepository();
        try {
            User admin = repo.findByUsername("admin");
            if (admin == null) {
                admin = new User();
                admin.setUsername("admin");
                admin.setRole(Role.ADMIN);

                String hash = BCrypt.hashpw("admin", BCrypt.gensalt());
                admin.setPasswordHash(hash);

                repo.save(admin);
            }
        } catch (Exception e) {
            // You might want to log this with LoggingService
            System.err.println("Error ensuring default admin: " + e.getMessage());
        }
    }
}
