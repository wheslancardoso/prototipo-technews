-- ===============================
-- DADOS INICIAIS - TECHNEWS
-- ===============================

-- Inserindo categorias iniciais
INSERT INTO categories (name, description, slug, color, active, created_at, updated_at) VALUES
('Inteligência Artificial', 'Notícias sobre IA, Machine Learning e Deep Learning', 'inteligencia-artificial', '#FF6B6B', true, NOW(), NOW()),
('Desenvolvimento', 'Linguagens de programação, frameworks e ferramentas', 'desenvolvimento', '#4ECDC4', true, NOW(), NOW()),
('Cibersegurança', 'Segurança digital, vulnerabilidades e proteção', 'ciberseguranca', '#45B7D1', true, NOW(), NOW()),
('Startups', 'Empreendedorismo, investimentos e inovação', 'startups', '#96CEB4', true, NOW(), NOW()),
('Hardware', 'Processadores, placas de vídeo e componentes', 'hardware', '#FFEAA7', true, NOW(), NOW()),
('Mobile', 'Aplicativos móveis, iOS e Android', 'mobile', '#DDA0DD', true, NOW(), NOW()),
('Cloud Computing', 'Computação em nuvem, AWS, Azure e Google Cloud', 'cloud-computing', '#74B9FF', true, NOW(), NOW()),
('Blockchain', 'Criptomoedas, NFTs e tecnologia blockchain', 'blockchain', '#FD79A8', true, NOW(), NOW());

-- Inserindo fontes confiáveis
INSERT INTO trusted_sources (name, domain_name, description, active, created_at, updated_at) VALUES
('TechCrunch', 'techcrunch.com', 'Portal líder em notícias de tecnologia e startups', true, NOW(), NOW()),
('Ars Technica', 'arstechnica.com', 'Análises técnicas profundas e notícias de tecnologia', true, NOW(), NOW()),
('The Verge', 'theverge.com', 'Tecnologia, ciência, arte e cultura', true, NOW(), NOW()),
('Wired', 'wired.com', 'Como a tecnologia está mudando o mundo', true, NOW(), NOW()),
('MIT Technology Review', 'technologyreview.com', 'Insights sobre tecnologias emergentes', true, NOW(), NOW()),
('IEEE Spectrum', 'spectrum.ieee.org', 'Engenharia, tecnologia e ciência aplicada', true, NOW(), NOW()),
('ZDNet', 'zdnet.com', 'Notícias de tecnologia para profissionais', true, NOW(), NOW()),
('Engadget', 'engadget.com', 'Gadgets, games e entretenimento digital', true, NOW(), NOW()),
('VentureBeat', 'venturebeat.com', 'Transformação digital e inovação', true, NOW(), NOW()),
('Hacker News', 'news.ycombinator.com', 'Comunidade de desenvolvedores e empreendedores', true, NOW(), NOW());