'use client';

import { useEffect, useState } from 'react';
import { Botao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { registrarConfirmacao, type PedidoDeConfirmacao } from '@/services/confirmacao';

/** Janela de confirmação usada por `confirmar()`, montada uma vez no layout. */
export function ConfirmacaoDoSistema() {
  const [pedido, setPedido] = useState<{ dados: PedidoDeConfirmacao; responder: (r: boolean) => void } | null>(null);

  useEffect(() => {
    registrarConfirmacao((dados, responder) => setPedido({ dados, responder }));
    return () => registrarConfirmacao(null);
  }, []);

  if (!pedido) return null;
  const responder = (resposta: boolean) => {
    pedido.responder(resposta);
    setPedido(null);
  };

  return (
    <Modal
      titulo={pedido.dados.titulo}
      largura="sm"
      aoFechar={() => responder(false)}
      rodape={
        <>
          <Botao variante="texto" onClick={() => responder(false)}>
            {pedido.dados.textoDesistir ?? 'Cancelar'}
          </Botao>
          <Botao variante={pedido.dados.perigo ? 'perigo' : 'primario'} onClick={() => responder(true)}>
            {pedido.dados.textoConfirmar ?? 'Confirmar'}
          </Botao>
        </>
      }
    >
      <p className="text-sm leading-relaxed text-tinta-2">{pedido.dados.mensagem}</p>
    </Modal>
  );
}
