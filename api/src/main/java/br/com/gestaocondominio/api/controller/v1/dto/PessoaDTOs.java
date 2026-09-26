package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import jakarta.validation.constraints.NotBlank;

/** Pedidos e respostas de {@code /api/v1/pessoas}. */
public final class PessoaDTOs {

    private PessoaDTOs() {
    }

    /** O CPF/CNPJ pode vir com ou sem pontuação. */
    public record ConsultaPorDocumentoRequest(@NotBlank(message = "Informe o CPF/CNPJ.") String cpfCnpj) {
    }

    /** O que o formulário de ocupante preenche quando o CPF/CNPJ já tem cadastro. */
    public record PessoaResumo(Integer id, String nome, Character tipo, String email, String telefone) {

        public static PessoaResumo de(Pessoa pessoa) {
            return new PessoaResumo(pessoa.getPesCod(), pessoa.getPesNome(), pessoa.getPesTipo(),
                    pessoa.getPesEmail(), pessoa.getPesTelefone());
        }
    }
}
