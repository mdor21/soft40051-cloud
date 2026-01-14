package com.ntu.cloudgui.cloudlb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileBufferStore {

    private static final Map<String, FilePayload> STORE = new ConcurrentHashMap<>();

    private FileBufferStore() {
    }

    public static void put(String fileId, String fileName, byte[] content) {
        if (fileId == null || fileId.isBlank() || content == null) {
            return;
        }
        STORE.put(fileId, new FilePayload(fileName, content));
    }

    public static FilePayload take(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return STORE.remove(fileId);
    }

    public static class FilePayload {
        private final String fileName;
        private final byte[] content;

        public FilePayload(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
