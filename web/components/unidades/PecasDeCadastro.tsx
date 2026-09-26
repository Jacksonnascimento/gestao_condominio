'use client';

import { useId } from 'react';
import { Search } from 'lucide-react';

/*
 * Peças pequenas das telas de cadastro (unidades e ocupantes) que ainda não existem em Interface.tsx. Se outras
 * telas precisarem delas, o lugar certo é lá.
 */

/** Uma linha "rótulo: valor" das janelas de detalhes; some quando não há valor. Use dentro de um <dl>. */
export function LinhaDeDetalhe({ rotulo, valor }: { rotulo: string; valor?: React.ReactNode }) {
  if (valor == null || valor === '') return null;
  return (
    <div className="grid grid-cols-[140px_minmax(0,1fr)] gap-3 py-2.5 text-sm">
      <dt className="text-apagado">{rotulo}</dt>
      <dd className="m-0 break-words">{valor}</dd>
    </div>
  );
}

/** Aviso de erro no topo de um formulário. */
export function ErroDoFormulario({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo ${className}`} role="alert">
      {children}
    </div>
  );
}

/** Caixa de marcar com o texto ao lado; a área de toque tem 44px de altura. */
export function CaixaDeMarcar({
  rotulo,
  marcada,
  aoMudar,
  className = '',
}: {
  rotulo: string;
  marcada: boolean;
  aoMudar: (marcada: boolean) => void;
  className?: string;
}) {
  const id = useId();
  return (
    <label htmlFor={id} className={`flex min-h-11 cursor-pointer items-center gap-2.5 text-sm font-semibold text-tinta-2 ${className}`}>
      <input
        id={id}
        type="checkbox"
        checked={marcada}
        onChange={(e) => aoMudar(e.target.checked)}
        className="size-[18px] cursor-pointer accent-tinta"
      />
      {rotulo}
    </label>
  );
}

/** Campo de busca com lupa, no mesmo visual da tela de encomendas. */
export function CampoDeBusca({
  valor,
  aoMudar,
  rotulo,
  dica,
}: {
  valor: string;
  aoMudar: (valor: string) => void;
  /** Texto para leitores de tela: "Buscar unidades". */
  rotulo: string;
  dica: string;
}) {
  return (
    <label className="flex h-11 w-full items-center gap-2 rounded-[10px] border border-borda bg-superficie px-3 text-apagado sm:h-10 sm:w-72">
      <Search size={16} aria-hidden />
      <span className="sr-only">{rotulo}</span>
      <input
        type="search"
        value={valor}
        onChange={(e) => aoMudar(e.target.value)}
        placeholder={dica}
        className="min-w-0 grow border-0 bg-transparent text-sm text-tinta outline-none placeholder:text-apagado"
      />
    </label>
  );
}

/** "Em Reforma" vira "Em reforma": as descrições da API vêm com iniciais maiúsculas em cada palavra. */
export function emFrase(texto?: string | null): string {
  if (!texto) return '';
  return texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase();
}

/** Número no formato brasileiro, sem casas decimais sobrando: 72,5 · 1.250 · 0,35. */
export function formatarNumero(valor?: number | null, casas = 2): string {
  if (valor == null) return '';
  return Number(valor).toLocaleString('pt-BR', { maximumFractionDigits: casas });
}
