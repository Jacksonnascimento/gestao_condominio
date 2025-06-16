package br.com.gestaocondominio.api.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.security.jwt")
public record JwtProperties(String secretKey, long expirationTimeMs) {
}