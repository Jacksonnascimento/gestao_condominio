package br.com.gestaocondominio.api.exception;

/**
 * Tentativa de cadastrar uma unidade que já existe, mas está inativa. A resposta 409 leva o
 * {@link #getUnidadeId() id} num campo próprio, para a tela oferecer a reativação.
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
