package br.com.gestaocondominio.api.cliente;

import br.com.gestaocondominio.api.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Descobre, pelo endereço acessado, de qual cliente é a requisição, antes de qualquer outro filtro — inclusive o de
 * segurança, que já consulta o banco para conferir o login.
 *
 * <p>O endereço vem de {@code X-Forwarded-Host} quando a requisição passou pelo front, que chama a API pelo proxy
 * interno do Next e repassa o endereço original nesse cabeçalho; sem ele, vale o {@code Host}, que é o caso de quem
 * chama {@code api.<cliente>...} diretamente, como o aplicativo.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroDoCliente extends OncePerRequestFilter {

    private final ClientesAtendidos clientes;
    private final ObjectMapper objectMapper;

    public FiltroDoCliente(ClientesAtendidos clientes, ObjectMapper objectMapper) {
        this.clientes = clientes;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!clientes.multiplos()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Cliente> cliente = clientes.doEndereco(enderecoAcessado(request));
        if (cliente.isEmpty()) {
            recusar(request, response);
            return;
        }

        ClienteAtual.definir(cliente.get());
        try {
            filterChain.doFilter(request, response);
        } finally {
            ClienteAtual.limpar();
        }
    }

    /** A página de erro e as respostas assíncronas também passam pela segurança, que consulta o banco. */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    static String enderecoAcessado(HttpServletRequest request) {
        String repassado = request.getHeader("X-Forwarded-Host");
        if (repassado != null && !repassado.isBlank()) {
            // Atravessando mais de um proxy, o cabeçalho acumula os endereços; o primeiro é o que o navegador acessou.
            return repassado.split(",")[0].trim();
        }
        String host = request.getHeader("Host");
        return host != null && !host.isBlank() ? host : request.getServerName();
    }

    private void recusar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(HttpStatus.NOT_FOUND,
                "Este endereço não corresponde a nenhum cliente atendido.", request.getRequestURI()));
    }
}
