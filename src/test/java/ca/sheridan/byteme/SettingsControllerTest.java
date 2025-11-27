package ca.sheridan.byteme;

import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.controllers.SettingsController;
import ca.sheridan.byteme.models.UpdateProfileRequest;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class SettingsControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CartService cartService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SettingsController settingsController;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setName("Test User");
        currentUser.setEmail("test@example.com");
        currentUser.setPassword("encodedPassword");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(currentUser);
    }

    @Test
    void testUpdateProfile_Success() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Updated Name");
        request.setEmail("updated@example.com");
        request.setCurrentPassword("currentPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmNewPassword("newPassword123");

        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        // Act
        String result = settingsController.updateProfile(request, redirectAttributes);

        // Assert
        assertEquals("redirect:/settings", result);
        verify(userService).saveUser(currentUser);
        assertEquals("Updated Name", currentUser.getName());
        assertEquals("updated@example.com", currentUser.getEmail());
        assertEquals("newEncodedPassword", currentUser.getPassword());
        verify(redirectAttributes).addFlashAttribute("successMessage", "Profile updated successfully!");
    }    @Test
    void testUpdateProfile_IncorrectPassword() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmNewPassword("newPassword123");

        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act
        String result = settingsController.updateProfile(request, redirectAttributes);

        // Assert
        assertEquals("redirect:/settings", result);
        verify(userService, never()).saveUser(any());
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Incorrect current password.");
    }

    @Test
    void testUpdateProfile_MismatchedNewPasswords() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("currentPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmNewPassword("mismatchedPassword");

        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

        // Act
        String result = settingsController.updateProfile(request, redirectAttributes);

        // Assert
        assertEquals("redirect:/settings", result);
        verify(userService, never()).saveUser(any());
        verify(redirectAttributes).addFlashAttribute("errorMessage", "New passwords do not match.");
    }

    @Test
    void testUpdatePreferences_Success() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setTimezone("America/New_York");
        request.setEmailNotifications(true);
        request.setInAppNotifications(false);
        request.setPushNotifications(true);
        request.setDarkThemeEnabled(false);

        // Act
        String result = settingsController.updatePreferences(request, redirectAttributes);

        // Assert
        assertEquals("redirect:/settings", result);
        verify(userService).saveUser(currentUser);
        assertEquals("America/New_York", currentUser.getTimezone());
        assertEquals(true, currentUser.isEmailNotifications());
        assertEquals(false, currentUser.isInAppNotifications());
        assertEquals(true, currentUser.isPushNotifications());
        assertEquals(false, currentUser.isDarkThemeEnabled());
        verify(redirectAttributes).addFlashAttribute("successMessage", "Preferences updated successfully!");
    }
}
