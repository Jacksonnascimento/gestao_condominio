package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "GC_PASSWORD_RESET_TOKEN")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @Column(name = "PRT_TOKEN")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa pessoa;

    @Column(name = "PRT_EXPIRACAO", nullable = false)
    private LocalDateTime expiracao;

    public PasswordResetToken(String token, Pessoa pessoa, int expiryHours) {
        this.token = token;
        this.pessoa = pessoa;
        this.expiracao = LocalDateTime.now().plusHours(expiryHours);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiracao);
    }
}