package br.com.gestaocondominio.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("--- [LOGGING FILTER]: INCOMING REQUEST ---");
        System.out.println("Method: " + request.getMethod());
        System.out.println("URI: " + request.getRequestURI());

        System.out.println("Headers: ");
        Collections.list(request.getHeaderNames()).forEach(headerName ->
                System.out.println(headerName + ": " + Collections.list(request.getHeaders(headerName)))
        );
        System.out.println("-----------------------------------------");
        
        filterChain.doFilter(request, response);
    }
}