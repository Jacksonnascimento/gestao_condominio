package br.com.gestaocondominio.api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * API usada pelo sistema web (Next.js) e pelo aplicativo: sem sessão nem CSRF, com login por token
     * ({@link FiltroDoToken}). Vem antes da cadeia das telas antigas e só vale para os caminhos abaixo.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, TokenService tokenService,
                                              RespostasDeSeguranca respostas) throws Exception {
        return http
            .securityMatcher("/api/v1/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(excecoes -> excecoes
                .authenticationEntryPoint(respostas)
                .accessDeniedHandler(respostas))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/login", "/api/v1/auth/renovar",
                    "/api/v1/auth/esqueci-senha", "/api/v1/auth/redefinir-senha").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/redefinir-senha/*").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new FiltroDoToken(tokenService, userDetailsService),
                UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /** Telas antigas em Thymeleaf, com login por formulário e sessão, até a migração para o Next.js terminar. */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // Configuração para permitir POST externo no endpoint de leads (ignorando CSRF apenas para /public/)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/public/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/esqueci-senha", "/definir-senha",
                    "/public/**" // Permite acesso público ao endpoint de leads
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authenticationProvider(authenticationProvider())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
            )
            .rememberMe(rememberMe -> rememberMe
                .userDetailsService(userDetailsService)
                .key("CONDIGTAL_REMEMBER_ME_KEY_SECRET")
                .tokenValiditySeconds(604800) // 7 dias
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                Set<String> roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                if (roles.contains("ROLE_GLOBAL_ADMIN") || 
                    roles.contains("ROLE_SINDICO") || 
                    roles.contains("ROLE_ADMIN") || 
                    roles.contains("ROLE_FUNCIONARIO_ADM") ||
                    roles.contains("ROLE_PORTEIRO")) {
                    response.sendRedirect("/dashboard");
                } 
                else if (roles.contains("ROLE_MORADOR")) {
                    response.sendRedirect("/unidades");
                } 
                else {
                    response.sendRedirect("/dashboard");
                }
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
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

    /**
     * Origens que o navegador pode usar para chamar a API com login: os endereços do sistema web de cada cliente,
     * separados por vírgula em {@code CORS_ORIGENS}. Aceita padrões, como {@code https://*.condigtal.com.br}, que
     * cobrem todos os clientes, inclusive os que ainda vão entrar. O aplicativo não passa por CORS.
     *
     * <p>O cadastro de interessados ({@code /public/**}) recebe o formulário do site de divulgação, de qualquer
     * origem, e sem credenciais.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${condigtal.cors.origens}") String origens) {
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOriginPatterns(Arrays.stream(origens.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        api.setAllowedHeaders(List.of("*"));
        api.setExposedHeaders(List.of("Content-Disposition"));
        api.setAllowCredentials(true);

        CorsConfiguration publico = new CorsConfiguration();
        publico.addAllowedOrigin("*");
        publico.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept"));
        publico.setAllowedMethods(Arrays.asList("POST", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", api);
        source.registerCorsConfiguration("/public/**", publico);
        return source;
    }
}
