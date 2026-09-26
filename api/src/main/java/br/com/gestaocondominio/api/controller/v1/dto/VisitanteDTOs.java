package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.VisitanteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.Visitante;
import br.com.gestaocondominio.api.domain.enums.VisitanteStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/** Pedidos e respostas de {@code /api/v1/visitantes}. */
public final class VisitanteDTOs {

    private VisitanteDTOs() {
    }

    /**
     * Entrada ou edição de um visitante. O condomínio é o da unidade; o morador que autorizou, quando informado,
     * precisa ser ocupante dessa unidade.
     */
    public record VisitantePedido(
            @NotBlank(message = "O nome do visitante é obrigatório.")
            @Size(max = 100, message = "O nome pode ter até 100 caracteres.") String nome,
            @Size(max = 14, message = "O CPF pode ter até 14 caracteres.") String cpf,
            @Size(max = 20, message = "O RG pode ter até 20 caracteres.") String rg,
            @Size(max = 20, message = "O telefone pode ter até 20 caracteres.") String telefone,
            @NotNull(message = "A unidade é obrigatória.") Integer unidadeId,
            Integer moradorId,
            String observacoes) {

        /** Converte para o formato que o {@code VisitanteService} recebe. */
        public VisitanteRequestDTO paraRequisicao() {
            VisitanteRequestDTO dto = new VisitanteRequestDTO();
            dto.setNome(nome.trim());
            dto.setCpf(textoOuNulo(cpf));
            dto.setRg(textoOuNulo(rg));
            dto.setTelefone(textoOuNulo(telefone));
            dto.setUnidadeId(unidadeId);
            dto.setMoradorId(moradorId);
            dto.setObservacoes(textoOuNulo(observacoes));
            return dto;
        }

        private static String textoOuNulo(String texto) {
            return StringUtils.hasText(texto) ? texto.trim() : null;
        }
    }

    /**
     * Visitante como aparece no cartão da listagem: sem CPF, RG e observações, que a tela antiga só mostra no
     * formulário de edição. {@code podeAlterar} diz se quem está logado pode editar e registrar a saída.
     */
    public record VisitanteResumo(Integer id, String nome, String telefone, VisitanteStatus status,
                                  String statusDescricao, LocalDateTime dataEntrada, LocalDateTime dataSaida,
                                  Integer condominioId, String condominioNome, Integer unidadeId,
                                  String unidadeNumero, String unidadeBloco, Integer moradorId, String moradorNome,
                                  boolean podeAlterar) {

        public static VisitanteResumo de(Visitante visitante, boolean podeAlterar) {
            Unidade unidade = visitante.getUnidade();
            Condominio condominio = visitante.getCondominio();
            return new VisitanteResumo(visitante.getVisCod(), visitante.getNome(), visitante.getTelefone(),
                    visitante.getStatus(), visitante.getStatus() == null ? null : visitante.getStatus().getDescricao(),
                    visitante.getDataEntrada(), visitante.getDataSaida(),
                    condominio.getConCod(), condominio.getConNome(),
                    unidade.getUniCod(), unidade.getUniNumero(), unidade.getBloco(),
                    visitante.getMoradorAutorizou() == null ? null : visitante.getMoradorAutorizou().getPesCod(),
                    visitante.getMoradorAutorizou() == null ? null : visitante.getMoradorAutorizou().getPesNome(),
                    podeAlterar);
        }
    }

    /**
     * Um visitante. CPF, RG e observações só vêm preenchidos para quem pode alterar o registro (administração e
     * portaria do condomínio), como no formulário de edição da tela antiga; para o morador vêm nulos.
     */
    public record VisitanteDetalhe(Integer id, String nome, String cpf, String rg, String telefone,
                                   String observacoes, VisitanteStatus status, String statusDescricao,
                                   LocalDateTime dataEntrada, LocalDateTime dataSaida,
                                   Integer condominioId, String condominioNome, Integer unidadeId,
                                   String unidadeNumero, String unidadeBloco, Integer moradorId, String moradorNome,
                                   boolean podeAlterar) {

        public static VisitanteDetalhe de(Visitante visitante, boolean podeAlterar) {
            VisitanteResumo resumo = VisitanteResumo.de(visitante, podeAlterar);
            return new VisitanteDetalhe(resumo.id(), resumo.nome(),
                    podeAlterar ? visitante.getCpf() : null,
                    podeAlterar ? visitante.getRg() : null,
                    resumo.telefone(),
                    podeAlterar ? visitante.getObservacoes() : null,
                    resumo.status(), resumo.statusDescricao(), resumo.dataEntrada(), resumo.dataSaida(),
                    resumo.condominioId(), resumo.condominioNome(), resumo.unidadeId(), resumo.unidadeNumero(),
                    resumo.unidadeBloco(), resumo.moradorId(), resumo.moradorNome(), podeAlterar);
        }
    }

    /** Condomínio para o filtro da listagem e para o formulário. */
    public record CondominioOpcao(Integer id, String nome) {

        public static CondominioOpcao de(Condominio condominio) {
            return new CondominioOpcao(condominio.getConCod(), condominio.getConNome());
        }
    }

    /** Unidade ativa para o filtro da listagem e para o formulário. */
    public record UnidadeOpcao(Integer id, String numero, String bloco, Integer condominioId) {

        public static UnidadeOpcao de(Unidade unidade) {
            return new UnidadeOpcao(unidade.getUniCod(), unidade.getUniNumero(), unidade.getBloco(),
                    unidade.getCondominio().getConCod());
        }
    }

    /** Ocupante da unidade que pode ser indicado como quem autorizou a entrada. Só nome e vínculo. */
    public record MoradorOpcao(Integer id, String nome, String vinculo) {

        public static MoradorOpcao de(Ocupante ocupante) {
            return new MoradorOpcao(ocupante.getPessoa().getPesCod(), ocupante.getPessoa().getPesNome(),
                    ocupante.getOcuVinculo() == null ? null : ocupante.getOcuVinculo().getDescricao());
        }
    }
}
