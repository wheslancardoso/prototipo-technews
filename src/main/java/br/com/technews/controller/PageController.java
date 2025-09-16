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

@Controller
public class PageController {
    
    @Autowired
    private SubscriberService subscriberService;
    
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String nome, 
                          @RequestParam String email,
                          RedirectAttributes redirectAttributes) {
        try {
            Subscriber subscriber = subscriberService.subscribe(nome, email);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Inscrição realizada com sucesso! Bem-vindo(a), " + subscriber.getNome() + "!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro interno. Tente novamente mais tarde.");
        }
        
        return "redirect:/";
    }
}
