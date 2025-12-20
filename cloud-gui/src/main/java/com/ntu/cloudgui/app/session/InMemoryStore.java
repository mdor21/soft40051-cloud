package com.ntu.cloudgui.app.session;

import com.ntu.cloudgui.app.model.FileMeta;
import com.ntu.cloudgui.app.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore {

    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<UUID, FileMeta> files = new ConcurrentHashMap<>();

    private InMemoryStore() {}

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    public User findUserByUsername(String username) {
        return users.get(username);
    }

    public void saveUser(User user) {
        users.put(user.getUsername(), user);
    }

    public void deleteUser(String username) {
        users.remove(username);
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public Collection<FileMeta> getAllFiles() {
        return files.values();
    }

    public void saveFile(FileMeta meta) {
        files.put(meta.getId(), meta);
    }

    public void deleteFile(UUID id) {
        files.remove(id);
    }

    public Optional<FileMeta> findFileById(UUID id) {
        return Optional.ofNullable(files.get(id));
    }
}
