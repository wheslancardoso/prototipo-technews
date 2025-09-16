package br.com.technews.controller;

import br.com.technews.model.Subscriber;
import br.com.technews.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PageController {

    @Autowired
    private SubscriberService subscriberService;
    
    @GetMapping("/")
    public String home(Model model) {
        List<Subscriber> subscribers = subscriberService.getAllSubscribers();
        model.addAttribute("subscriberCount", subscribers.size());
        return "index";
    }

    @GetMapping("/admin")
    public String adminDashboard() {
        return "redirect:/admin/articles";
    }
    
    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String nome, 
                          @RequestParam String email, 
                          RedirectAttributes redirectAttributes) {
        try {
            subscriberService.subscribe(nome, email);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Obrigado, " + nome + "! Sua inscrição foi realizada com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro interno. Tente novamente mais tarde.");
        }
        
        return "redirect:/";
    }
}
