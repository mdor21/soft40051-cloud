import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String password = "admin";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        
        System.out.println("=== BCrypt Hash Generator ===");
        System.out.println("Password: " + password);
        System.out.println("Generated Hash: " + hash);
        
        // Verify it works
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("Verification Test: " + matches);
        System.out.println("============================");
    }
}
