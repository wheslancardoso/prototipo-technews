-- ===============================
-- MIGRAÇÃO DE DADOS INICIAIS - TECHNEWS
-- Versão: V2
-- Descrição: Inserção de dados iniciais para PostgreSQL
-- ===============================

-- Inserir categorias padrão
INSERT INTO categories (name, description, slug, active) VALUES
('Tecnologia', 'Notícias gerais sobre tecnologia e inovação', 'tecnologia', true),
('Inteligência Artificial', 'Notícias sobre IA, Machine Learning e automação', 'inteligencia-artificial', true),
('Programação', 'Notícias sobre desenvolvimento de software e linguagens de programação', 'programacao', true),
('Startups', 'Notícias sobre startups e empreendedorismo tecnológico', 'startups', true),
('Cibersegurança', 'Notícias sobre segurança digital e proteção de dados', 'ciberseguranca', true),
('Dispositivos Móveis', 'Notícias sobre smartphones, tablets e tecnologia móvel', 'dispositivos-moveis', true),
('Cloud Computing', 'Notícias sobre computação em nuvem e infraestrutura', 'cloud-computing', true),
('Blockchain', 'Notícias sobre blockchain, criptomoedas e Web3', 'blockchain', true),
('Gaming', 'Notícias sobre jogos e indústria de games', 'gaming', true),
('Ciência', 'Notícias sobre descobertas científicas e pesquisa', 'ciencia', true);

-- Inserir fontes confiáveis padrão
INSERT INTO trusted_sources (name, domain_name, description, active) VALUES
('TechCrunch', 'techcrunch.com', 'Portal líder em notícias de tecnologia e startups', true),
('The Verge', 'theverge.com', 'Notícias sobre tecnologia, ciência, arte e cultura', true),
('Ars Technica', 'arstechnica.com', 'Notícias técnicas detalhadas sobre tecnologia', true),
('Wired', 'wired.com', 'Revista sobre como a tecnologia afeta a cultura, economia e política', true),
('MIT Technology Review', 'technologyreview.com', 'Análises profundas sobre tecnologias emergentes', true),
('TecMundo', 'tecmundo.com.br', 'Portal brasileiro de tecnologia', true),
('Olhar Digital', 'olhardigital.com.br', 'Notícias de tecnologia em português', true),
('Canaltech', 'canaltech.com.br', 'Portal brasileiro sobre tecnologia e inovação', true),
('Gizmodo', 'gizmodo.com', 'Notícias sobre gadgets e tecnologia', true),
('Engadget', 'engadget.com', 'Notícias sobre eletrônicos de consumo', true);

-- Inserir usuário administrador padrão (senha será configurada via Spring Security)
-- Nota: Este é apenas um exemplo. Em produção, use um sistema de gerenciamento de usuários mais robusto

-- Inserir alguns artigos de exemplo (opcional - pode ser removido em produção)
INSERT INTO news_articles (title, description, url, source, status, category_id, published_at) VALUES
('Bem-vindo ao TechNews', 
 'Sistema de newsletter de tecnologia configurado com sucesso', 
 'http://localhost:8080', 
 'TechNews', 
 'APPROVED', 
 1, 
 CURRENT_TIMESTAMP);

-- Atualizar timestamps
UPDATE categories SET updated_at = CURRENT_TIMESTAMP;
UPDATE trusted_sources SET updated_at = CURRENT_TIMESTAMP;
UPDATE news_articles SET updated_at = CURRENT_TIMESTAMP;