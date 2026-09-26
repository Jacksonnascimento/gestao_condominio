package br.com.gestaocondominio.api.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Endereço do sistema web do cliente atual, base dos links enviados por e-mail (definir ou redefinir a senha).
 *
 * <p>Vem só de {@code WEB_URL_PUBLICA}, com {@code {cliente}} trocado pelo identificador do cliente. Nunca do
 * {@code Origin} ou do {@code Host} da requisição: quem pede "esqueci a senha" não precisa de login, e um cabeçalho
 * forjado faria o e-mail da vítima levar o token de redefinição para outro site.</p>
 */
@Component
public class EnderecoDoSistemaWeb {

    private final String modelo;

    public EnderecoDoSistemaWeb(@Value("${condigtal.web.url-publica:}") String modelo) {
        this.modelo = modelo == null ? "" : modelo.trim();
    }

    /**
     * Endereço sem a barra final, como {@code https://residencialflores.condigtal.com.br}.
     *
     * @throws IllegalStateException se {@code WEB_URL_PUBLICA} não estiver configurada
     */
    public String atual() {
        if (modelo.isEmpty()) {
            throw new IllegalStateException("Configure WEB_URL_PUBLICA no .env (ex.: https://{cliente}.condigtal.com.br) "
                    + "para o sistema poder enviar links por e-mail.");
        }
        return ClienteAtual.noEndereco(modelo).replaceAll("/+$", "");
    }
}
