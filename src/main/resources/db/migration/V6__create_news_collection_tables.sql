-- Criação das tabelas para o sistema de coleta automática de notícias

-- Tabela de fontes de notícias (RSS, APIs, etc.)
CREATE TABLE news_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('RSS_FEED', 'API', 'WEB_SCRAPING')),
    category_id BIGINT REFERENCES categories(id),
    active BOOLEAN NOT NULL DEFAULT true,
    last_fetch_at TIMESTAMP,
    fetch_interval_minutes INTEGER NOT NULL DEFAULT 60,
    max_articles_per_fetch INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de notícias coletadas automaticamente
CREATE TABLE collected_news (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    original_url VARCHAR(1000) NOT NULL UNIQUE,
    image_url VARCHAR(1000),
    published_at TIMESTAMP,
    source_id BIGINT NOT NULL REFERENCES news_sources(id),
    category_id BIGINT REFERENCES categories(id),
    content_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED')),
    quality_score DECIMAL(3,1) DEFAULT 0.0,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização de consultas
CREATE INDEX idx_collected_news_url ON collected_news(original_url);
CREATE INDEX idx_collected_news_hash ON collected_news(content_hash);
CREATE INDEX idx_collected_news_published ON collected_news(published_at);
CREATE INDEX idx_collected_news_status ON collected_news(status);
CREATE INDEX idx_collected_news_quality ON collected_news(quality_score);
CREATE INDEX idx_collected_news_source ON collected_news(source_id);
CREATE INDEX idx_collected_news_category ON collected_news(category_id);

CREATE INDEX idx_news_sources_active ON news_sources(active);
CREATE INDEX idx_news_sources_type ON news_sources(type);
CREATE INDEX idx_news_sources_category ON news_sources(category_id);

-- Trigger para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_news_sources_updated_at 
    BEFORE UPDATE ON news_sources 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_collected_news_updated_at 
    BEFORE UPDATE ON collected_news 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Inserção de fontes de notícias iniciais
INSERT INTO news_sources (name, url, type, fetch_interval_minutes, max_articles_per_fetch) VALUES
('TechCrunch RSS', 'https://techcrunch.com/feed/', 'RSS_FEED', 60, 15),
('Ars Technica RSS', 'https://feeds.arstechnica.com/arstechnica/index', 'RSS_FEED', 90, 10),
('The Verge RSS', 'https://www.theverge.com/rss/index.xml', 'RSS_FEED', 60, 12),
('Wired RSS', 'https://www.wired.com/feed/rss', 'RSS_FEED', 120, 8),
('MIT Technology Review RSS', 'https://www.technologyreview.com/feed/', 'RSS_FEED', 180, 5),
('Hacker News RSS', 'https://hnrss.org/frontpage', 'RSS_FEED', 30, 20),
('GitHub Blog RSS', 'https://github.blog/feed/', 'RSS_FEED', 240, 5),
('Stack Overflow Blog RSS', 'https://stackoverflow.blog/feed/', 'RSS_FEED', 360, 3);

-- Comentários nas tabelas
COMMENT ON TABLE news_sources IS 'Fontes de notícias configuradas para coleta automática';
COMMENT ON TABLE collected_news IS 'Notícias coletadas automaticamente das fontes configuradas';

COMMENT ON COLUMN news_sources.fetch_interval_minutes IS 'Intervalo em minutos entre coletas desta fonte';
COMMENT ON COLUMN news_sources.max_articles_per_fetch IS 'Máximo de artigos a coletar por vez desta fonte';
COMMENT ON COLUMN collected_news.content_hash IS 'Hash SHA-256 do conteúdo para deduplicação';
COMMENT ON COLUMN collected_news.quality_score IS 'Score de qualidade de 0.0 a 10.0 calculado automaticamente';
COMMENT ON COLUMN collected_news.status IS 'Status do processamento: PENDING, APPROVED, REJECTED, PUBLISHED';