import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String currentHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM1JiOV7M0OKu9o4.jTW";
        
        System.out.println("Testando senha: " + password);
        System.out.println("Hash atual: " + currentHash);
        System.out.println("Senha confere com hash atual: " + encoder.matches(password, currentHash));
        
        String newHash = encoder.encode(password);
        System.out.println("Novo hash gerado: " + newHash);
        System.out.println("Senha confere com novo hash: " + encoder.matches(password, newHash));
    }
}