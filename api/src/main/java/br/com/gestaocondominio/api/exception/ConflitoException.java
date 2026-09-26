package br.com.gestaocondominio.api.exception;

/**
 * O pedido esbarra no estado atual do registro (ex.: inativar um condomínio que ainda tem unidades). Na API v1 vira
 * 409 com a mensagem. Estende {@link IllegalStateException} para que as telas antigas, que já tratavam esses casos
 * como {@code IllegalStateException}, continuem se comportando igual.
 */
public class ConflitoException extends IllegalStateException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
