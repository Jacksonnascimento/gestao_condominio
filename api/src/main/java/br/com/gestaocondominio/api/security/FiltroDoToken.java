package br.com.gestaocondominio.api.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica as chamadas de {@code /api/v1} pelo token de acesso. A pessoa é lida do banco a cada chamada: inativada,
 * ou com a senha trocada, perde o acesso na hora, sem esperar o token vencer. Sem token válido a chamada segue sem
 * login, e a regra de acesso da rota decide (401 pelo {@link RespostasDeSeguranca}).
 *
 * <p>Não é um {@code @Component} de propósito: registrado como bean de filtro, o Spring o aplicaria também às telas
 * antigas, fora da cadeia de segurança da API.</p>
 */
public class FiltroDoToken extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;

    public FiltroDoToken(TokenService tokenService, UserDetailsServiceImpl userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        DecodedJWT token = tokenService.validarAcesso(tokenDoCabecalho(request));
        if (token != null) {
            autenticar(token);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(DecodedJWT token) {
        UserDetailsImpl usuario;
        try {
            usuario = userDetailsService.loadUserByCodigo(Integer.valueOf(token.getSubject()));
        } catch (UsernameNotFoundException | NumberFormatException e) {
            return;
        }
        if (!usuario.isEnabled() || !tokenService.senhaConfere(token, usuario.getPessoa())) {
            return;
        }
        var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    static String tokenDoCabecalho(HttpServletRequest request) {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return cabecalho.substring(7).trim();
        }
        return null;
    }
}
