package com.ntu.cloudgui.aggservice;

import java.util.Arrays;
import java.util.List;

public class Configuration {

    public String getDbHost() {
        return System.getenv("DB_HOST");
    }

    public int getDbPort() {
        return Integer.parseInt(System.getenv("DB_PORT"));
    }

    public String getDbName() {
        return System.getenv("DB_NAME");
    }

    public String getDbUser() {
        return System.getenv("DB_USER");
    }

    public String getDbPassword() {
        return System.getenv("DB_PASSWORD");
    }

    public List<String> getFileServers() {
        return Arrays.asList(System.getenv("FILE_SERVERS").split(","));
    }

    public int getAggregatorPort() {
        String port = System.getenv("AGGREGATOR_PORT");
        return port != null ? Integer.parseInt(port) : 9000;
    }

    public int getSemaphorePermits() {
        String permits = System.getenv("SEMAPHORE_PERMITS");
        return permits != null ? Integer.parseInt(permits) : 10;
    }
}
