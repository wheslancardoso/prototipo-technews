-- ===============================
-- DADOS DE TESTE - TECHNEWS
-- ===============================

-- Inserindo categorias para testes (diferentes das de produção)
INSERT INTO categories (name, description, slug, color, active, created_at, updated_at) VALUES
('Test AI', 'Categoria de teste para IA', 'test-ai', '#FF6B6B', true, NOW(), NOW()),
('Test Dev', 'Categoria de teste para desenvolvimento', 'test-dev', '#4ECDC4', true, NOW(), NOW()),
('Test Security', 'Categoria de teste para segurança', 'test-security', '#45B7D1', true, NOW(), NOW());

-- Inserindo fontes para testes
INSERT INTO trusted_sources (name, domain_name, description, active, created_at, updated_at) VALUES
('Test Source', 'test.com', 'Fonte de teste', true, NOW(), NOW()),
('Test Tech', 'testtech.com', 'Portal de teste de tecnologia', true, NOW(), NOW());

-- Inserindo artigo de teste
INSERT INTO news_articles (title, description, url, source, status, category_id, published_at, created_at, updated_at) VALUES
('Test Article', 'Artigo de teste', 'http://test.com/article', 'Test Source', 'APPROVED', 1, NOW(), NOW(), NOW());