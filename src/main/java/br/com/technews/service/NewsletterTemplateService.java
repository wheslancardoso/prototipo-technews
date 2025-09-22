package br.com.technews.service;

import br.com.technews.entity.NewsletterTemplate;
import br.com.technews.repository.NewsletterTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço para gerenciamento de templates de newsletter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterTemplateService {

    private final NewsletterTemplateRepository templateRepository;

    /**
     * Buscar todos os templates ativos
     */
    public List<NewsletterTemplate> findAllActive() {
        return templateRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * Buscar template por ID
     */
    public Optional<NewsletterTemplate> findById(Long id) {
        return templateRepository.findById(id);
    }

    /**
     * Buscar template por chave
     */
    public Optional<NewsletterTemplate> findByKey(String templateKey) {
        return templateRepository.findByTemplateKeyAndIsActiveTrue(templateKey);
    }

    /**
     * Buscar template padrão
     */
    public NewsletterTemplate getDefaultTemplate() {
        return templateRepository.findByIsDefaultTrueAndIsActiveTrue()
                .orElseGet(this::createDefaultTemplate);
    }

    /**
     * Criar novo template
     */
    @Transactional
    public NewsletterTemplate createTemplate(NewsletterTemplate template) {
        // Verificar se já existe template com a mesma chave
        if (templateRepository.existsByTemplateKey(template.getTemplateKey())) {
            throw new IllegalArgumentException("Já existe um template com a chave: " + template.getTemplateKey());
        }

        // Se for marcado como padrão, desmarcar outros templates padrão
        if (template.getIsDefault()) {
            unsetDefaultTemplates();
        }

        return templateRepository.save(template);
    }

    /**
     * Atualizar template existente
     */
    @Transactional
    public NewsletterTemplate updateTemplate(Long id, NewsletterTemplate updatedTemplate) {
        NewsletterTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template não encontrado: " + id));

        // Verificar se a chave não está sendo usada por outro template
        if (!existingTemplate.getTemplateKey().equals(updatedTemplate.getTemplateKey()) &&
            templateRepository.existsByTemplateKey(updatedTemplate.getTemplateKey())) {
            throw new IllegalArgumentException("Já existe um template com a chave: " + updatedTemplate.getTemplateKey());
        }

        // Se for marcado como padrão, desmarcar outros templates padrão
        if (updatedTemplate.getIsDefault() && !existingTemplate.getIsDefault()) {
            unsetDefaultTemplates();
        }

        // Atualizar campos
        existingTemplate.setName(updatedTemplate.getName());
        existingTemplate.setDescription(updatedTemplate.getDescription());
        existingTemplate.setTemplateKey(updatedTemplate.getTemplateKey());
        existingTemplate.setHtmlContent(updatedTemplate.getHtmlContent());
        existingTemplate.setCssStyles(updatedTemplate.getCssStyles());
        existingTemplate.setConfiguration(updatedTemplate.getConfiguration());
        existingTemplate.setIsActive(updatedTemplate.getIsActive());
        existingTemplate.setIsDefault(updatedTemplate.getIsDefault());

        return templateRepository.save(existingTemplate);
    }

    /**
     * Desativar template
     */
    @Transactional
    public void deactivateTemplate(Long id) {
        NewsletterTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template não encontrado: " + id));

        if (template.getIsDefault()) {
            throw new IllegalArgumentException("Não é possível desativar o template padrão");
        }

        template.setIsActive(false);
        templateRepository.save(template);
    }

    /**
     * Definir template como padrão
     */
    @Transactional
    public void setAsDefault(Long id) {
        NewsletterTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template não encontrado: " + id));

        if (!template.getIsActive()) {
            throw new IllegalArgumentException("Não é possível definir um template inativo como padrão");
        }

        // Desmarcar outros templates padrão
        unsetDefaultTemplates();

        // Marcar como padrão
        template.setIsDefault(true);
        templateRepository.save(template);
    }

    /**
     * Buscar templates por nome
     */
    public List<NewsletterTemplate> searchByName(String name) {
        return templateRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name);
    }

    /**
     * Contar templates ativos
     */
    public long countActiveTemplates() {
        return templateRepository.countByIsActiveTrue();
    }

    /**
     * Inicializar templates padrão do sistema
     */
    @Transactional
    public void initializeDefaultTemplates() {
        // Verificar se já existem templates
        if (templateRepository.count() > 0) {
            return;
        }

        System.out.println("Inicializando templates padrão do sistema...");

        // Template Padrão
        NewsletterTemplate defaultTemplate = NewsletterTemplate.builder()
                .name("Template Padrão")
                .description("Layout clássico com artigos em lista")
                .templateKey("default")
                .isActive(true)
                .isDefault(true)
                .createdBy("system")
                .build();

        // Template Grid
        NewsletterTemplate gridTemplate = NewsletterTemplate.builder()
                .name("Template Grid")
                .description("Layout em grade com cards")
                .templateKey("grid")
                .isActive(true)
                .isDefault(false)
                .createdBy("system")
                .build();

        // Template Mobile
        NewsletterTemplate mobileTemplate = NewsletterTemplate.builder()
                .name("Template Mobile")
                .description("Otimizado para dispositivos móveis")
                .templateKey("mobile")
                .isActive(true)
                .isDefault(false)
                .createdBy("system")
                .build();

        templateRepository.saveAll(List.of(defaultTemplate, gridTemplate, mobileTemplate));
        System.out.println("Templates padrão inicializados com sucesso!");
    }

    /**
     * Criar template padrão se não existir
     */
    private NewsletterTemplate createDefaultTemplate() {
        System.out.println("Criando template padrão...");
        
        NewsletterTemplate defaultTemplate = NewsletterTemplate.builder()
                .name("Template Padrão")
                .description("Layout clássico com artigos em lista")
                .templateKey("default")
                .isActive(true)
                .isDefault(true)
                .createdBy("system")
                .build();

        return templateRepository.save(defaultTemplate);
    }

    /**
     * Desmarcar todos os templates como padrão
     */
    private void unsetDefaultTemplates() {
        List<NewsletterTemplate> defaultTemplates = templateRepository.findByIsDefaultTrueAndIsActiveTrue()
                .stream()
                .map(template -> {
                    template.setIsDefault(false);
                    return template;
                })
                .toList();

        if (!defaultTemplates.isEmpty()) {
            templateRepository.saveAll(defaultTemplates);
        }
    }
}