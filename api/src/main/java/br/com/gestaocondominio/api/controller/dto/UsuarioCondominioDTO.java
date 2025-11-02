package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioCondominioDTO {
    private Integer pessoaId;
    private String pessoaNome;
    private String pessoaEmail;
    private Integer condominioId;
    private String condominioNome;
    private UserRole papel;
    private String papelDescricao;
    private LocalDateTime dataAssociacao;
    private Boolean ativo;

    public UsuarioCondominioDTO(UsuarioCondominio uc) {
        if (uc.getPessoa() != null) {
            this.pessoaId = uc.getPessoa().getPesCod();
            this.pessoaNome = uc.getPessoa().getPesNome();
            this.pessoaEmail = uc.getPessoa().getPesEmail();
        }
        if (uc.getCondominio() != null) {
            this.condominioId = uc.getCondominio().getConCod();
            this.condominioNome = uc.getCondominio().getConNome();
        }
        this.papel = uc.getUscPapel();
        this.papelDescricao = uc.getUscPapel().getDescricao(); 
        this.dataAssociacao = uc.getUscDtAssociacao();
        this.ativo = uc.getUscAtivoAssociacao();
    }
}