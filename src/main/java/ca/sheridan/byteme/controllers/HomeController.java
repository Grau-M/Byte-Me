package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.models.AuthenticationRequest;
import ca.sheridan.byteme.models.AuthenticationResponse;
import ca.sheridan.byteme.services.AuthenticationService;
import ca.sheridan.byteme.services.CartService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthenticationService authenticationService;

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

    @PostMapping("/login")
    public String login(AuthenticationRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        try {
            AuthenticationResponse authResponse = authenticationService.authenticate(request);
            Cookie cookie = new Cookie("jwt", authResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Invalid credentials");
            return "redirect:/login";
        }
    }
}