package br.com.technews.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminEntryController {

    @GetMapping
    public String redirectToArticles() {
        return "redirect:/admin/articles";
    }
}