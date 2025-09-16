package br.com.technews.controller;

import br.com.technews.entity.Subscriber;
import br.com.technews.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/subscribers")
public class AdminController {

    @Autowired
    private SubscriberService subscriberService;

    @GetMapping
    public String listSubscribers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            Model model) {
        
        Page<Subscriber> subscribers = subscriberService.findAllPaginated(PageRequest.of(page, size));
        
        model.addAttribute("subscribers", subscribers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", subscribers.getTotalPages());
        model.addAttribute("totalElements", subscribers.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("activeCount", subscriberService.countActiveSubscribers());
        model.addAttribute("totalCount", subscriberService.countTotalSubscribers());
        
        return "admin/subscribers/list";
    }
    
    @GetMapping("/{id}")
    public String viewSubscriber(@PathVariable Long id, Model model) {
        try {
            Subscriber subscriber = subscriberService.findById(id);
            model.addAttribute("subscriber", subscriber);
            return "admin/subscribers/view";
        } catch (RuntimeException e) {
            return "redirect:/admin/subscribers";
        }
    }
    
    @GetMapping("/{id}/edit")
    public String editSubscriber(@PathVariable Long id, Model model) {
        try {
            Subscriber subscriber = subscriberService.findById(id);
            model.addAttribute("subscriber", subscriber);
            return "admin/subscribers/form";
        } catch (RuntimeException e) {
            return "redirect:/admin/subscribers";
        }
    }
    
    @PostMapping("/{id}")
    public String updateSubscriber(@PathVariable Long id, @ModelAttribute Subscriber subscriber, RedirectAttributes redirectAttributes) {
        try {
            subscriber.setId(id);
            subscriberService.updateSubscriber(subscriber);
            redirectAttributes.addFlashAttribute("success", "Assinante atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao atualizar assinante: " + e.getMessage());
        }
        return "redirect:/admin/subscribers";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteSubscriber(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            subscriberService.deleteSubscriber(id);
            redirectAttributes.addFlashAttribute("success", "Assinante removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao remover assinante: " + e.getMessage());
        }
        return "redirect:/admin/subscribers";
    }
    
    @PostMapping("/{id}/toggle-status")
    public String toggleSubscriberStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Subscriber subscriber = subscriberService.findById(id);
            subscriber.setAtivo(!subscriber.isAtivo());
            subscriberService.updateSubscriber(subscriber);
            redirectAttributes.addFlashAttribute("success", "Status do assinante alterado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao alterar status: " + e.getMessage());
        }
        return "redirect:/admin/subscribers";
    }
    
    @GetMapping("/export")
    public ResponseEntity<String> exportSubscribers() {
        List<Subscriber> subscribers = subscriberService.getAllSubscribers();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Nome,Email,Ativo,Data de Inscrição\n");
        
        for (Subscriber subscriber : subscribers) {
            csv.append(subscriber.getId()).append(",")
               .append(subscriber.getNome()).append(",")
               .append(subscriber.getEmail()).append(",")
               .append(subscriber.isAtivo() ? "Sim" : "Não").append(",")
               .append(subscriber.getDataInscricao()).append("\n");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "subscribers.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
    }
}