package com.ntu.cloudgui.hostmanager.docker;

/**
 * Represents the result of a process execution.
 */
public class ProcessResult {

    private final int exitCode;
    private final String output;
    private final String error;

    public ProcessResult(int exitCode, String output, String error) {
        this.exitCode = exitCode;
        this.output = output;
        this.error = error;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
