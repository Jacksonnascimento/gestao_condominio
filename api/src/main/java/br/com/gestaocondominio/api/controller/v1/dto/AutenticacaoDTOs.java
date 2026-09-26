package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/auth} e {@code /api/v1/perfil}. */
public final class AutenticacaoDTOs {

    private AutenticacaoDTOs() {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record RenovarRequest(@NotBlank(message = "Informe o token de renovação.") String tokenRenovacao) {
    }

    public record EsqueciSenhaRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email) {
    }

    public record RedefinirSenhaRequest(
            @NotBlank(message = "O link de redefinição é inválido.") String token,
            @NotBlank(message = "Informe a nova senha.")
            @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.") String novaSenha) {
    }

    public record AtualizarPerfilRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 100, message = "O nome pode ter até 100 caracteres.")
            String nome,
            @Size(max = 20) String telefone,
            @Size(max = 20) String telefone2) {
    }

    public record TrocarSenhaRequest(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank(message = "Informe a nova senha.")
            @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.") String novaSenha) {
    }

    /**
     * Tokens emitidos no login e na renovação. O {@code token} vai em toda chamada ({@code Authorization: Bearer});
     * o {@code tokenRenovacao} só serve para pedir um par novo quando o primeiro vencer ({@code expiraEm}).
     */
    public record LoginResponse(String token, String tokenRenovacao, Instant expiraEm, UsuarioLogado usuario) {
    }

    /** Quem está logado, com os condomínios e papéis que definem o que cada tela mostra. */
    public record UsuarioLogado(Integer codigo, String nome, String email, String cpfCnpj, String telefone,
                                String telefone2, boolean administradorGeral, boolean possuiFoto,
                                List<Vinculo> vinculos) {

        public static UsuarioLogado de(Pessoa pessoa, List<UsuarioCondominio> vinculos) {
            return new UsuarioLogado(pessoa.getPesCod(), pessoa.getPesNome(), pessoa.getPesEmail(),
                    pessoa.getPesCpfCnpj(), pessoa.getPesTelefone(), pessoa.getPesTelefone2(),
                    Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin()), pessoa.getPesImagem() != null,
                    vinculos.stream()
                            .filter(v -> Boolean.TRUE.equals(v.getUscAtivoAssociacao()))
                            .map(Vinculo::de)
                            .toList());
        }
    }

    public record Vinculo(Integer condominioCodigo, String condominioNome, UserRole papel, String papelDescricao) {

        static Vinculo de(UsuarioCondominio vinculo) {
            return new Vinculo(vinculo.getConCod(),
                    vinculo.getCondominio() == null ? null : vinculo.getCondominio().getConNome(),
                    vinculo.getUscPapel(), vinculo.getUscPapel().getDescricao());
        }
    }

    public record Mensagem(String mensagem) {
    }
}
