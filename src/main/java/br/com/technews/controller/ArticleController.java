package br.com.technews.controller;

import br.com.technews.model.NewsArticle;
import br.com.technews.model.ArticleStatus;
import br.com.technews.service.NewsArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/articles")
public class ArticleController {

    @Autowired
    private NewsArticleService newsArticleService;

    @GetMapping
    public String listArticles(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsArticle> articles = newsArticleService.findAll(pageable);
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("totalElements", articles.getTotalElements());
        
        return "admin/articles/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("article", new NewsArticle());
        model.addAttribute("categories", getCategories());
        return "admin/articles/form";
    }

    @PostMapping
    public String createArticle(@ModelAttribute NewsArticle article, 
                               RedirectAttributes redirectAttributes) {
        try {
            newsArticleService.create(article);
            redirectAttributes.addFlashAttribute("successMessage", "Artigo criado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar artigo: " + e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    @GetMapping("/{id}")
    public String viewArticle(@PathVariable Long id, Model model) {
        Optional<NewsArticle> article = newsArticleService.findById(id);
        if (article.isPresent()) {
            model.addAttribute("article", article.get());
            return "admin/articles/view";
        }
        return "redirect:/admin/articles";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<NewsArticle> article = newsArticleService.findById(id);
        if (article.isPresent()) {
            model.addAttribute("article", article.get());
            model.addAttribute("categories", getCategories());
            return "admin/articles/form";
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}")
    public String updateArticle(@PathVariable Long id, 
                               @ModelAttribute NewsArticle article,
                               RedirectAttributes redirectAttributes) {
        try {
            newsArticleService.update(id, article);
            redirectAttributes.addFlashAttribute("successMessage", "Artigo atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar artigo: " + e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/publish")
    public String publishArticle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            newsArticleService.publish(id);
            redirectAttributes.addFlashAttribute("successMessage", "Artigo publicado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao publicar artigo: " + e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublishArticle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            newsArticleService.unpublish(id);
            redirectAttributes.addFlashAttribute("successMessage", "Artigo despublicado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao despublicar artigo: " + e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/delete")
    public String deleteArticle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            newsArticleService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Artigo excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao excluir artigo: " + e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    private String[] getCategories() {
        return new String[]{
            "Tecnologia", "Inteligência Artificial", "Programação", 
            "Cibersegurança", "Startups", "Inovação", "Mobile", 
            "Web Development", "Data Science", "Cloud Computing"
        };
    }
}