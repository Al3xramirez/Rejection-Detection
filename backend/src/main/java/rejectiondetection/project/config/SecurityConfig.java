package rejectiondetection.project.config;

import java.util.LinkedHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration class for the application.
 * Configures HTTP security, OAuth2 login, and logout settings.
 * Currently the set-up is allowing or permitting access to these enpoints,
 * this is to make sure that a user can access these endpoints without authentication.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/dashboard/sync", "/api/detection/analyze"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/test", "/api/auth/google", "/api/auth/me", "/oauth2/**", "/login/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(googleAuthorizationRequestResolver(clientRegistrationRepository))
                ) 
                .defaultSuccessUrl("http://localhost:5173/dashboard", true) //redirect to dashboard page now
            ) 
            .logout(Customizer.withDefaults());

        return http.build();
    }

    /**
     * CORS config so the Vue frontend (localhost:5173) can send the JSESSIONID
     * cookie to the backend (localhost:8080) on every API call.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // required to forward the JSESSIONID cookie

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
            );

        return new OAuth2AuthorizationRequestResolver() {
            
    @Override
    public OAuth2AuthorizationRequest resolve(jakarta.servlet.http.HttpServletRequest request) {
        return customizeAuthorizationRequest(defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
        jakarta.servlet.http.HttpServletRequest request,
        String clientRegistrationId
    ) {
        return customizeAuthorizationRequest(defaultResolver.resolve(request, clientRegistrationId));
    }
};
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest request) {
        if (request == null) {
            return null;
        }

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>(request.getAdditionalParameters());
        parameters.put("access_type", "offline");
        parameters.put("prompt", "consent");
        parameters.put("include_granted_scopes", "true");

        return OAuth2AuthorizationRequest.from(request)
            .additionalParameters(parameters)
            .build();
    }

}
