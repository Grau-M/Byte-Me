package ca.sheridan.byteme.controllers;

// AuthenticationRequest and AuthenticationResponse may no longer be needed here
// import ca.sheridan.byteme.models.AuthenticationRequest;
// import ca.sheridan.byteme.models.AuthenticationResponse;

import ca.sheridan.byteme.services.CartService;
// No longer need these imports for the deleted method
// import jakarta.servlet.http.Cookie;
// import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
// No longer need PostMapping or RedirectAttributes
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        // This adds a "logoutSuccess" attribute to the model,
        // which you can use in index.html to show a "Logged out successfully" message.
        model.addAttribute("logoutSuccess", true); 
        model.addAttribute("cartCount", cartService.getCartCount());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        // This just serves the login.html page.
        // Spring Security handles the POST.
        return "login"; 
    }

    //
    //  DELETE THE @PostMapping("/login") METHOD
    //  It is no longer needed and conflicts with SecurityConfig
    //
    
}