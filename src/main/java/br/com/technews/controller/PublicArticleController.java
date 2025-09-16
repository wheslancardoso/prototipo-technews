package br.com.technews.controller;

import br.com.technews.model.NewsArticle;
import br.com.technews.service.NewsArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/articles")
public class PublicArticleController {

    @Autowired
    private NewsArticleService newsArticleService;

    @GetMapping
    public String listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<NewsArticle> articles = newsArticleService.findPublishedArticles(pageable);
        
        // Filtrar por categoria se especificada
        if (category != null && !category.trim().isEmpty()) {
            List<NewsArticle> filteredArticles = newsArticleService.findByCategory(category.trim());
            // Para simplificar, vamos usar todos os artigos publicados por enquanto
            // Em uma implementação mais robusta, seria necessário criar métodos específicos no repository
        }
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("totalElements", articles.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("categories", getCategories());
        
        return "articles/index";
    }

    @GetMapping("/{id}")
    public String viewArticle(@PathVariable Long id, Model model) {
        Optional<NewsArticle> articleOpt = newsArticleService.findById(id);
        
        if (articleOpt.isPresent()) {
            NewsArticle article = articleOpt.get();
            
            // Só mostra artigos publicados
            if (!article.getPublished()) {
                return "redirect:/articles";
            }
            
            model.addAttribute("article", article);
            return "articles/view";
        } else {
            return "redirect:/articles";
        }
    }

    @GetMapping("/category/{category}")
    public String listArticlesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        List<NewsArticle> categoryArticles = newsArticleService.findByCategory(category);
        Page<NewsArticle> articles = newsArticleService.findPublishedArticles(pageable);
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("totalElements", articles.getTotalElements());
        model.addAttribute("category", category);
        model.addAttribute("categories", getCategories());
        
        return "articles/index";
    }

    private String[] getCategories() {
        return new String[]{
            "Tecnologia", "Inteligência Artificial", "Programação", 
            "Cibersegurança", "Startups", "Inovação", "Mobile", 
            "Web Development", "Data Science", "Cloud Computing"
        };
    }
}