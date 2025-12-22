public class AppConfig {
    private String dbHost;
    private String dbPort;
    private String sshHost;
    private String encryptionAlgo;
    
    public AppConfig() {
        // Load from environment variables
        this.dbHost = System.getenv("DB_HOST");
        this.dbPort = System.getenv("DB_PORT");
        this.sshHost = System.getenv("SSH_HOST");
        this.encryptionAlgo = "AES/256/GCM";
    }
    
    // Getters for all config
    public String getDbHost() { return dbHost; }
    public String getDbPort() { return dbPort; }
    // ... more getters
}
