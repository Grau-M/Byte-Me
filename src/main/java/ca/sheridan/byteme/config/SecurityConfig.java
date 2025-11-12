package ca.sheridan.byteme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    // We no longer inject JwtAuthenticationFilter
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            
            .formLogin(form -> form
                .loginPage("/login") 
                .defaultSuccessUrl("/dashboard", true)
                .usernameParameter("email")  // <-- ADD THIS LINE
                .permitAll()
            )
            
            .logout(logout -> logout
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**", 
                    "/h2-console/**",
                    "/",
                    "/login",
                    "/register",
                    "/css/**","/js/**","/images/**","/favicon.ico",
                    "/order", "/add-to-cart", "/checkout", "/charge", "/result",
                    "/api/shipping/calculate"
                ).permitAll()
                .requestMatchers("/checkout").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider);
            
        return http.build();
    }
}