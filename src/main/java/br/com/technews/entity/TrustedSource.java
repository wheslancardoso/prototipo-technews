package br.com.technews.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "trusted_sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustedSource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome da fonte é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Column(nullable = false)
    private String name;
    
    @NotBlank(message = "Domínio é obrigatório")
    @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Formato de domínio inválido")
    @Column(nullable = false, unique = true)
    private String domainName;
    
    @Size(max = 500, message = "Descrição não pode exceder 500 caracteres")
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public TrustedSource(String name, String domainName) {
        this.name = name;
        this.domainName = domainName;
        this.active = true;
    }
    
    public TrustedSource(String name, String domainName, String description) {
        this.name = name;
        this.domainName = domainName;
        this.description = description;
        this.active = true;
    }
    
    // Método utilitário para verificar se a fonte está ativa
    public boolean isActive() {
        return active != null && active;
    }
    
    // Método para ativar/desativar a fonte
    public void toggleActive() {
        this.active = !this.active;
    }
    
    @Override
    public String toString() {
        return "TrustedSource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", domainName='" + domainName + '\'' +
                ", active=" + active +
                '}';
    }
}
