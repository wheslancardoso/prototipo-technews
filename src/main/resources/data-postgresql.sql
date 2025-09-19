-- ===============================
-- DADOS INICIAIS - TECHNEWS (PostgreSQL)
-- ===============================

-- Inserindo categorias iniciais
INSERT INTO categories (name, description, slug, color, active, created_at, updated_at) VALUES
('Inteligência Artificial', 'Notícias sobre IA, Machine Learning e Deep Learning', 'inteligencia-artificial', '#FF6B6B', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Desenvolvimento', 'Linguagens de programação, frameworks e ferramentas', 'desenvolvimento', '#4ECDC4', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cibersegurança', 'Segurança digital, vulnerabilidades e proteção', 'ciberseguranca', '#45B7D1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Startups', 'Empreendedorismo, investimentos e inovação', 'startups', '#96CEB4', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hardware', 'Processadores, placas de vídeo e componentes', 'hardware', '#FFEAA7', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mobile', 'Aplicativos móveis, iOS e Android', 'mobile', '#DDA0DD', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cloud Computing', 'Computação em nuvem, AWS, Azure e Google Cloud', 'cloud-computing', '#74B9FF', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Blockchain', 'Criptomoedas, NFTs e tecnologia blockchain', 'blockchain', '#FD79A8', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserindo fontes confiáveis
INSERT INTO trusted_sources (name, domain_name, description, active, created_at, updated_at) VALUES
('TechCrunch', 'techcrunch.com', 'Portal líder em notícias de tecnologia e startups', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ars Technica', 'arstechnica.com', 'Análises técnicas profundas e notícias de tecnologia', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('The Verge', 'theverge.com', 'Tecnologia, ciência, arte e cultura', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Wired', 'wired.com', 'Como a tecnologia está mudando o mundo', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MIT Technology Review', 'technologyreview.com', 'Insights sobre tecnologias emergentes', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('IEEE Spectrum', 'spectrum.ieee.org', 'Engenharia, tecnologia e ciência aplicada', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ZDNet', 'zdnet.com', 'Notícias de tecnologia para profissionais', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Engadget', 'engadget.com', 'Gadgets, games e entretenimento digital', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('VentureBeat', 'venturebeat.com', 'Transformação digital e inovação', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hacker News', 'news.ycombinator.com', 'Comunidade de desenvolvedores e empreendedores', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);