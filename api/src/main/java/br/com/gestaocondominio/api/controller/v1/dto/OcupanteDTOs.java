package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/ocupantes}. */
public final class OcupanteDTOs {

    private OcupanteDTOs() {
    }

    /**
     * Vínculo de uma pessoa a uma unidade. Se o CPF/CNPJ ainda não tem cadastro, a pessoa é cadastrada com nome,
     * e-mail e telefone (nome e e-mail passam a ser obrigatórios); se já tem, esses campos são ignorados. Período de
     * uso e tipo do período só valem para multiproprietário.
     */
    public record CadastrarOcupanteRequest(
            @NotBlank(message = "Informe o CPF/CNPJ.") String cpfCnpj,
            @Pattern(regexp = "[FJ]", message = "Tipo de pessoa inválido: use F (física) ou J (jurídica).")
            String tipoPessoa,
            @Size(max = 100, message = "O nome pode ter até 100 caracteres.") String nome,
            @Email(message = "E-mail inválido.") @Size(max = 100, message = "O e-mail pode ter até 100 caracteres.")
            String email,
            @Size(max = 20, message = "O telefone pode ter até 20 caracteres.") String telefone,
            @NotNull(message = "Informe a unidade.") Integer unidadeId,
            @NotNull(message = "Informe o vínculo.") OcupanteVinculo vinculo,
            @NotNull(message = "Informe o início da ocupação.") LocalDate inicioOcupacao,
            LocalDate fimOcupacao,
            @Size(max = 100, message = "O período de uso pode ter até 100 caracteres.") String periodoUso,
            TipoPeriodoOcupante tipoPeriodo) {

        public OcupanteRequestDTO paraDto() {
            OcupanteRequestDTO dto = new OcupanteRequestDTO();
            dto.setPesCpfCnpj(cpfCnpj);
            dto.setPesTipo(tipoPessoa == null ? 'F' : tipoPessoa.charAt(0));
            dto.setPesNome(nome == null ? null : nome.trim());
            dto.setPesEmail(email == null ? null : email.trim());
            dto.setPesTelefone(telefone);
            dto.setUnidadeId(unidadeId);
            dto.setVinculo(vinculo);
            dto.setInicioOcupacao(inicioOcupacao);
            dto.setFimOcupacao(fimOcupacao);
            dto.setPeriodoUso(periodoUso);
            dto.setTipoPeriodo(tipoPeriodo);
            return dto;
        }
    }

    /** Edição: o CPF/CNPJ e a unidade não mudam; para trocar de unidade, exclua e cadastre de novo. */
    public record AtualizarOcupanteRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 100, message = "O nome pode ter até 100 caracteres.")
            String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 100, message = "O e-mail pode ter até 100 caracteres.") String email,
            @Size(max = 20, message = "O telefone pode ter até 20 caracteres.") String telefone,
            @NotNull(message = "Informe o vínculo.") OcupanteVinculo vinculo,
            @NotNull(message = "Informe o início da ocupação.") LocalDate inicioOcupacao,
            LocalDate fimOcupacao,
            @Size(max = 100, message = "O período de uso pode ter até 100 caracteres.") String periodoUso,
            TipoPeriodoOcupante tipoPeriodo) {

        public OcupanteRequestDTO paraDto() {
            OcupanteRequestDTO dto = new OcupanteRequestDTO();
            dto.setPesNome(nome.trim());
            dto.setPesEmail(email.trim());
            dto.setPesTelefone(telefone);
            dto.setVinculo(vinculo);
            dto.setInicioOcupacao(inicioOcupacao);
            dto.setFimOcupacao(fimOcupacao);
            dto.setPeriodoUso(periodoUso);
            dto.setTipoPeriodo(tipoPeriodo);
            return dto;
        }
    }

    /**
     * Um ocupante. O {@code cpfCnpj} só vem para quem gerencia os ocupantes do condomínio, e só na busca por id (é o
     * que o formulário de edição mostra); nas listagens e para os moradores vem nulo.
     */
    public record OcupanteResposta(Integer id, Integer pessoaId, String nome, String cpfCnpj, String email,
                                   String telefone, String vinculo, String vinculoDescricao, Integer unidadeId,
                                   String unidadeNumero, String unidadeBloco, Integer condominioId,
                                   String condominioNome, LocalDate inicioOcupacao, LocalDate fimOcupacao,
                                   String periodoUso, TipoPeriodoOcupante tipoPeriodo, String tipoPeriodoDescricao) {

        public static OcupanteResposta de(Ocupante o, boolean comDocumento) {
            Pessoa pessoa = o.getPessoa();
            return new OcupanteResposta(o.getOcuCod(), pessoa.getPesCod(), pessoa.getPesNome(),
                    comDocumento ? pessoa.getPesCpfCnpj() : null, pessoa.getPesEmail(), pessoa.getPesTelefone(),
                    o.getOcuVinculo() == null ? null : o.getOcuVinculo().name(),
                    o.getOcuVinculo() == null ? null : o.getOcuVinculo().getDescricao(),
                    o.getUnidade().getUniCod(), o.getUnidade().getUniNumero(), o.getUnidade().getBloco(),
                    o.getUnidade().getCondominio().getConCod(), o.getUnidade().getCondominio().getConNome(),
                    o.getOcuDtInicioOcupacao(), o.getOcuDtFimOcupacao(), o.getOcuPeriodoUso(), o.getOcuTipoPeriodo(),
                    o.getOcuTipoPeriodo() == null ? null : o.getOcuTipoPeriodo().getDescricao());
        }
    }

    public record OpcoesOcupante(List<Opcao> vinculos, List<Opcao> tiposPeriodo, boolean podeGerenciar) {
    }
}
