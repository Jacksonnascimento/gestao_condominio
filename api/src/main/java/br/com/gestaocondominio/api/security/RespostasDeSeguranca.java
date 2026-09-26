package br.com.gestaocondominio.api.security;

import br.com.gestaocondominio.api.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas da API quando falta login (401) ou permissão (403), em JSON. O 401 é o que avisa o sistema web e o
 * aplicativo de que o token venceu e é hora de renovar ou voltar ao login; sem isto o Spring responderia 403 nos dois
 * casos, ou redirecionaria para a tela de login antiga.
 */
@Component
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostasDeSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Sessão expirada ou inválida. Entre novamente.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN,
                "Acesso negado. Você não tem permissão para executar esta ação.");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                          String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(status, mensagem, request.getRequestURI()));
    }
}
