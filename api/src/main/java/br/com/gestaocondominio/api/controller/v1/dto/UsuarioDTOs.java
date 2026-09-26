package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.controller.dto.UsuarioCondominioRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Pedidos e respostas de {@code /api/v1/usuarios}. Os vínculos saem como {@code UsuarioCondominioDTO}. */
public final class UsuarioDTOs {

    private UsuarioDTOs() {
    }

    /** Como a senha do novo usuário é definida. */
    public enum AcaoSenha {
        ENVIAR_LINK("Enviar link de definição por e-mail (expira em 24h)"),
        CRIAR_SENHA("Definir senha manualmente");

        private final String descricao;

        AcaoSenha(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    /**
     * Novo acesso a um condomínio. Para o papel Morador, informe {@code pessoaId}, escolhido entre os ocupantes sem
     * acesso ({@code GET /api/v1/usuarios/ocupantes-sem-login}). Para os demais, informe o CPF: se ele já tiver
     * cadastro, o acesso é dado a essa pessoa e os outros dados pessoais são ignorados; se não, a pessoa é cadastrada.
     * Sem {@code acaoSenha}, vale {@code ENVIAR_LINK}. {@code CRIAR_SENHA} só vale para pessoa nova.
     */
    public record NovoUsuarioRequest(
            Integer condominioId,
            @NotNull(message = "Informe o papel do usuário.") UserRole papel,
            Integer pessoaId,
            AcaoSenha acaoSenha,
            @Size(max = 18, message = "CPF inválido.") String cpf,
            @Size(max = 100, message = "O nome pode ter até 100 caracteres.") String nome,
            @Email(message = "E-mail inválido.")
            @Size(max = 100, message = "O e-mail pode ter até 100 caracteres.") String email,
            @Size(max = 20, message = "O telefone pode ter até 20 caracteres.") String telefone,
            @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.") String senha) {

        public UsuarioCondominioRequestDTO paraDTO() {
            UsuarioCondominioRequestDTO dto = new UsuarioCondominioRequestDTO();
            dto.setCondominioId(condominioId);
            dto.setPapel(papel);
            dto.setPessoaId(pessoaId);
            dto.setAcaoSenha(acaoSenha == null ? null : acaoSenha.name());
            dto.setPesCpfCnpj(cpf);
            dto.setPesNome(nome);
            dto.setPesEmail(email);
            dto.setPesTelefone(telefone);
            dto.setPesSenhaLogin(senha);
            return dto;
        }
    }

    /** Edição do vínculo: nome e e-mail (o login) da pessoa e o papel dela no condomínio. */
    public record EditarUsuarioRequest(
            @NotBlank(message = "Informe o nome.")
            @Size(max = 100, message = "O nome pode ter até 100 caracteres.") String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 100, message = "O e-mail pode ter até 100 caracteres.") String email,
            @NotNull(message = "Informe o papel do usuário.") UserRole papel) {
    }

    /** Senha nova definida pela administração, sem link. */
    public record DefinirSenhaRequest(
            @NotBlank(message = "Informe a nova senha.")
            @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.") String novaSenha) {
    }

    /** Ocupante de unidade que ainda não tem acesso de morador ao condomínio. */
    public record OcupanteSemLogin(Integer pessoaCodigo, String nome, String email, String unidadeNumero,
                                   String unidadeBloco) {

        public static OcupanteSemLogin de(OcupanteResponseDTO ocupante) {
            return new OcupanteSemLogin(ocupante.pessoaId(), ocupante.nomeCompleto(), ocupante.email(),
                    ocupante.unidadeNumero(), ocupante.unidadeBloco());
        }
    }

    /** Pessoa já cadastrada com o CPF procurado, para preencher o formulário de novo usuário. */
    public record PessoaCadastrada(Integer codigo, String nome, String email, String telefone) {

        public static PessoaCadastrada de(Pessoa pessoa) {
            return new PessoaCadastrada(pessoa.getPesCod(), pessoa.getPesNome(), pessoa.getPesEmail(),
                    pessoa.getPesTelefone());
        }
    }

    public record CondominioDisponivel(Integer codigo, String nome) {

        public static CondominioDisponivel de(Condominio condominio) {
            return new CondominioDisponivel(condominio.getConCod(), condominio.getConNome());
        }
    }

    public record OpcoesUsuario(List<CondominioDisponivel> condominios, List<Opcao> papeis, List<Opcao> acoesSenha,
                                boolean podeGerenciar) {
    }
}
