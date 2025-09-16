package br.com.technews.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "trusted_sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustedSource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String domainName;
    
    public TrustedSource(String domainName) {
        this.domainName = domainName;
    }
}
