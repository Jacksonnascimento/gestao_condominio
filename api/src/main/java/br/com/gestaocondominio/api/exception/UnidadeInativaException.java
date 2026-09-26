package br.com.gestaocondominio.api.exception;

/**
 * Tentativa de cadastrar uma unidade que já existe, mas está inativa. A mensagem mantém o formato
 * {@code UNIDADE_INATIVA:<id>} que a tela antiga de unidades lê para oferecer a reativação; a API v1 devolve o
 * {@link #getUnidadeId() id} num campo próprio.
 */
public class UnidadeInativaException extends ConflitoException {

    private final Integer unidadeId;

    public UnidadeInativaException(Integer unidadeId) {
        super("UNIDADE_INATIVA:" + unidadeId);
        this.unidadeId = unidadeId;
    }

    public Integer getUnidadeId() {
        return unidadeId;
    }
}
