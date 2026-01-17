package com.ntu.cloudgui.app.session;

import com.ntu.cloudgui.app.model.FileMeta;
import com.ntu.cloudgui.app.model.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryStoreTest {

    @Test
    public void saveAndFindUserRoundTrip() {
        InMemoryStore store = InMemoryStore.getInstance();
        String username = "test-user-" + UUID.randomUUID();

        User user = new User();
        user.setUsername(username);
        store.saveUser(user);

        User loaded = store.findUserByUsername(username);
        Assert.assertNotNull("Expected user to be found", loaded);
        Assert.assertEquals(username, loaded.getUsername());

        store.deleteUser(username);
        Assert.assertNull("Expected user to be removed", store.findUserByUsername(username));
    }

    @Test
    public void saveAndFindFileRoundTrip() {
        InMemoryStore store = InMemoryStore.getInstance();
        UUID fileId = UUID.randomUUID();

        FileMeta meta = new FileMeta();
        meta.setId(fileId);
        meta.setName("sample.txt");
        meta.setOwnerUsername("owner-" + fileId);
        store.saveFile(meta);

        Optional<FileMeta> loaded = store.findFileById(fileId);
        Assert.assertTrue("Expected file to be found", loaded.isPresent());
        Assert.assertEquals("sample.txt", loaded.get().getName());

        store.deleteFile(fileId);
        Assert.assertFalse("Expected file to be removed", store.findFileById(fileId).isPresent());
    }
}
