package br.com.technews.dto;

/**
 * DTO para estatísticas de newsletter
 */
public class NewsletterStats {
    private final long totalArticles;
    private final long totalSubscribers;
    private final long articlesToday;
    private final long totalViews;

    public NewsletterStats(long totalArticles, long totalSubscribers, long articlesToday, long totalViews) {
        this.totalArticles = totalArticles;
        this.totalSubscribers = totalSubscribers;
        this.articlesToday = articlesToday;
        this.totalViews = totalViews;
    }

    public long getTotalArticles() {
        return totalArticles;
    }

    public long getTotalSubscribers() {
        return totalSubscribers;
    }

    public long getArticlesToday() {
        return articlesToday;
    }

    public long getTotalViews() {
        return totalViews;
    }
}