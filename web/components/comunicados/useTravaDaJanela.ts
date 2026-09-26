'use client';

import { useCallback, useRef, useState } from 'react';

/**
 * Trava uma janela (Modal) enquanto algo está em andamento nela: um envio, ou uma confirmação aberta por cima.
 * O Modal guarda o `aoFechar` e o `ocupado` do momento em que abriu para tratar o Esc; por isso a trava fica numa
 * ref, lida na hora em que a pessoa tenta fechar, e não só no estado.
 *
 * Uso: `<Modal aoFechar={fechar} ocupado={travada}>`, e `travar(true)` / `travar(false)` em volta da ação.
 */
export function useTravaDaJanela(aoFechar: () => void) {
  const [travada, setTravadaNaTela] = useState(false);
  const trava = useRef(false);
  const travar = useCallback((valor: boolean) => {
    trava.current = valor;
    setTravadaNaTela(valor);
  }, []);
  const fechar = useCallback(() => {
    if (!trava.current) aoFechar();
  }, [aoFechar]);
  return { travada, travar, fechar };
}
