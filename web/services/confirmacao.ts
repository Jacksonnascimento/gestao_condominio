export interface PedidoDeConfirmacao {
  titulo: string;
  mensagem: string;
  textoConfirmar?: string;
  /** Ação que apaga ou desfaz algo: o botão de confirmar fica em destaque de perigo. */
  perigo?: boolean;
}

type Ouvinte = (pedido: PedidoDeConfirmacao, responder: (resposta: boolean) => void) => void;

let ouvinte: Ouvinte | null = null;

/** Usado pelo ConfirmacaoDoSistema, montado uma vez no layout. */
export function registrarConfirmacao(novo: Ouvinte | null) {
  ouvinte = novo;
}

/** Pergunta ao usuário antes de uma ação; resolve true se ele confirmar. Substitui o confirm() do navegador. */
export function confirmar(pedido: PedidoDeConfirmacao): Promise<boolean> {
  return new Promise((resolve) => {
    if (!ouvinte) {
      resolve(false);
      return;
    }
    ouvinte(pedido, resolve);
  });
}
