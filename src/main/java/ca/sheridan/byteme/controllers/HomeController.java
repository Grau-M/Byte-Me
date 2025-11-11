package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private CartService cartService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("cartCount", cartService.getCartCount());
        return "index";
    }

    @GetMapping(params = "logout")
    public String indexWithLogout(Model model) {
        model.addAttribute("logoutSuccess", true);
        model.addAttribute("cartCount", cartService.getCartCount());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // will render src/main/resources/templates/login.html
    }
}