package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.AdminCompanyRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor; 
import lombok.Data;       
import lombok.NoArgsConstructor; 
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_ADMINISTRADORA_USUARIO", schema = "dbo")
public class AdministradoraUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADU_COD")
    private Integer aduCod;

    @ManyToOne
    @JoinColumn(name = "ADM_COD", nullable = false)
    private Administradora administradora;

    @ManyToOne
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "ADU_PAPEL", nullable = false, length = 50)
    private AdminCompanyRole aduPapel;

    @Column(name = "ADU_DT_CADASTRO", nullable = false)
    private LocalDateTime aduDtCadastro;

    @Column(name = "ADU_DT_ATUALIZACAO")
    private LocalDateTime aduDtAtualizacao;

    @Column(name = "ADU_ATIVO", nullable = false)
    private Boolean aduAtivo;
}