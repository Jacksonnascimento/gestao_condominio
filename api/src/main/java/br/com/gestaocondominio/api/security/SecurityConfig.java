package br.com.gestaocondominio.api.security;

import jakarta.servlet.DispatcherType;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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
     * ({@link FiltroDoToken}). Vem antes da cadeia dos demais caminhos e só vale para os caminhos abaixo.
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

    /**
     * Tudo o que não é da API: só o cadastro de interessados ({@code /public/**}), que recebe o formulário do site de
     * divulgação, fica aberto. O resto é recusado com 401/403 em JSON, sem sessão, sem tela de login e sem
     * redirecionamento.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain demaisCaminhosFilterChain(HttpSecurity http, RespostasDeSeguranca respostas)
            throws Exception {
        return http
            .cors(Customizer.withDefaults())
            // O site de divulgação envia o formulário de outra origem, sem token de CSRF
            .csrf(csrf -> csrf.ignoringRequestMatchers("/public/**"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(excecoes -> excecoes
                .authenticationEntryPoint(respostas)
                .accessDeniedHandler(respostas))
            .authorizeHttpRequests(auth -> auth
                // O repasse para /error (de qualquer caminho, inclusive da API) passa por esta cadeia; sem liberar,
                // uma falha numa chamada da API com token virava 401 em vez do erro real
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/public/**").permitAll()
                .anyRequest().denyAll())
            .build();
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
