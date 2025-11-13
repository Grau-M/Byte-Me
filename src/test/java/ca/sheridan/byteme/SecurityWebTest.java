package ca.sheridan.byteme;
 
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.repositories.UserRepository;
 
 
@SpringBootTest
@AutoConfigureMockMvc
class SecurityWebTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @Autowired
    private UserRepository userRepository;
 
    @Autowired
    private PasswordEncoder passwordEncoder;
 
    private final String adminEmail = "admin@cookie.com";
    private final String staffEmail = "staff@cookie.com";
    private final String customerEmail = "customer@cookie.com";
    private final String rawPassword = "Password123!";
 
 
    @BeforeEach
    void setUpDemoUser() {
        userRepository.deleteAll(); // Optional: clean slate for test isolation
 
        userRepository.save(User.builder()
                .name("Admin User")
                .email(adminEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .build());
 
        userRepository.save(User.builder()
                .name("Staff User")
                .email(staffEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.STAFF)
                .build());
 
        userRepository.save(User.builder()
                .name("Customer User")
                .email(customerEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.CUSTOMER)
                .build());
 
    }
 
    @Test
    void loginPageShowsFormFields() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"email\"")))
            .andExpect(content().string(containsString("name=\"password\"")))
            .andExpect(content().string(containsString("Login - Cookiegram Bakery")));
    }
 
    @Test
    void adminLoginShowsAdminDashboard() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(adminEmail).password(rawPassword))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Admin Dashboard")))
            .andExpect(content().string(containsString("System Administration")));
    }
 
    @Test
    void staffLoginShowsStaffDashboard() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(staffEmail).password(rawPassword))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Staff Dashboard")))
            .andExpect(content().string(containsString("Manage Orders")));
}
 
    @Test
    void customerLoginShowsCustomerDashboard() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(customerEmail).password(rawPassword))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Customer Dashboard")))
            .andExpect(content().string(containsString("Your Orders")));
    }
 
    @Test
    void staffShouldNotSeeAdminContent() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(staffEmail).password(rawPassword))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(content().string(not(containsString("Admin Dashboard"))))
            .andExpect(content().string(not(containsString("Admin Panel"))));
    }
 
    @Test
    void customerShouldNotSeeStaffOrAdminContent() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(customerEmail).password(rawPassword))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(content().string(not(containsString("Staff Dashboard"))))
            .andExpect(content().string(not(containsString("Admin Dashboard"))));
    }
    @Test
    void unauthenticatedUserRedirectedFromDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
    @Test
    void customerBlockedFromAdminContent() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(customerEmail).password(rawPassword))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Admin Dashboard"))))
            .andExpect(content().string(not(containsString("Admin Panel"))));
    }
 
    @Test
    void loginFailsWithInvalidPassword() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("email").user(adminEmail).password("WrongPassword"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }
 
    @Test
    void loginFailsWithUnknownUser() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("email").user("ghost@cookie.com").password("Password123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }
 
    @Test
    void logoutInvalidatesSession() throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(adminEmail).password(rawPassword))
            .andReturn();
 
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
 
        mockMvc.perform(post("/logout").session(session).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/?logout"));

 
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
 
 
 
}