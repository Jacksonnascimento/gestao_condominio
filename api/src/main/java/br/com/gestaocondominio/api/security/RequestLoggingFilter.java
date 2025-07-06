package br.com.gestaocondominio.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
       
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        System.out.println("--- [LOGGING FILTER]: INCOMING REQUEST ---");
        System.out.println("Method: " + wrappedRequest.getMethod());
        System.out.println("URI: " + wrappedRequest.getRequestURI());

        System.out.println("Headers: ");
        Collections.list(wrappedRequest.getHeaderNames()).forEach(headerName ->
                System.out.println(headerName + ": " + Collections.list(wrappedRequest.getHeaders(headerName)))
        );
        
        
        filterChain.doFilter(wrappedRequest, response);

      
        byte[] requestBody = wrappedRequest.getContentAsByteArray();
        if (requestBody.length > 0) {
            String bodyString = new String(requestBody, StandardCharsets.UTF_8);
            System.out.println("Request Body: " + bodyString);
        } else {
            System.out.println("Request Body: [EMPTY]");
        }
        System.out.println("-----------------------------------------");
    }
}