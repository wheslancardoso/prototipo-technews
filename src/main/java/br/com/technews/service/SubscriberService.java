package br.com.technews.service;

import br.com.technews.model.Subscriber;
import br.com.technews.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriberService {
    
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    public Subscriber subscribe(String nome, String email) {
        // Verifica se o email já existe
        if (subscriberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado: " + email);
        }
        
        // Cria novo assinante
        Subscriber subscriber = new Subscriber(nome, email);
        return subscriberRepository.save(subscriber);
    }
    
    public boolean isEmailAlreadySubscribed(String email) {
        return subscriberRepository.existsByEmail(email);
    }
}
