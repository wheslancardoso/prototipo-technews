package br.com.technews.controller.admin;

import br.com.technews.entity.Category;
import br.com.technews.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * Controller para gerenciamento de categorias no painel administrativo
 */
@Controller
@RequestMapping("/admin/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    /**
     * Lista todas as categorias com paginação
     */
    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Category> categories = categoryService.findAll(pageable);
        
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categories.getTotalPages());
        model.addAttribute("totalElements", categories.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("search", search);
        
        return "admin/categories/list";
    }
    
    /**
     * Exibe formulário para criar nova categoria
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("isEdit", false);
        return "admin/categories/form";
    }
    
    /**
     * Processa criação de nova categoria
     */
    @PostMapping("/new")
    public String createCategory(@Valid @ModelAttribute Category category,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }
        
        try {
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Categoria '" + category.getName() + "' criada com sucesso!");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao criar categoria: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }
    }
    
    /**
     * Exibe formulário para editar categoria
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> category = categoryService.findById(id);
        
        if (category.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Categoria não encontrada!");
            return "redirect:/admin/categories";
        }
        
        model.addAttribute("category", category.get());
        model.addAttribute("isEdit", true);
        return "admin/categories/form";
    }
    
    /**
     * Processa atualização de categoria
     */
    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable Long id,
                               @Valid @ModelAttribute Category category,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }
        
        try {
            categoryService.update(id, category);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Categoria '" + category.getName() + "' atualizada com sucesso!");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao atualizar categoria: " + e.getMessage());
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }
    }
    
    /**
     * Ativa/desativa categoria
     */
    @PostMapping("/toggle/{id}")
    public String toggleCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.toggleActive(id);
            String status = category.getActive() ? "ativada" : "desativada";
            redirectAttributes.addFlashAttribute("successMessage", 
                "Categoria '" + category.getName() + "' " + status + " com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro ao alterar status da categoria: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
    
    /**
     * Remove categoria
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Category> category = categoryService.findById(id);
            if (category.isPresent()) {
                categoryService.delete(id);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Categoria '" + category.get().getName() + "' removida com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Categoria não encontrada!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro ao remover categoria: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
    
    /**
     * Visualiza detalhes da categoria
     */
    @GetMapping("/view/{id}")
    public String viewCategory(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> category = categoryService.findById(id);
        
        if (category.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Categoria não encontrada!");
            return "redirect:/admin/categories";
        }
        
        model.addAttribute("category", category.get());
        return "admin/categories/view";
    }
    
    /**
     * API endpoint para buscar categorias (para uso em AJAX)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public Object searchCategories(@RequestParam String term) {
        return categoryService.search(term);
    }
}