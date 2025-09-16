package br.com.technews.controller.admin;

import br.com.technews.model.TrustedSource;
import br.com.technews.service.TrustedSourceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gerenciamento administrativo de fontes confiáveis
 */
@Controller
@RequestMapping("/admin/trusted-sources")
public class TrustedSourceController {

    @Autowired
    private TrustedSourceService trustedSourceService;

    /**
     * Lista todas as fontes confiáveis com paginação e filtros
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(defaultValue = "name") String sort,
                      @RequestParam(defaultValue = "asc") String direction,
                      @RequestParam(required = false) String search,
                      @RequestParam(required = false) Boolean active,
                      Model model) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<TrustedSource> sourcesPage;
        
        if (search != null && !search.trim().isEmpty()) {
            if (active != null) {
                sourcesPage = trustedSourceService.findBySearchTermAndActive(search, active, pageable);
            } else {
                sourcesPage = trustedSourceService.findBySearchTerm(search, pageable);
            }
        } else if (active != null) {
            sourcesPage = trustedSourceService.findByActive(active, pageable);
        } else {
            sourcesPage = trustedSourceService.findAll(pageable);
        }

        model.addAttribute("sourcesPage", sourcesPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDirection", direction);
        model.addAttribute("search", search);
        model.addAttribute("activeFilter", active);
        
        // Estatísticas
        model.addAttribute("totalSources", trustedSourceService.count());
        model.addAttribute("activeSources", trustedSourceService.countActive());
        model.addAttribute("inactiveSources", trustedSourceService.countInactive());

        return "admin/trusted-sources/list";
    }

    /**
     * Exibe formulário para criar nova fonte confiável
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("trustedSource", new TrustedSource());
        model.addAttribute("isEdit", false);
        return "admin/trusted-sources/form";
    }

    /**
     * Processa criação de nova fonte confiável
     */
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute TrustedSource trustedSource,
                        BindingResult result,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/trusted-sources/form";
        }

        try {
            TrustedSource savedSource = trustedSourceService.save(trustedSource);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Fonte confiável '" + savedSource.getName() + "' criada com sucesso!");
            return "redirect:/admin/trusted-sources";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/trusted-sources/form";
        }
    }

    /**
     * Exibe formulário para editar fonte confiável
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<TrustedSource> sourceOpt = trustedSourceService.findById(id);
        
        if (sourceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fonte confiável não encontrada!");
            return "redirect:/admin/trusted-sources";
        }

        model.addAttribute("trustedSource", sourceOpt.get());
        model.addAttribute("isEdit", true);
        return "admin/trusted-sources/form";
    }

    /**
     * Processa atualização de fonte confiável
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute TrustedSource trustedSource,
                        BindingResult result,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "admin/trusted-sources/form";
        }

        try {
            TrustedSource updatedSource = trustedSourceService.update(id, trustedSource);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Fonte confiável '" + updatedSource.getName() + "' atualizada com sucesso!");
            return "redirect:/admin/trusted-sources";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "admin/trusted-sources/form";
        }
    }

    /**
     * Visualiza detalhes de uma fonte confiável
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<TrustedSource> sourceOpt = trustedSourceService.findById(id);
        
        if (sourceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fonte confiável não encontrada!");
            return "redirect:/admin/trusted-sources";
        }

        model.addAttribute("trustedSource", sourceOpt.get());
        return "admin/trusted-sources/view";
    }

    /**
     * Alterna status ativo/inativo de uma fonte confiável
     */
    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TrustedSource source = trustedSourceService.toggleActive(id);
            String status = source.isActive() ? "ativada" : "desativada";
            redirectAttributes.addFlashAttribute("successMessage", 
                "Fonte confiável '" + source.getName() + "' " + status + " com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/trusted-sources";
    }

    /**
     * Remove uma fonte confiável
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<TrustedSource> sourceOpt = trustedSourceService.findById(id);
            if (sourceOpt.isPresent()) {
                String sourceName = sourceOpt.get().getName();
                trustedSourceService.delete(id);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Fonte confiável '" + sourceName + "' removida com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Fonte confiável não encontrada!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro ao remover fonte confiável: " + e.getMessage());
        }
        
        return "redirect:/admin/trusted-sources";
    }

    /**
     * API para busca de fontes confiáveis (AJAX)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchSources(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<TrustedSource> sourcesPage = trustedSourceService.findBySearchTerm(term, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sources", sourcesPage.getContent());
        response.put("totalElements", sourcesPage.getTotalElements());
        response.put("totalPages", sourcesPage.getTotalPages());
        response.put("currentPage", page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * API para verificar se domínio já existe
     */
    @GetMapping("/api/check-domain")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkDomain(
            @RequestParam String domain,
            @RequestParam(required = false) Long excludeId) {
        
        boolean exists;
        if (excludeId != null) {
            exists = trustedSourceService.findById(excludeId).isPresent() && 
                    !trustedSourceService.findById(excludeId).get().getDomainName().equals(domain) &&
                    trustedSourceService.isDomainNameExists(domain);
        } else {
            exists = trustedSourceService.isDomainNameExists(domain);
        }
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        
        return ResponseEntity.ok(response);
    }

    /**
     * API para verificar se nome já existe
     */
    @GetMapping("/api/check-name")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkName(
            @RequestParam String name,
            @RequestParam(required = false) Long excludeId) {
        
        boolean exists;
        if (excludeId != null) {
            exists = trustedSourceService.findById(excludeId).isPresent() && 
                    !trustedSourceService.findById(excludeId).get().getName().equals(name) &&
                    trustedSourceService.isNameExists(name);
        } else {
            exists = trustedSourceService.isNameExists(name);
        }
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        
        return ResponseEntity.ok(response);
    }

    /**
     * API para obter estatísticas das fontes confiáveis
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", trustedSourceService.count());
        stats.put("active", trustedSourceService.countActive());
        stats.put("inactive", trustedSourceService.countInactive());
        
        List<TrustedSource> recentSources = trustedSourceService.findRecentSources(5);
        stats.put("recent", recentSources);
        
        return ResponseEntity.ok(stats);
    }
}