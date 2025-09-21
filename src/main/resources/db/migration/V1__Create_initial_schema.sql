-- ===============================
-- MIGRAÇÃO INICIAL - TECHNEWS
-- Versão: V1
-- Descrição: Criação do schema inicial para PostgreSQL
-- ===============================

-- Tabela de Categorias
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    slug VARCHAR(60) NOT NULL UNIQUE,
    color VARCHAR(7),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Assinantes
CREATE TABLE subscribers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    verification_token VARCHAR(255),
    verification_token_expires_at TIMESTAMP,
    unsubscribe_token VARCHAR(255) UNIQUE,
    manage_token VARCHAR(255) UNIQUE,
    verified_at TIMESTAMP,
    reactivated_at TIMESTAMP,
    subscription_frequency VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    last_email_sent_at TIMESTAMP,
    email_count INTEGER NOT NULL DEFAULT 0,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP,
    subscription_ip VARCHAR(45),
    subscription_user_agent VARCHAR(500),
    
    CONSTRAINT chk_subscription_frequency 
        CHECK (subscription_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);

-- Tabela de Artigos de Notícias
CREATE TABLE news_articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    content TEXT,
    summary VARCHAR(500),
    url VARCHAR(1000) NOT NULL,
    image_url VARCHAR(1000),
    source VARCHAR(200),
    source_domain VARCHAR(100),
    author VARCHAR(200),
    category VARCHAR(50),
    published BOOLEAN DEFAULT false,
    published_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    category_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_news_articles_category 
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT chk_status 
        CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'PUBLISHED'))
);

-- Tabela de Fontes Confiáveis
CREATE TABLE trusted_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Relacionamento Assinante-Categoria (Many-to-Many)
CREATE TABLE subscriber_categories (
    subscriber_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    
    PRIMARY KEY (subscriber_id, category_id),
    CONSTRAINT fk_subscriber_categories_subscriber 
        FOREIGN KEY (subscriber_id) REFERENCES subscribers(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriber_categories_category 
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Índices para melhor performance
CREATE INDEX idx_subscribers_email ON subscribers(email);
CREATE INDEX idx_subscribers_active ON subscribers(active);
CREATE INDEX idx_subscribers_email_verified ON subscribers(email_verified);
CREATE INDEX idx_news_articles_status ON news_articles(status);
CREATE INDEX idx_news_articles_published_at ON news_articles(published_at);
CREATE INDEX idx_news_articles_category ON news_articles(category_id);
CREATE INDEX idx_trusted_sources_domain ON trusted_sources(domain_name);
CREATE INDEX idx_trusted_sources_active ON trusted_sources(active);

-- Comentários nas tabelas
COMMENT ON TABLE categories IS 'Categorias de notícias disponíveis no sistema';
COMMENT ON TABLE subscribers IS 'Assinantes da newsletter';
COMMENT ON TABLE news_articles IS 'Artigos de notícias coletados e curados';
COMMENT ON TABLE trusted_sources IS 'Fontes confiáveis para coleta de notícias';
COMMENT ON TABLE subscriber_categories IS 'Relacionamento entre assinantes e suas categorias de interesse';