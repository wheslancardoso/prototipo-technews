package br.com.technews.repository;

import br.com.technews.entity.NewsletterSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para agendamento de newsletters
 */
@Repository
public interface NewsletterScheduleRepository extends JpaRepository<NewsletterSchedule, Long> {

    /**
     * Busca agendamentos por status
     */
    List<NewsletterSchedule> findByStatus(NewsletterSchedule.ScheduleStatus status);

    /**
     * Busca agendamentos por status com paginação
     */
    @Query("SELECT ns FROM NewsletterSchedule ns WHERE ns.status = :status ORDER BY ns.scheduledDate DESC")
    Page<NewsletterSchedule> findByStatus(@Param("status") NewsletterSchedule.ScheduleStatus status, Pageable pageable);

    /**
     * Busca agendamentos por status ordenados por data
     */
    Page<NewsletterSchedule> findByStatusOrderByScheduledDateDesc(NewsletterSchedule.ScheduleStatus status, Pageable pageable);

    /**
     * Busca agendamentos recentes com paginação
     */
    @Query("SELECT ns FROM NewsletterSchedule ns ORDER BY ns.createdAt DESC")
    Page<NewsletterSchedule> findRecentSchedules(Pageable pageable);

    /**
     * Conta agendamentos por status
     */
    long countByStatus(NewsletterSchedule.ScheduleStatus status);

    /**
     * Busca agendamentos pendentes até uma data
     */
    @Query("SELECT s FROM NewsletterSchedule s WHERE s.status = 'PENDING' AND s.scheduledDate <= :date ORDER BY s.scheduledDate ASC")
    List<NewsletterSchedule> findPendingSchedulesUntil(@Param("date") LocalDateTime date);

    /**
     * Busca agendamentos em um período
     */
    @Query("SELECT ns FROM NewsletterSchedule ns WHERE ns.scheduledDate BETWEEN :startDate AND :endDate ORDER BY ns.scheduledDate")
    List<NewsletterSchedule> findByScheduledDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Busca agendamentos por data específica
     */
    @Query("SELECT ns FROM NewsletterSchedule ns WHERE DATE(ns.scheduledDate) = DATE(:date)")
    List<NewsletterSchedule> findByScheduledDate(@Param("date") LocalDateTime date);

    /**
     * Busca agendamentos da semana
     */
    @Query("SELECT ns FROM NewsletterSchedule ns WHERE ns.scheduledDate BETWEEN :weekStart AND :weekEnd ORDER BY ns.scheduledDate")
    List<NewsletterSchedule> findByWeek(@Param("weekStart") LocalDateTime weekStart, @Param("weekEnd") LocalDateTime weekEnd);

    /**
     * Busca agendamentos do mês
     */
    @Query("SELECT ns FROM NewsletterSchedule ns WHERE ns.scheduledDate BETWEEN :monthStart AND :monthEnd ORDER BY ns.scheduledDate")
    List<NewsletterSchedule> findByMonth(@Param("monthStart") LocalDateTime monthStart, @Param("monthEnd") LocalDateTime monthEnd);

    /**
     * Busca agendamentos pendentes para envio
     */
    @Query("SELECT ns FROM NewsletterSchedule ns WHERE ns.status = 'PENDING' AND ns.scheduledDate <= :now ORDER BY ns.scheduledDate")
    List<NewsletterSchedule> findPendingSchedulesToSend(@Param("now") LocalDateTime now);

    /**
     * Remove agendamentos antigos que falharam
     */
    @Modifying
    @Query("DELETE FROM NewsletterSchedule ns WHERE ns.status = 'FAILED' AND ns.createdDate < :cutoffDate")
    void deleteOldFailedSchedules(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca agendamentos do dia atual
     */
    @Query("SELECT s FROM NewsletterSchedule s WHERE DATE(s.scheduledDate) = CURRENT_DATE ORDER BY s.scheduledDate ASC")
    List<NewsletterSchedule> findTodaySchedules();

    /**
     * Busca agendamentos da semana atual
     */
    @Query("SELECT s FROM NewsletterSchedule s WHERE YEARWEEK(s.scheduledDate) = YEARWEEK(CURRENT_DATE) ORDER BY s.scheduledDate ASC")
    List<NewsletterSchedule> findWeekSchedules();

    /**
     * Busca agendamentos do mês atual
     */
    @Query("SELECT s FROM NewsletterSchedule s WHERE YEAR(s.scheduledDate) = YEAR(CURRENT_DATE) AND MONTH(s.scheduledDate) = MONTH(CURRENT_DATE) ORDER BY s.scheduledDate ASC")
    List<NewsletterSchedule> findMonthSchedules();

    /**
     * Busca agendamentos por template
     */
    List<NewsletterSchedule> findByTemplateKey(String templateKey);

    /**
     * Busca agendamentos criados por usuário
     */
    List<NewsletterSchedule> findByCreatedBy(String createdBy);

    /**
     * Busca agendamentos com falha para reprocessamento
     */
    @Query("SELECT s FROM NewsletterSchedule s WHERE s.status = 'FAILED' AND s.retryCount < 3 ORDER BY s.scheduledDate ASC")
    List<NewsletterSchedule> findFailedSchedulesForRetry();
}