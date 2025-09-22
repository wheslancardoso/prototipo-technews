-- ===============================
-- DADOS INICIAIS - TECHNEWS
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

-- Inserindo artigos de exemplo para demonstração
INSERT INTO news_articles (title, description, content, url, source_domain, author, category, status, published, published_at, created_at, updated_at) VALUES
('Revolução da Inteligência Artificial em 2024', 
 'As principais tendências e avanços em IA que estão moldando o futuro da tecnologia', 
 'A inteligência artificial continua a evoluir rapidamente, com novos modelos de linguagem e aplicações práticas surgindo constantemente. Este artigo explora as principais tendências para 2024.',
 'https://techcrunch.com/ai-revolution-2024', 
 'techcrunch.com', 
 'João Silva', 
 'Inteligência Artificial', 
 'PUBLISHED', 
 true, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP),

('Novas Linguagens de Programação Ganham Destaque', 
 'Rust, Go e outras linguagens modernas estão transformando o desenvolvimento de software', 
 'O cenário de desenvolvimento está em constante evolução, com novas linguagens oferecendo melhor performance, segurança e produtividade para os desenvolvedores.',
 'https://arstechnica.com/programming-languages-2024', 
 'arstechnica.com', 
 'Maria Santos', 
 'Desenvolvimento', 
 'PUBLISHED', 
 true, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP),

('Cibersegurança: Principais Ameaças de 2024', 
 'Análise das vulnerabilidades mais críticas e como se proteger contra ataques modernos', 
 'Com o aumento dos ataques cibernéticos, é essencial entender as principais ameaças e implementar medidas de proteção adequadas.',
 'https://wired.com/cybersecurity-threats-2024', 
 'wired.com', 
 'Carlos Oliveira', 
 'Cibersegurança', 
 'PUBLISHED', 
 true, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP),

('Startups Brasileiras Recebem Investimento Record', 
 'Ecossistema de inovação nacional atrai bilhões em investimentos estrangeiros', 
 'O Brasil está se consolidando como um hub de inovação, com startups locais recebendo aportes significativos de investidores internacionais.',
 'https://venturebeat.com/brazilian-startups-investment', 
 'venturebeat.com', 
 'Ana Costa', 
 'Startups', 
 'PUBLISHED', 
 true, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP),

('Novos Processadores Prometem Revolução em Performance', 
 'Chips de nova geração oferecem ganhos significativos em eficiência energética', 
 'A indústria de semicondutores continua inovando, com novos processadores que prometem transformar a computação moderna.',
 'https://engadget.com/new-processors-2024', 
 'engadget.com', 
 'Pedro Lima', 
 'Hardware', 
 'PUBLISHED', 
 true, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP, 
 CURRENT_TIMESTAMP);

-- Inserindo configurações do sistema
INSERT INTO system_settings (setting_key, setting_value, description, created_at, updated_at) VALUES
('site_name', 'TechNews', 'Nome do site', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('site_description', 'Portal de notícias sobre tecnologia, inovação e startups', 'Descrição do site', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('articles_per_page', '10', 'Número de artigos por página', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('newsletter_enabled', 'true', 'Newsletter habilitada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auto_publish_enabled', 'false', 'Publicação automática habilitada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===============================
-- DADOS DE TESTE - NEWSLETTERS
-- ===============================

-- Inserir dados de teste para newsletters
INSERT INTO newsletters (title, description, newsletter_date, slug, published, views, created_at, updated_at, published_at) VALUES
('TechNews - Edição de 20/09/2024', 'As principais notícias de tecnologia, inovação e startups do dia 20 de setembro de 2024.', '2024-09-20', '20-09-2024', true, 150, '2024-09-20 08:00:00', '2024-09-20 08:00:00', '2024-09-20 08:00:00'),
('TechNews - Edição de 19/09/2024', 'As principais notícias de tecnologia, inovação e startups do dia 19 de setembro de 2024.', '2024-09-19', '19-09-2024', true, 120, '2024-09-19 08:00:00', '2024-09-19 08:00:00', '2024-09-19 08:00:00'),
('TechNews - Edição de 18/09/2024', 'As principais notícias de tecnologia, inovação e startups do dia 18 de setembro de 2024.', '2024-09-18', '18-09-2024', true, 95, '2024-09-18 08:00:00', '2024-09-18 08:00:00', '2024-09-18 08:00:00'),
('TechNews - Edição de 17/09/2024', 'As principais notícias de tecnologia, inovação e startups do dia 17 de setembro de 2024.', '2024-09-17', '17-09-2024', true, 80, '2024-09-17 08:00:00', '2024-09-17 08:00:00', '2024-09-17 08:00:00'),
('TechNews - Edição de 16/09/2024', 'As principais notícias de tecnologia, inovação e startups do dia 16 de setembro de 2024.', '2024-09-16', '16-09-2024', true, 110, '2024-09-16 08:00:00', '2024-09-16 08:00:00', '2024-09-16 08:00:00');

-- Inserir artigos de teste para o dia 20/09/2024
INSERT INTO news_articles (title, content, summary, slug, category, author, published, views, created_at, updated_at, published_at) VALUES
('OpenAI lança GPT-5 com capacidades revolucionárias', 'A OpenAI anunciou hoje o lançamento do GPT-5, sua mais nova versão do modelo de linguagem que promete revolucionar a inteligência artificial com capacidades aprimoradas de raciocínio, compreensão multimodal e geração de código. O novo modelo demonstra melhorias significativas em tarefas complexas e oferece maior precisão em respostas técnicas.', 'OpenAI apresenta GPT-5 com melhorias significativas em raciocínio e capacidades multimodais.', 'openai-lanca-gpt-5-capacidades-revolucionarias', 'Inteligência Artificial', 'Tech Reporter', true, 250, '2024-09-20 09:00:00', '2024-09-20 09:00:00', '2024-09-20 09:00:00'),
('Meta anuncia novo headset VR com tecnologia avançada', 'A Meta revelou seu mais novo headset de realidade virtual, prometendo uma experiência imersiva sem precedentes com resolução 4K por olho, tracking de movimento aprimorado e redução significativa no motion sickness. O dispositivo marca um novo capítulo na evolução da realidade virtual.', 'Meta apresenta headset VR de nova geração com resolução 4K e tracking aprimorado.', 'meta-anuncia-novo-headset-vr-tecnologia-avancada', 'Realidade Virtual', 'VR Specialist', true, 180, '2024-09-20 10:30:00', '2024-09-20 10:30:00', '2024-09-20 10:30:00'),
('Tesla revela avanços em carros autônomos', 'A Tesla compartilhou novos dados sobre o progresso de seus veículos autônomos, mostrando melhorias significativas na segurança e eficiência. Os dados revelam uma redução de 40% em acidentes e maior precisão na navegação urbana complexa.', 'Tesla demonstra progressos em direção autônoma com redução de 40% em acidentes.', 'tesla-revela-avancos-carros-autonomos', 'Automobilismo', 'Auto Tech', true, 200, '2024-09-20 11:15:00', '2024-09-20 11:15:00', '2024-09-20 11:15:00'),
('Google apresenta chip quântico de próxima geração', 'O Google anunciou seu mais novo processador quântico, prometendo resolver problemas complexos em segundos que levariam anos para computadores tradicionais. O chip representa um marco na computação quântica comercial.', 'Google desenvolve chip quântico capaz de realizar cálculos 1000x mais rápidos.', 'google-apresenta-chip-quantico-proxima-geracao', 'Computação Quântica', 'Quantum Expert', true, 160, '2024-09-20 14:00:00', '2024-09-20 14:00:00', '2024-09-20 14:00:00'),
('Apple anuncia iPhone 16 com IA integrada', 'A Apple revelou o iPhone 16, que vem com recursos de inteligência artificial integrados nativamente, processamento local avançado e novos recursos de câmera alimentados por IA. O dispositivo marca a entrada definitiva da Apple na era da IA móvel.', 'iPhone 16 traz IA nativa com processamento local e novos recursos de câmera.', 'apple-anuncia-iphone-16-ia-integrada', 'Smartphones', 'Mobile Reporter', true, 300, '2024-09-20 16:30:00', '2024-09-20 16:30:00', '2024-09-20 16:30:00');

-- Inserir artigos de teste para o dia 19/09/2024
INSERT INTO news_articles (title, content, summary, slug, category, author, published, views, created_at, updated_at, published_at) VALUES
('Microsoft lança Azure AI Studio', 'A Microsoft apresentou o Azure AI Studio, uma plataforma completa para desenvolvimento de aplicações de IA empresarial, oferecendo ferramentas integradas para treino, deploy e monitoramento de modelos de machine learning.', 'Microsoft disponibiliza plataforma unificada para desenvolvimento de IA empresarial.', 'microsoft-lanca-azure-ai-studio', 'Cloud Computing', 'Cloud Expert', true, 140, '2024-09-19 09:30:00', '2024-09-19 09:30:00', '2024-09-19 09:30:00'),
('Amazon investe US$ 4 bilhões em startup de IA', 'A Amazon anunciou um investimento massivo em uma startup especializada em inteligência artificial generativa, focando no desenvolvimento de assistentes virtuais mais avançados e naturais para uso empresarial.', 'Amazon faz maior investimento em IA com foco em assistentes virtuais avançados.', 'amazon-investe-4-bilhoes-startup-ia', 'Investimentos', 'Finance Tech', true, 190, '2024-09-19 11:00:00', '2024-09-19 11:00:00', '2024-09-19 11:00:00'),
('SpaceX completa missão histórica para Marte', 'A SpaceX realizou com sucesso o lançamento de sua missão não tripulada para Marte, utilizando tecnologia de foguetes reutilizáveis e marcando um novo marco na exploração espacial comercial.', 'SpaceX marca novo marco na exploração espacial com tecnologia reutilizável.', 'spacex-completa-missao-historica-marte', 'Espaço', 'Space Reporter', true, 220, '2024-09-19 13:45:00', '2024-09-19 13:45:00', '2024-09-19 13:45:00');

-- Inserir artigos de teste para o dia 18/09/2024
INSERT INTO news_articles (title, content, summary, slug, category, author, published, views, created_at, updated_at, published_at) VALUES
('NVIDIA anuncia nova arquitetura de GPU', 'A NVIDIA revelou sua nova arquitetura de GPU focada em inteligência artificial e gaming, oferecendo 50% mais performance em workloads de IA e ray tracing aprimorado para jogos de nova geração.', 'NVIDIA apresenta GPUs de nova geração com 50% mais performance em IA.', 'nvidia-anuncia-nova-arquitetura-gpu', 'Hardware', 'Hardware Specialist', true, 170, '2024-09-18 10:00:00', '2024-09-18 10:00:00', '2024-09-18 10:00:00'),
('Startup brasileira recebe investimento milionário', 'Uma startup brasileira de fintech recebeu aporte de R$ 50 milhões para expansão nacional e desenvolvimento de soluções inovadoras em pagamentos digitais e blockchain.', 'Fintech nacional levanta capital para revolucionar pagamentos digitais.', 'startup-brasileira-recebe-investimento-milionario', 'Startups', 'Startup Reporter', true, 130, '2024-09-18 14:20:00', '2024-09-18 14:20:00', '2024-09-18 14:20:00');

-- Relacionar artigos com newsletters (assumindo IDs sequenciais)
-- Newsletter do dia 20/09/2024 com artigos do mesmo dia
INSERT INTO newsletter_articles (newsletter_id, article_id) 
SELECT n.id, a.id 
FROM newsletters n, news_articles a 
WHERE n.slug = '20-09-2024' 
AND a.slug IN ('openai-lanca-gpt-5-capacidades-revolucionarias', 'meta-anuncia-novo-headset-vr-tecnologia-avancada', 'tesla-revela-avancos-carros-autonomos', 'google-apresenta-chip-quantico-proxima-geracao', 'apple-anuncia-iphone-16-ia-integrada');

-- Newsletter do dia 19/09/2024 com artigos do mesmo dia
INSERT INTO newsletter_articles (newsletter_id, article_id) 
SELECT n.id, a.id 
FROM newsletters n, news_articles a 
WHERE n.slug = '19-09-2024' 
AND a.slug IN ('microsoft-lanca-azure-ai-studio', 'amazon-investe-4-bilhoes-startup-ia', 'spacex-completa-missao-historica-marte');

-- Newsletter do dia 18/09/2024 com artigos do mesmo dia
INSERT INTO newsletter_articles (newsletter_id, article_id) 
SELECT n.id, a.id 
FROM newsletters n, news_articles a 
WHERE n.slug = '18-09-2024' 
AND a.slug IN ('nvidia-anuncia-nova-arquitetura-gpu', 'startup-brasileira-recebe-investimento-milionario');