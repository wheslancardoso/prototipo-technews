package br.com.technews.repository;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    
    // Busca por email
    Optional<Subscriber> findByEmail(String email);
    
    // Verifica se email existe
    boolean existsByEmail(String email);
    
    // Verifica se email existe excluindo um ID específico
    @Query("SELECT COUNT(s) > 0 FROM Subscriber s WHERE s.email = :email AND s.id != :excludeId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);
    
    // Busca por token de verificação
    Optional<Subscriber> findByVerificationToken(String verificationToken);
    
    // Busca por token de unsubscribe
    Optional<Subscriber> findByUnsubscribeToken(String unsubscribeToken);
    
    // Busca assinantes ativos
    List<Subscriber> findByActiveTrue();
    
    // Busca assinantes ativos com paginação
    Page<Subscriber> findByActiveTrue(Pageable pageable);
    
    // Busca assinantes inativos
    List<Subscriber> findByActiveFalse();
    
    // Busca assinantes com email verificado
    List<Subscriber> findByEmailVerifiedTrue();
    
    // Busca assinantes ativos e com email verificado
    @Query("SELECT s FROM Subscriber s WHERE s.active = true AND s.emailVerified = true")
    List<Subscriber> findActiveAndVerifiedSubscribers();
    
    // Busca assinantes ativos e verificados por frequência
    @Query("SELECT s FROM Subscriber s WHERE s.active = true AND s.emailVerified = true AND s.frequency = :frequency")
    List<Subscriber> findActiveAndVerifiedByFrequency(@Param("frequency") Subscriber.SubscriptionFrequency frequency);
    
    // Busca assinantes por categoria
    @Query("SELECT s FROM Subscriber s JOIN s.subscribedCategories c WHERE c = :category AND s.active = true AND s.emailVerified = true")
    List<Subscriber> findActiveVerifiedByCategory(@Param("category") Category category);
    
    // Busca assinantes por múltiplas categorias
    @Query("SELECT DISTINCT s FROM Subscriber s JOIN s.subscribedCategories c WHERE c IN :categories AND s.active = true AND s.emailVerified = true")
    List<Subscriber> findActiveVerifiedByCategories(@Param("categories") List<Category> categories);
    
    // Busca com filtros e paginação
    @Query("SELECT s FROM Subscriber s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:active IS NULL OR s.active = :active) AND " +
           "(:emailVerified IS NULL OR s.emailVerified = :emailVerified)")
    Page<Subscriber> findWithFilters(@Param("search") String search,
                                   @Param("active") Boolean active,
                                   @Param("emailVerified") Boolean emailVerified,
                                   Pageable pageable);
    
    // Busca assinantes que não receberam email há X dias
    @Query("SELECT s FROM Subscriber s WHERE s.active = true AND s.emailVerified = true AND " +
           "(s.lastEmailSentAt IS NULL OR s.lastEmailSentAt < :cutoffDate)")
    List<Subscriber> findDueForEmail(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Busca assinantes por frequência e que não receberam email há X dias
    @Query("SELECT s FROM Subscriber s WHERE s.active = true AND s.emailVerified = true AND " +
           "s.frequency = :frequency AND " +
           "(s.lastEmailSentAt IS NULL OR s.lastEmailSentAt < :cutoffDate)")
    List<Subscriber> findDueForEmailByFrequency(@Param("frequency") Subscriber.SubscriptionFrequency frequency,
                                              @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Busca tokens de verificação expirados
    @Query("SELECT s FROM Subscriber s WHERE s.verificationToken IS NOT NULL AND " +
           "s.verificationTokenExpiresAt < :now")
    List<Subscriber> findExpiredVerificationTokens(@Param("now") LocalDateTime now);
    
    // Estatísticas - Total de assinantes
    @Query("SELECT COUNT(s) FROM Subscriber s")
    long countTotalSubscribers();
    
    // Estatísticas - Assinantes ativos
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.active = true")
    long countActiveSubscribers();
    
    // Estatísticas - Assinantes verificados
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.emailVerified = true")
    long countVerifiedSubscribers();
    
    // Estatísticas - Assinantes ativos e verificados
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.active = true AND s.emailVerified = true")
    long countActiveAndVerifiedSubscribers();
    
    // Estatísticas por frequência
    @Query("SELECT s.frequency, COUNT(s) FROM Subscriber s WHERE s.active = true GROUP BY s.frequency")
    List<Object[]> countByFrequency();
    
    // Estatísticas - Novos assinantes no período
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.subscribedAt >= :startDate AND s.subscribedAt <= :endDate")
    long countNewSubscribersInPeriod(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    // Estatísticas - Assinantes que cancelaram no período
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.unsubscribedAt >= :startDate AND s.unsubscribedAt <= :endDate")
    long countUnsubscribedInPeriod(@Param("startDate") LocalDateTime startDate, 
                                 @Param("endDate") LocalDateTime endDate);
    
    // Top categorias por número de assinantes
    @Query("SELECT c.name, COUNT(s) FROM Subscriber s JOIN s.subscribedCategories c " +
           "WHERE s.active = true GROUP BY c.id, c.name ORDER BY COUNT(s) DESC")
    List<Object[]> findTopCategoriesBySubscriberCount();
    
    // Busca assinantes recentes (últimos X dias)
    @Query("SELECT s FROM Subscriber s WHERE s.subscribedAt >= :cutoffDate ORDER BY s.subscribedAt DESC")
    List<Subscriber> findRecentSubscribers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Busca assinantes mais ativos (que mais receberam emails)
    @Query("SELECT s FROM Subscriber s WHERE s.active = true ORDER BY s.emailCount DESC")
    List<Subscriber> findMostActiveSubscribers(Pageable pageable);
    
    // Busca por token de gerenciamento
    Optional<Subscriber> findByManageToken(String manageToken);
    
    // Busca por email e token de gerenciamento
    Optional<Subscriber> findByEmailAndManageToken(String email, String manageToken);
    
    // Busca por email ou nome contendo texto (case insensitive)
    @Query("SELECT s FROM Subscriber s WHERE " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    Page<Subscriber> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(@Param("email") String email, @Param("fullName") String fullName, Pageable pageable);
    
    // Busca por frequência
    Page<Subscriber> findByFrequency(Subscriber.SubscriptionFrequency frequency, Pageable pageable);
    
    // Busca por status ativo
    Page<Subscriber> findByActive(Boolean active, Pageable pageable);
    
    // Busca por frequência e status ativo
    Page<Subscriber> findByFrequencyAndActive(Subscriber.SubscriptionFrequency frequency, Boolean active, Pageable pageable);
}
