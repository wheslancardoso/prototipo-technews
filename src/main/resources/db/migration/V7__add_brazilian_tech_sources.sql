-- Migration V7: Adicionar fontes brasileiras de tecnologia
-- Adiciona principais sites brasileiros de tecnologia com feeds RSS

-- Inserção de fontes brasileiras de tecnologia
INSERT INTO news_sources (name, url, type, fetch_interval_minutes, max_articles_per_fetch) VALUES
-- Principais portais brasileiros de tecnologia
('TecMundo RSS', 'https://www.tecmundo.com.br/rss', 'RSS_FEED', 60, 15),
('Canaltech RSS', 'https://canaltech.com.br/rss/', 'RSS_FEED', 60, 15),
('Olhar Digital RSS', 'https://olhardigital.com.br/feed/', 'RSS_FEED', 60, 12),
('Tecnoblog RSS', 'https://tecnoblog.net/feed/', 'RSS_FEED', 90, 10),
('Gizmodo Brasil RSS', 'https://gizmodo.uol.com.br/feed/', 'RSS_FEED', 90, 10),

-- Fontes especializadas brasileiras
('Inovação Tecnológica RSS', 'https://www.inovacaotecnologica.com.br/boletim/rss.xml', 'RSS_FEED', 120, 8),
('Showmetech RSS', 'https://www.showmetech.com.br/feed/', 'RSS_FEED', 120, 8),
('Meio Bit RSS', 'https://meiobit.com/feed/', 'RSS_FEED', 120, 8),
('Adrenaline RSS', 'https://www.adrenaline.com.br/feed/', 'RSS_FEED', 120, 8),

-- Podcasts e conteúdo especializado
('Canaltech Podcast RSS', 'https://canaltech.com.br/rss/podcast/', 'RSS_FEED', 240, 5);

-- Comentários sobre as fontes brasileiras
COMMENT ON TABLE news_sources IS 'Fontes de notícias configuradas para coleta automática - incluindo fontes brasileiras de tecnologia';

-- Atualizar comentário para refletir a inclusão de fontes brasileiras
UPDATE news_sources SET 
    fetch_interval_minutes = 45,  -- Intervalo menor para fontes brasileiras mais ativas
    max_articles_per_fetch = 12   -- Ajuste para balancear volume
WHERE name IN ('TecMundo RSS', 'Canaltech RSS', 'Olhar Digital RSS');