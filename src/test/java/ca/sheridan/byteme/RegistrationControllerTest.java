package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.controllers.RegistrationController;
import ca.sheridan.byteme.models.RegistrationForm;
import ca.sheridan.byteme.repositories.UserRepository;
import ca.sheridan.byteme.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private RegistrationController registrationController;

    @Test
    public void testHandleRegister_Success() {
        // Arrange
        RegistrationForm form = new RegistrationForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setConfirmPassword("password");

        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.empty());
        when(bindingResult.hasErrors()).thenReturn(false);

        // Act
        String result = registrationController.handleRegister(form, bindingResult, redirectAttributes);

        // Assert
        assertEquals("redirect:/login", result);
        verify(userService).createUser("Test User", "test@example.com", "password", Role.CUSTOMER);
        verify(redirectAttributes).addFlashAttribute("registered", true);
    }
    @Test
    public void testHandleRegister_PasswordMismatch() {
        // Arrange
        RegistrationForm form = new RegistrationForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setConfirmPassword("wrongpassword");

        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        String result = registrationController.handleRegister(form, bindingResult, redirectAttributes);

        // Assert
        assertEquals("redirect:/register", result);
        verify(redirectAttributes).addFlashAttribute(eq("org.springframework.validation.BindingResult.registrationForm"), any());
        verify(redirectAttributes).addFlashAttribute(eq("registrationForm"), any());
    }

    @Test
    public void testHandleRegister_EmailExists() {
        // Arrange
        RegistrationForm form = new RegistrationForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setConfirmPassword("password");

        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.of(new ca.sheridan.byteme.beans.User()));
        when(bindingResult.hasErrors()).thenReturn(true); // <-- Added this line

        // Act
        String result = registrationController.handleRegister(form, bindingResult, redirectAttributes);

        // Assert
        assertEquals("redirect:/register", result);
        verify(bindingResult).rejectValue("email", "exists", "Email already registered");
        verify(redirectAttributes).addFlashAttribute(eq("org.springframework.validation.BindingResult.registrationForm"), any());
        verify(redirectAttributes).addFlashAttribute(eq("registrationForm"), any());
    }
}
