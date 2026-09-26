'use client';

import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

/** Janelas abertas, da de baixo para a de cima. */
const janelasAbertas: string[] = [];

/**
 * Janela sobre a tela, com título, conteúdo e rodapé de ações. Fecha no Esc, no X e ao clicar fora; enquanto
 * `ocupado` (salvando), não fecha.
 */
export function Modal({
  titulo,
  subtitulo,
  aoFechar,
  ocupado,
  rodape,
  largura = 'md',
  children,
}: {
  titulo: string;
  subtitulo?: string;
  aoFechar: () => void;
  ocupado?: boolean;
  rodape?: React.ReactNode;
  largura?: 'sm' | 'md' | 'lg';
  children: React.ReactNode;
}) {
  const idDoTitulo = useId();
  const caixa = useRef<HTMLDivElement>(null);
  // O Esc lê sempre o estado atual (salvando ou não) e a função de fechar mais recente
  const atual = useRef({ ocupado, aoFechar });
  useEffect(() => {
    atual.current = { ocupado, aoFechar };
  });

  useEffect(() => {
    janelasAbertas.push(idDoTitulo);
    const anterior = document.activeElement as HTMLElement | null;
    const primeiro = caixa.current?.querySelector<HTMLElement>('input, select, textarea, button:not([data-fechar])');
    primeiro?.focus();
    const aoTeclar = (evento: KeyboardEvent) => {
      // Com uma janela sobre a outra (uma confirmação, por exemplo), o Esc fecha só a de cima
      const deCima = janelasAbertas[janelasAbertas.length - 1] === idDoTitulo;
      if (evento.key === 'Escape' && deCima && !atual.current.ocupado) atual.current.aoFechar();
    };
    document.addEventListener('keydown', aoTeclar);
    const rolagem = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      janelasAbertas.splice(janelasAbertas.indexOf(idDoTitulo), 1);
      document.removeEventListener('keydown', aoTeclar);
      document.body.style.overflow = rolagem;
      anterior?.focus();
    };
  }, [idDoTitulo]);

  const larguras = { sm: 'max-w-md', md: 'max-w-xl', lg: 'max-w-3xl' };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-6">
      <button
        type="button"
        aria-label="Fechar"
        tabIndex={-1}
        className="absolute inset-0 cursor-default bg-veu/45"
        onClick={() => !ocupado && aoFechar()}
      />
      <div
        ref={caixa}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idDoTitulo}
        className={`relative flex max-h-[92vh] w-full ${larguras[largura]} flex-col rounded-t-2xl bg-superficie shadow-2xl sm:rounded-2xl`}
      >
        <div className="flex items-start justify-between gap-4 border-b border-borda-suave px-6 pt-5 pb-4">
          <div className="flex flex-col gap-1">
            <h2 id={idDoTitulo} className="font-titulo text-[28px] leading-tight font-normal">
              {titulo}
            </h2>
            {subtitulo && <p className="text-sm text-apagado">{subtitulo}</p>}
          </div>
          <button
            type="button"
            data-fechar
            aria-label="Fechar"
            disabled={ocupado}
            onClick={aoFechar}
            className="-mr-2 flex size-10 shrink-0 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho"
          >
            <X size={19} aria-hidden />
          </button>
        </div>
        <div className="overflow-y-auto px-6 py-5">{children}</div>
        {rodape && <div className="flex flex-wrap justify-end gap-2.5 border-t border-borda-suave px-6 py-4">{rodape}</div>}
      </div>
    </div>,
    document.body,
  );
}
