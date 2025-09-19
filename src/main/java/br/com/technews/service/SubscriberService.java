package br.com.technews.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import br.com.technews.repository.SubscriberRepository;
import br.com.technews.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberService {

    private static final Logger log = LoggerFactory.getLogger(SubscriberService.class);
    private final SubscriberRepository subscriberRepository;
    private final CategoryRepository categoryRepository;
    private final EmailService emailService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Cria nova inscrição na newsletter
     */
    @Transactional
    public Subscriber subscribe(String email, String fullName, 
                               Subscriber.SubscriptionFrequency frequency, 
                               Set<Long> categoryIds) {
        
        // Validações
        validateEmail(email);
        validateFullName(fullName);
        
        if (subscriberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este email já está inscrito na newsletter");
        }

        // Busca categorias selecionadas
        Set<Category> categories = null;
        if (categoryIds != null && !categoryIds.isEmpty()) {
            categories = Set.copyOf(categoryRepository.findAllById(categoryIds));
        }

        // Cria novo assinante
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email.toLowerCase().trim());
        subscriber.setFullName(fullName.trim());
        subscriber.setFrequency(frequency != null ? frequency : Subscriber.SubscriptionFrequency.WEEKLY);
        subscriber.setSubscribedCategories(categories);
        subscriber.setActive(true);
        subscriber.setEmailVerified(false);
        subscriber.setSubscribedAt(LocalDateTime.now());
        
        // Gera tokens
        subscriber.setVerificationToken(UUID.randomUUID().toString());
        subscriber.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        subscriber.setUnsubscribeToken(UUID.randomUUID().toString());

        subscriber = subscriberRepository.save(subscriber);

        // Envia email de verificação
        emailService.sendVerificationEmail(subscriber);

        log.info("Nova inscrição criada: {} com frequência {}", email, frequency);
        return subscriber;
    }

    /**
     * Verifica email do assinante
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByVerificationToken(token);
        
        if (subscriberOpt.isEmpty()) {
            return false;
        }

        Subscriber subscriber = subscriberOpt.get();
        
        // Verifica se token não expirou
        if (subscriber.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Marca como verificado
        subscriber.setEmailVerified(true);
        subscriber.setVerificationToken(null);
        subscriber.setVerificationTokenExpiresAt(null);
        subscriber.setVerifiedAt(LocalDateTime.now());

        subscriberRepository.save(subscriber);

        // Envia email de boas-vindas
        emailService.sendWelcomeEmail(subscriber);

        log.info("Email verificado com sucesso: {}", subscriber.getEmail());
        return true;
    }

    /**
     * Cancela inscrição
     */
    @Transactional
    public boolean unsubscribe(String token) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByUnsubscribeToken(token);
        
        if (subscriberOpt.isEmpty()) {
            return false;
        }

        Subscriber subscriber = subscriberOpt.get();
        subscriber.setActive(false);
        subscriber.setUnsubscribedAt(LocalDateTime.now());

        subscriberRepository.save(subscriber);

        // Envia email de confirmação
        emailService.sendUnsubscribeConfirmationEmail(subscriber);

        log.info("Inscrição cancelada: {}", subscriber.getEmail());
        return true;
    }

    /**
     * Busca assinante por email
     */
    public Optional<Subscriber> findByEmail(String email) {
        return subscriberRepository.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Busca assinante por ID
     */
    public Optional<Subscriber> findById(Long id) {
        return subscriberRepository.findById(id);
    }

    /**
     * Lista todos os assinantes com filtros e paginação
     */
    public Page<Subscriber> findAll(String search, Boolean active, Boolean emailVerified, Pageable pageable) {
        return subscriberRepository.findWithFilters(search, active, emailVerified, pageable);
    }

    /**
     * Lista assinantes ativos
     */
    public List<Subscriber> findActiveSubscribers() {
        return subscriberRepository.findByActiveTrue();
    }

    /**
     * Lista assinantes ativos e verificados
     */
    public List<Subscriber> findActiveAndVerifiedSubscribers() {
        return subscriberRepository.findActiveAndVerifiedSubscribers();
    }

    /**
     * Cria ou atualiza assinante (admin)
     */
    @Transactional
    public Subscriber save(Subscriber subscriber) {
        if (subscriber.getId() == null) {
            // Novo assinante
            validateEmail(subscriber.getEmail());
            validateFullName(subscriber.getFullName());
            
            if (subscriberRepository.existsByEmail(subscriber.getEmail())) {
                throw new IllegalArgumentException("Este email já está inscrito");
            }

            subscriber.setEmail(subscriber.getEmail().toLowerCase().trim());
            subscriber.setFullName(subscriber.getFullName().trim());
            subscriber.setSubscribedAt(LocalDateTime.now());
            
            if (subscriber.getUnsubscribeToken() == null) {
                subscriber.setUnsubscribeToken(UUID.randomUUID().toString());
            }
            
            if (!subscriber.isEmailVerified() && subscriber.getVerificationToken() == null) {
                subscriber.setVerificationToken(UUID.randomUUID().toString());
                subscriber.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            }
        } else {
            // Atualização
            validateEmail(subscriber.getEmail());
            validateFullName(subscriber.getFullName());
            
            if (subscriberRepository.existsByEmailAndIdNot(subscriber.getEmail(), subscriber.getId())) {
                throw new IllegalArgumentException("Este email já está sendo usado por outro assinante");
            }

            subscriber.setEmail(subscriber.getEmail().toLowerCase().trim());
            subscriber.setFullName(subscriber.getFullName().trim());
            subscriber.setUpdatedAt(LocalDateTime.now());
        }

        return subscriberRepository.save(subscriber);
    }

    /**
     * Remove assinante
     */
    @Transactional
    public void delete(Long id) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinante não encontrado"));
        
        subscriberRepository.delete(subscriber);
        log.info("Assinante removido: {}", subscriber.getEmail());
    }

    /**
     * Ativa/desativa assinante
     */
    @Transactional
    public Subscriber toggleActive(Long id) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinante não encontrado"));
        
        subscriber.setActive(!subscriber.isActive());
        
        if (!subscriber.isActive()) {
            subscriber.setUnsubscribedAt(LocalDateTime.now());
        } else {
            subscriber.setUnsubscribedAt(null);
            subscriber.setReactivatedAt(LocalDateTime.now());
        }
        
        subscriber.setUpdatedAt(LocalDateTime.now());
        subscriber = subscriberRepository.save(subscriber);
        
        log.info("Status do assinante {} alterado para: {}", subscriber.getEmail(), 
                subscriber.isActive() ? "ativo" : "inativo");
        
        return subscriber;
    }

    /**
     * Obtém estatísticas dos assinantes
     */
    public SubscriberStats getStats() {
        long total = subscriberRepository.countTotalSubscribers();
        long active = subscriberRepository.countActiveSubscribers();
        long verified = subscriberRepository.countVerifiedSubscribers();
        long activeAndVerified = subscriberRepository.countActiveAndVerifiedSubscribers();
        
        List<Object[]> frequencyStats = subscriberRepository.countByFrequency();
        
        return new SubscriberStats(total, active, verified, activeAndVerified, frequencyStats);
    }

    /**
     * Valida formato do email
     */
    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email é obrigatório");
        }
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Formato de email inválido");
        }
    }

    /**
     * Valida nome completo
     */
    private void validateFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("Nome completo é obrigatório");
        }
        
        if (fullName.trim().length() < 2) {
            throw new IllegalArgumentException("Nome deve ter pelo menos 2 caracteres");
        }
        
        if (fullName.trim().length() > 100) {
            throw new IllegalArgumentException("Nome não pode ter mais de 100 caracteres");
        }
    }

    /**
     * Classe para estatísticas dos assinantes
     */
    public static class SubscriberStats {
        private final long total;
        private final long active;
        private final long verified;
        private final long activeAndVerified;
        private final long unverified;
        private final List<Object[]> frequencyStats;
        private final List<Object[]> frequencyDistribution;
        private final List<Object[]> categoryDistribution;
        private final List<Object[]> recentSubscriptions;
        private final double monthlyGrowth;

        public SubscriberStats(long total, long active, long verified, long activeAndVerified, List<Object[]> frequencyStats) {
            this.total = total;
            this.active = active;
            this.verified = verified;
            this.activeAndVerified = activeAndVerified;
            this.unverified = total - verified;
            this.frequencyStats = frequencyStats;
            this.frequencyDistribution = frequencyStats;
            this.categoryDistribution = List.of();
            this.recentSubscriptions = List.of();
            this.monthlyGrowth = 0.0;
        }

        // Getters
        public long getTotal() { return total; }
        public long getActive() { return active; }
        public long getVerified() { return verified; }
        public long getActiveAndVerified() { return activeAndVerified; }
        public long getUnverifiedSubscribers() { return unverified; }
        public List<Object[]> getFrequencyStats() { return frequencyStats; }
        public List<Object[]> getFrequencyDistribution() { return frequencyDistribution; }
        public List<Object[]> getCategoryDistribution() { return categoryDistribution; }
        public List<Object[]> getRecentSubscriptions() { return recentSubscriptions; }
        public double getMonthlyGrowth() { return monthlyGrowth; }
        
        // Métodos adicionais para compatibilidade
        public long getTotalSubscribers() { return total; }
        public long getActiveSubscribers() { return active; }
        public long getVerifiedSubscribers() { return verified; }
    }

    /**
     * Verifica se um email já está inscrito
     */
    public boolean isEmailSubscribed(String email) {
        return subscriberRepository.existsByEmail(email);
    }

    /**
     * Busca subscriber por token de gerenciamento
     */
    public Optional<Subscriber> findByManageToken(String token) {
        return subscriberRepository.findByManageToken(token);
    }

    /**
     * Desinscreve usando email e token
     */
    @Transactional
    public boolean unsubscribe(String email, String token) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmailAndManageToken(email, token);
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.setActive(false);
            subscriberRepository.save(subscriber);
            return true;
        }
        return false;
    }

    /**
     * Reativa uma inscrição
     */
    @Transactional
    public boolean reactivateSubscription(String email) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmail(email);
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.setActive(true);
            subscriberRepository.save(subscriber);
            return true;
        }
        return false;
    }

    /**
     * Atualiza preferências do subscriber
     */
    @Transactional
    public void updatePreferences(String token, Subscriber.SubscriptionFrequency frequency, 
                                 Set<Long> categoryIds, String fullName) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByManageToken(token);
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.setFrequency(frequency);
            subscriber.setFullName(fullName);
            
            // Atualizar categorias se fornecidas
            if (categoryIds != null && !categoryIds.isEmpty()) {
                Set<Category> categories = categoryRepository.findAllById(categoryIds)
                    .stream().collect(Collectors.toSet());
                subscriber.setSubscribedCategories(categories);
            }
            
            subscriberRepository.save(subscriber);
        }
    }

    /**
     * Busca por email ou nome contendo texto
     */
    public Page<Subscriber> findByEmailContainingOrNomeContaining(String email, String nome, Pageable pageable) {
        return subscriberRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(email, nome, pageable);
    }

    /**
     * Busca por frequência e status ativo
     */
    public Page<Subscriber> findByFrequenciaAndAtivo(Subscriber.SubscriptionFrequency frequency, Boolean active, Pageable pageable) {
        if (frequency != null && active != null) {
            return subscriberRepository.findByFrequencyAndActive(frequency, active, pageable);
        } else if (frequency != null) {
            return subscriberRepository.findByFrequency(frequency, pageable);
        } else if (active != null) {
            return subscriberRepository.findByActive(active, pageable);
        } else {
            return subscriberRepository.findAll(pageable);
        }
    }

    /**
     * Busca por status ativo
     */
    public Page<Subscriber> findByAtivo(Boolean active, Pageable pageable) {
        return subscriberRepository.findByActive(active, pageable);
    }

    /**
     * Obtém todos os subscribers
     */
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }
}
