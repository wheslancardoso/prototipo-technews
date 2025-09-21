package br.com.technews.repository;

import br.com.technews.entity.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    List<NewsSource> findByActiveTrue();

    Optional<NewsSource> findByUrl(String url);

    List<NewsSource> findByType(NewsSource.SourceType type);

    @Query("SELECT ns FROM NewsSource ns WHERE ns.active = true AND " +
           "(ns.lastFetchAt IS NULL OR ns.lastFetchAt < :cutoffTime)")
    List<NewsSource> findSourcesReadyForFetch(LocalDateTime cutoffTime);

    @Query("SELECT ns FROM NewsSource ns WHERE ns.active = true AND ns.category.id = :categoryId")
    List<NewsSource> findActiveByCategoryId(Long categoryId);

    @Query("SELECT COUNT(ns) FROM NewsSource ns WHERE ns.active = true")
    long countActiveSources();
}