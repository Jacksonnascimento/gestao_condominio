-- Colunas e tipos que o código já usava, mas que só existiam nos bancos criados pelo antigo ddl-auto=update.
-- Com elas, o Hibernate passa a apenas validar a estrutura (ddl-auto=validate), e quem muda o banco é o Flyway.

ALTER TABLE gc_condominio ADD COLUMN con_dia_geracao_cobranca INTEGER;
ALTER TABLE gc_condominio ADD COLUMN con_geracao_auto_ativa BOOLEAN;

ALTER TABLE gc_contrato ALTER COLUMN ctr_cod TYPE BIGINT;
