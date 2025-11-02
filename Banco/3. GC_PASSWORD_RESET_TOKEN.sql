
--Tabela de Tokens para Reset de Senha
CREATE TABLE GC_PASSWORD_RESET_TOKEN (
    PRT_TOKEN VARCHAR(255) NOT NULL,
    PES_COD INT NOT NULL,
    PRT_EXPIRACAO TIMESTAMP NOT NULL,
    
    PRIMARY KEY (PRT_TOKEN),
    FOREIGN KEY (PES_COD) REFERENCES GC_PESSOA (PES_COD) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_pes_cod ON gc_password_reset_token (pes_cod);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expiracao ON gc_password_reset_token (prt_expiracao);