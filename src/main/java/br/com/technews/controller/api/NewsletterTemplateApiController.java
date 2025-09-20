package br.com.technews.controller.api;

import br.com.technews.entity.NewsletterTemplate;
import br.com.technews.service.NewsletterTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API REST para gerenciamento de templates de newsletter
 */
@RestController
@RequestMapping("/api/newsletter/templates")
@RequiredArgsConstructor
@Slf4j
public class NewsletterTemplateApiController {

    private final NewsletterTemplateService templateService;

    /**
     * Listar todos os templates ativos
     */
    @GetMapping
    public ResponseEntity<List<NewsletterTemplate>> getAllTemplates() {
        try {
            List<NewsletterTemplate> templates = templateService.findAllActive();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Erro ao buscar templates: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Buscar template por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NewsletterTemplate> getTemplateById(@PathVariable Long id) {
        try {
            Optional<NewsletterTemplate> template = templateService.findById(id);
            return template.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar template por ID: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Buscar template por chave
     */
    @GetMapping("/key/{templateKey}")
    public ResponseEntity<NewsletterTemplate> getTemplateByKey(@PathVariable String templateKey) {
        try {
            Optional<NewsletterTemplate> template = templateService.findByKey(templateKey);
            return template.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar template por chave: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Buscar template padrão
     */
    @GetMapping("/default")
    public ResponseEntity<NewsletterTemplate> getDefaultTemplate() {
        try {
            NewsletterTemplate template = templateService.getDefaultTemplate();
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("Erro ao buscar template padrão: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Criar novo template
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody CreateTemplateRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar dados obrigatórios
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Nome do template é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getTemplateKey() == null || request.getTemplateKey().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Chave do template é obrigatória");
                return ResponseEntity.badRequest().body(response);
            }

            // Criar template
            NewsletterTemplate template = NewsletterTemplate.builder()
                    .name(request.getName().trim())
                    .description(request.getDescription())
                    .templateKey(request.getTemplateKey().trim().toLowerCase())
                    .htmlContent(request.getHtmlContent())
                    .cssStyles(request.getCssStyles())
                    .configuration(request.getConfiguration())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                    .createdBy("admin") // TODO: Pegar do contexto de segurança
                    .build();

            NewsletterTemplate savedTemplate = templateService.createTemplate(template);

            response.put("success", true);
            response.put("message", "Template criado com sucesso");
            response.put("template", savedTemplate);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Erro ao criar template: ", e);
            response.put("success", false);
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Atualizar template existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(@PathVariable Long id, @RequestBody UpdateTemplateRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar dados obrigatórios
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Nome do template é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getTemplateKey() == null || request.getTemplateKey().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Chave do template é obrigatória");
                return ResponseEntity.badRequest().body(response);
            }

            // Criar objeto para atualização
            NewsletterTemplate updatedTemplate = NewsletterTemplate.builder()
                    .name(request.getName().trim())
                    .description(request.getDescription())
                    .templateKey(request.getTemplateKey().trim().toLowerCase())
                    .htmlContent(request.getHtmlContent())
                    .cssStyles(request.getCssStyles())
                    .configuration(request.getConfiguration())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                    .build();

            NewsletterTemplate savedTemplate = templateService.updateTemplate(id, updatedTemplate);

            response.put("success", true);
            response.put("message", "Template atualizado com sucesso");
            response.put("template", savedTemplate);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Erro ao atualizar template: ", e);
            response.put("success", false);
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Desativar template
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deactivateTemplate(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            templateService.deactivateTemplate(id);

            response.put("success", true);
            response.put("message", "Template desativado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Erro ao desativar template: ", e);
            response.put("success", false);
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Definir template como padrão
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<Map<String, Object>> setAsDefault(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            templateService.setAsDefault(id);

            response.put("success", true);
            response.put("message", "Template definido como padrão com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Erro ao definir template como padrão: ", e);
            response.put("success", false);
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Buscar templates por nome
     */
    @GetMapping("/search")
    public ResponseEntity<List<NewsletterTemplate>> searchTemplates(@RequestParam String name) {
        try {
            List<NewsletterTemplate> templates = templateService.searchByName(name);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Erro ao buscar templates por nome: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Classes de Request
    public static class CreateTemplateRequest {
        private String name;
        private String description;
        private String templateKey;
        private String htmlContent;
        private String cssStyles;
        private String configuration;
        private Boolean isActive;
        private Boolean isDefault;

        // Getters e Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTemplateKey() { return templateKey; }
        public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
        
        public String getHtmlContent() { return htmlContent; }
        public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
        
        public String getCssStyles() { return cssStyles; }
        public void setCssStyles(String cssStyles) { this.cssStyles = cssStyles; }
        
        public String getConfiguration() { return configuration; }
        public void setConfiguration(String configuration) { this.configuration = configuration; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }

    public static class UpdateTemplateRequest extends CreateTemplateRequest {
        // Herda todos os campos da classe pai
    }
}