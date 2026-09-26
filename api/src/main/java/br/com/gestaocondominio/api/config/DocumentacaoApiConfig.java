package br.com.gestaocondominio.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação da API em {@code /swagger-ui.html}, que é a referência de quem desenvolve o sistema web e o aplicativo.
 * O botão "Authorize" recebe o token do login.
 */
@Configuration
public class DocumentacaoApiConfig {

    @Bean
    public OpenAPI documentacaoDaApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CONDIGTAL — API")
                        .version("v1")
                        .description("""
                                API do sistema web e do aplicativo. Cada cliente tem o seu endereço \
                                (api.<cliente>.<domínio>), que define o banco de dados usado.

                                Login: POST /api/v1/auth/login devolve o token de acesso e o de renovação. \
                                Envie o de acesso em toda chamada (Authorization: Bearer <token>); quando ele \
                                vencer (resposta 401), troque o de renovação por um par novo em \
                                POST /api/v1/auth/renovar."""))
                .components(new Components().addSecuritySchemes("token", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
