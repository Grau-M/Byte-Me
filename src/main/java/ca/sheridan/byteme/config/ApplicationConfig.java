package ca.sheridan.byteme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService; // Keep this import
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    // 1. REMOVE userDetailsService from the constructor.
    // We don't need it here anymore.
    // private final UserDetailsService userDetailsService;

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) { // 2. PASS IT IN HERE
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        // 3. Spring will provide the userDetailsService bean (your UserService)
        authProvider.setUserDetailsService(userDetailsService); 
        
        // 4. Set the password encoder
        authProvider.setPasswordEncoder(passwordEncoder()); 
        
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}