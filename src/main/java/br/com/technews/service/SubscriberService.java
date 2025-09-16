package br.com.technews.service;

import br.com.technews.model.Subscriber;
import br.com.technews.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }
    
    public Page<Subscriber> findAllPaginated(Pageable pageable) {
        return subscriberRepository.findAll(pageable);
    }
    
    public Subscriber findById(Long id) {
        return subscriberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assinante não encontrado com ID: " + id));
    }
    
    public Subscriber updateSubscriber(Subscriber subscriber) {
        if (subscriber.getId() == null) {
            throw new IllegalArgumentException("ID do assinante não pode ser nulo");
        }
        
        // Verifica se existe outro assinante com o mesmo email
        Optional<Subscriber> existingByEmail = subscriberRepository.findByEmail(subscriber.getEmail());
        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(subscriber.getId())) {
            throw new IllegalArgumentException("Email já cadastrado por outro assinante");
        }
        
        return subscriberRepository.save(subscriber);
    }
    
    public void deleteSubscriber(Long id) {
        if (!subscriberRepository.existsById(id)) {
            throw new RuntimeException("Assinante não encontrado com ID: " + id);
        }
        subscriberRepository.deleteById(id);
    }
    
    public long countActiveSubscribers() {
        return subscriberRepository.countByAtivoTrue();
    }
    
    public long countTotalSubscribers() {
        return subscriberRepository.count();
    }
}
