package br.com.gestaocondominio.api.exception;

/**
 * O pedido esbarra no estado atual do registro (ex.: inativar um condomínio que ainda tem unidades, ou cadastrar um
 * registro que já existe). Vira 409 com a mensagem.
 */
public class ConflitoException extends IllegalStateException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
