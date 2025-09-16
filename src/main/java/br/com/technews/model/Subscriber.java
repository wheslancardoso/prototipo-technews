package br.com.technews.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscribers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscriber {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nome;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private boolean ativo = true;
    
    @Column(nullable = false)
    private LocalDateTime dataInscricao = LocalDateTime.now();
    
    public Subscriber(String nome, String email) {
        this.nome = nome;
        this.email = email;
        this.ativo = true;
        this.dataInscricao = LocalDateTime.now();
    }
}
