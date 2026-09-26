package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.enums.CondominioTipologia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/condominios}. */
public final class CondominioDTOs {

    private CondominioDTOs() {
    }

    /**
     * Dados do cadastro. O CEP pode vir com ou sem hífen e é gravado só com os números; a UF é gravada em maiúsculas.
     * Na edição, campo nulo mantém o valor atual.
     */
    public record CondominioRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 100, message = "O nome pode ter até 100 caracteres.")
            String nome,
            @Size(max = 100, message = "O logradouro pode ter até 100 caracteres.") String logradouro,
            @Size(max = 10, message = "O número pode ter até 10 caracteres.") String numero,
            @Size(max = 50, message = "O complemento pode ter até 50 caracteres.") String complemento,
            @Size(max = 50, message = "O bairro pode ter até 50 caracteres.") String bairro,
            @Size(max = 50, message = "A cidade pode ter até 50 caracteres.") String cidade,
            @Pattern(regexp = "([A-Za-z]{2})?", message = "Informe a UF com duas letras.") String estado,
            @Pattern(regexp = "(\\d{5}-?\\d{3})?", message = "CEP inválido.") String cep,
            @Size(max = 50, message = "O país pode ter até 50 caracteres.") String pais,
            @Size(max = 100, message = "A referência pode ter até 100 caracteres.") String referencia,
            @PositiveOrZero(message = "O número de unidades não pode ser negativo.") Integer numeroUnidades,
            @NotNull(message = "Informe a tipologia.") CondominioTipologia tipologia,
            @Min(value = 1, message = "O dia de vencimento da taxa deve ficar entre 1 e 31.")
            @Max(value = 31, message = "O dia de vencimento da taxa deve ficar entre 1 e 31.")
            Integer diaVencimentoTaxa) {

        public Condominio paraEntidade() {
            Condominio condominio = new Condominio();
            condominio.setConNome(nome.trim());
            condominio.setConLogradouro(aparado(logradouro));
            condominio.setConNumero(aparado(numero));
            condominio.setConComplemento(aparado(complemento));
            condominio.setConBairro(aparado(bairro));
            condominio.setConCidade(aparado(cidade));
            condominio.setConEstado(estado == null ? null : estado.toUpperCase());
            condominio.setConCep(cep == null ? null : cep.replace("-", ""));
            condominio.setConPais(aparado(pais));
            condominio.setConReferencia(aparado(referencia));
            condominio.setConNumeroUnidades(numeroUnidades);
            condominio.setConTipologia(tipologia);
            condominio.setConDtVencimentoTaxa(diaVencimentoTaxa);
            return condominio;
        }

        private static String aparado(String valor) {
            return valor == null ? null : valor.trim();
        }
    }

    public record CondominioResposta(Integer id, String nome, String logradouro, String numero, String complemento,
                                     String bairro, String cidade, String estado, String cep, String pais,
                                     String referencia, Integer numeroUnidades, CondominioTipologia tipologia,
                                     Integer diaVencimentoTaxa, boolean ativo, LocalDateTime dataCadastro,
                                     LocalDateTime dataAtualizacao) {

        public static CondominioResposta de(Condominio c) {
            return new CondominioResposta(c.getConCod(), c.getConNome(), c.getConLogradouro(), c.getConNumero(),
                    c.getConComplemento(), c.getConBairro(), c.getConCidade(), c.getConEstado(), c.getConCep(),
                    c.getConPais(), c.getConReferencia(), c.getConNumeroUnidades(), c.getConTipologia(),
                    c.getConDtVencimentoTaxa(), Boolean.TRUE.equals(c.getConAtivo()), c.getConDtCadastro(),
                    c.getConDtAtualizacao());
        }
    }

    public record OpcoesCondominio(List<Opcao> tipologias, boolean podeGerenciar) {
    }
}
