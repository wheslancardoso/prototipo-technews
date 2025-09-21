package br.com.technews.repository;

import br.com.technews.entity.CollectedNews;
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
public interface CollectedNewsRepository extends JpaRepository<CollectedNews, Long> {

    Optional<CollectedNews> findByOriginalUrl(String originalUrl);

    Optional<CollectedNews> findByContentHash(String contentHash);

    List<CollectedNews> findByStatus(CollectedNews.NewsStatus status);

    Page<CollectedNews> findByStatusOrderByPublishedAtDesc(
        CollectedNews.NewsStatus status, Pageable pageable);

    @Query("SELECT cn FROM CollectedNews cn WHERE cn.status = :status AND " +
           "cn.publishedAt >= :fromDate ORDER BY cn.qualityScore DESC, cn.publishedAt DESC")
    List<CollectedNews> findTopQualityNewsByStatusAndDate(
        @Param("status") CollectedNews.NewsStatus status,
        @Param("fromDate") LocalDateTime fromDate,
        Pageable pageable);

    @Query("SELECT cn FROM CollectedNews cn WHERE cn.status = 'APPROVED' AND " +
           "cn.publishedAt >= :fromDate ORDER BY cn.publishedAt DESC")
    List<CollectedNews> findRecentApprovedNews(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT cn FROM CollectedNews cn WHERE cn.status = 'APPROVED' AND " +
           "cn.qualityScore >= :minScore ORDER BY cn.qualityScore DESC, cn.publishedAt DESC")
    List<CollectedNews> findHighQualityNews(@Param("minScore") Double minScore, Pageable pageable);

    @Query("SELECT COUNT(cn) FROM CollectedNews cn WHERE cn.status = :status")
    long countByStatus(@Param("status") CollectedNews.NewsStatus status);

    @Query("SELECT COUNT(cn) FROM CollectedNews cn WHERE cn.createdAt >= :fromDate")
    long countCollectedSince(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT cn FROM CollectedNews cn WHERE cn.category.id = :categoryId AND " +
           "cn.status = 'APPROVED' ORDER BY cn.publishedAt DESC")
    List<CollectedNews> findApprovedNewsByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    boolean existsByContentHash(String contentHash);
}