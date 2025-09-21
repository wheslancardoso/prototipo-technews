package br.com.technews.controller;

import br.com.technews.entity.NewsArticle;
import br.com.technews.service.NewsArticleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controlador para visualização de artigos
 */
@Controller
@RequiredArgsConstructor
public class ArticleViewController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArticleViewController.class);

    private final NewsArticleService newsArticleService;

    /**
     * Exibe a página de detalhes de um artigo
     */
    @GetMapping("/article/{id}")
    public String viewArticle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<NewsArticle> articleOpt = newsArticleService.findById(id);
            
            if (articleOpt.isEmpty()) {
                log.warn("Artigo não encontrado com ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Artigo não encontrado.");
                return "redirect:/";
            }
            
            NewsArticle article = articleOpt.get();
            
            // Verifica se o artigo está publicado
            if (!"PUBLISHED".equals(article.getStatus())) {
                log.warn("Tentativa de acesso a artigo não publicado com ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Artigo não disponível.");
                return "redirect:/";
            }
            
            model.addAttribute("article", article);
            
            // Log para auditoria
            log.info("Artigo visualizado - ID: {}, Título: {}", id, article.getTitle());
            
            return "article-detail";
            
        } catch (Exception e) {
            log.error("Erro ao carregar artigo com ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao carregar o artigo.");
            return "redirect:/";
        }
    }

    /**
     * Exibe a página de detalhes de um artigo por slug (URL amigável)
     */
    @GetMapping("/article/slug/{slug}")
    public String viewArticleBySlug(@PathVariable String slug, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Implementação futura para busca por slug
            // Por enquanto, redireciona para a home
            log.info("Tentativa de acesso por slug: {}", slug);
            redirectAttributes.addFlashAttribute("info", "Funcionalidade de URL amigável em desenvolvimento.");
            return "redirect:/";
            
        } catch (Exception e) {
            log.error("Erro ao carregar artigo por slug: {}", slug, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao carregar o artigo.");
            return "redirect:/";
        }
    }
}