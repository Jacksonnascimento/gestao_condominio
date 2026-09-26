'use client';

import { useId } from 'react';

/**
 * Caixa de marcar com o texto ao lado, clicável por inteiro (alvo de 44px de altura). Candidata a ir para o
 * Interface.tsx: as telas de reservas e de áreas comuns usam.
 */
export function CaixaDeMarcar({
  rotulo,
  ajuda,
  className = '',
  id,
  ...resto
}: { rotulo: React.ReactNode; ajuda?: string } & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const gerado = useId();
  const idDoCampo = id ?? gerado;
  return (
    <div className={`flex flex-col ${className}`}>
      <label htmlFor={idDoCampo} className="flex min-h-11 cursor-pointer items-center gap-3 text-sm font-semibold text-tinta-2">
        <input id={idDoCampo} type="checkbox" className="size-[18px] shrink-0 cursor-pointer accent-ouro" {...resto} />
        <span>{rotulo}</span>
      </label>
      {ajuda && <span className="-mt-1.5 pl-[30px] text-xs text-apagado">{ajuda}</span>}
    </div>
  );
}
