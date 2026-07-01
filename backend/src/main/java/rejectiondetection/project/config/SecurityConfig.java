package rejectiondetection.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration class for the application.
 * Configures HTTP security, OAuth2 login, and logout settings.
 * Currently the set-up is allowing or permitting access to these enpoints,
 * this is to make sure that a user can access these endpoints without authentication.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/test", "/api/auth/google", "/oauth2/**", "/login/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl("http://localhost:5173/", true) // Will Redirect this URL to a dashboard API soon
            )
            .logout(Customizer.withDefaults());

        return http.build();
    }
}
