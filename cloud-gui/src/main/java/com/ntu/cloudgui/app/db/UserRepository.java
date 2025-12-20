package com.ntu.cloudgui.app.db;

import com.ntu.cloudgui.app.model.Role;
import com.ntu.cloudgui.app.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public User findByUsername(String username) throws Exception {
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, password_hash, role FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRole(Role.valueOf(rs.getString("role").toUpperCase()));
                    return u;
                }
            }
        }
        return null;
    }

    public void save(User user) throws Exception {
        try (Connection conn = MySqlConnectionManager.getConnection()) {
            if (user.getId() == null) {
                // Insert new user
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, user.getUsername());
                    stmt.setString(2, user.getPasswordHash());
                    stmt.setString(3, user.getRole().name());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            user.setId(keys.getLong(1));
                        }
                    }
                }
            } else {
                // Update existing user
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET username = ?, password_hash = ?, role = ? WHERE id = ?")) {
                    stmt.setString(1, user.getUsername());
                    stmt.setString(2, user.getPasswordHash());
                    stmt.setString(3, user.getRole().name());
                    stmt.setLong(4, user.getId());
                    stmt.executeUpdate();
                }
            }
        }
    }

    public List<User> findAll() throws Exception {
        List<User> users = new ArrayList<>();
        try (Connection conn = MySqlConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, password_hash, role FROM users")) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRole(Role.valueOf(rs.getString("role").toUpperCase()));
                users.add(u);
            }
        }
        return users;
    }

    public void deleteByUsername(String username) throws Exception {
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }
}
