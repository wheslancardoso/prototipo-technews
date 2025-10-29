package br.com.technews.controller;

import br.com.technews.entity.NewsArticle;
import br.com.technews.service.NewsArticleService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PublicArticleController {

    private final NewsArticleService newsArticleService;

    @GetMapping
    public String listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String author,
            Model model) {
        
        // Configurar ordenação
        Sort sort = createSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<NewsArticle> articles;
        
        // Aplicar filtros combinados
        articles = newsArticleService.searchArticlesWithFilters(
            search, category, dateFrom, dateTo, author, pageable);
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("totalElements", articles.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("author", author);
        model.addAttribute("categories", getCategories());
        model.addAttribute("authors", getAuthors());
        
        return "articles/index";
    }
    
    private Sort createSort(String sortBy, String sortDir) {
        String field = "publishedAt"; // default
        Sort.Direction direction = Sort.Direction.DESC; // default
        
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "title":
                    field = "title";
                    break;
                case "author":
                    field = "author";
                    break;
                case "category":
                    field = "category";
                    break;
                case "date":
                default:
                    field = "publishedAt";
                    break;
            }
        }
        
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }
        
        return Sort.by(direction, field);
    }
    
    private List<String> getAuthors() {
        return newsArticleService.getDistinctAuthors();
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
        Page<NewsArticle> articles = newsArticleService.findPublishedArticlesByCategory(category, pageable);
        
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