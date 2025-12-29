package com.ntu.cloudgui.app.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class LocalTerminalService {

    private static final List<String> ALLOWED = List.of(
            "mv", "cp", "ls", "mkdir", "ps", "whoami", "tree", "nano"
    );

    public String execute(String commandLine) throws IOException, InterruptedException {
        String trimmed = commandLine.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String base = trimmed.split("\\s+")[0];
        if (!ALLOWED.contains(base)) {
            return "Command not allowed: " + base;
        }

        // macOS shell
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", trimmed);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
        }
        int exit = process.waitFor();
        out.append("[exit code: ").append(exit).append("]");
        return out.toString();
    }
}
