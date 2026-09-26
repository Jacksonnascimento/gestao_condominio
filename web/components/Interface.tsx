'use client';

import { forwardRef, useId } from 'react';
import { ChevronLeft, ChevronRight, LoaderCircle, Search } from 'lucide-react';

/*
 * Peças de interface usadas em todas as telas, no visual aprovado: botões, campos, filtros, selos, abas, cabeçalho
 * de página, cartões, linhas de detalhe e paginação.
 */

type VarianteDoBotao = 'primario' | 'secundario' | 'texto' | 'perigo';

const CLASSES_DO_BOTAO: Record<VarianteDoBotao, string> = {
  primario: 'bg-tinta text-fundo hover:bg-tinta-2 border border-tinta',
  secundario: 'bg-superficie text-tinta border border-contorno hover:bg-lateral',
  texto: 'bg-transparent text-tinta-2 border border-transparent hover:bg-trilho',
  perigo: 'bg-perigo text-white border border-perigo hover:opacity-90',
};

interface PropsDoBotao extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: VarianteDoBotao;
  pequeno?: boolean;
  carregando?: boolean;
}

export function Botao({ variante = 'secundario', pequeno, carregando, className = '', children, disabled, ...resto }: PropsDoBotao) {
  return (
    <button
      type="button"
      {...resto}
      disabled={disabled || carregando}
      className={`inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-[10px] font-semibold whitespace-nowrap transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
        pequeno ? 'h-[34px] px-3 text-[13px] font-bold' : 'h-[42px] px-4 text-sm'
      } ${CLASSES_DO_BOTAO[variante]} ${className}`}
    >
      {carregando && <LoaderCircle size={16} className="animate-spin" aria-hidden />}
      {children}
    </button>
  );
}

const CLASSE_DO_CAMPO =
  'h-11 w-full rounded-[10px] border border-borda bg-superficie px-3 text-sm text-tinta outline-none transition-colors placeholder:text-apagado focus:border-ouro focus-visible:outline-none';

interface PropsDoRotulo {
  rotulo: string;
  ajuda?: string;
  obrigatorio?: boolean;
  className?: string;
}

function Rotulo({ id, rotulo, obrigatorio }: { id: string; rotulo: string; obrigatorio?: boolean }) {
  return (
    <label htmlFor={id} className="text-[13px] font-semibold text-tinta-2">
      {rotulo}
      {obrigatorio && <span className="text-ouro"> *</span>}
    </label>
  );
}

export const Campo = forwardRef<HTMLInputElement, PropsDoRotulo & React.InputHTMLAttributes<HTMLInputElement>>(
  function Campo({ rotulo, ajuda, obrigatorio, className = '', id, ...resto }, ref) {
    const gerado = useId();
    const idDoCampo = id ?? gerado;
    return (
      <div className={`flex flex-col gap-1.5 ${className}`}>
        <Rotulo id={idDoCampo} rotulo={rotulo} obrigatorio={obrigatorio} />
        <input ref={ref} id={idDoCampo} required={obrigatorio} className={CLASSE_DO_CAMPO} {...resto} />
        {ajuda && <span className="text-xs text-apagado">{ajuda}</span>}
      </div>
    );
  },
);

export function CampoDeSelecao({
  rotulo,
  ajuda,
  obrigatorio,
  className = '',
  id,
  children,
  ...resto
}: PropsDoRotulo & React.SelectHTMLAttributes<HTMLSelectElement>) {
  const gerado = useId();
  const idDoCampo = id ?? gerado;
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <Rotulo id={idDoCampo} rotulo={rotulo} obrigatorio={obrigatorio} />
      <select id={idDoCampo} required={obrigatorio} className={`${CLASSE_DO_CAMPO} cursor-pointer`} {...resto}>
        {children}
      </select>
      {ajuda && <span className="text-xs text-apagado">{ajuda}</span>}
    </div>
  );
}

export function CampoDeTexto({
  rotulo,
  ajuda,
  obrigatorio,
  className = '',
  id,
  ...resto
}: PropsDoRotulo & React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const gerado = useId();
  const idDoCampo = id ?? gerado;
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <Rotulo id={idDoCampo} rotulo={rotulo} obrigatorio={obrigatorio} />
      <textarea
        id={idDoCampo}
        required={obrigatorio}
        rows={3}
        className={`${CLASSE_DO_CAMPO} h-auto min-h-[88px] py-2.5`}
        {...resto}
      />
      {ajuda && <span className="text-xs text-apagado">{ajuda}</span>}
    </div>
  );
}

/** Caixa de marcar com o texto ao lado, clicável por inteiro (alvo de 44px de altura). */
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

/** Aviso de erro no topo de um formulário ou de uma janela. */
export const CaixaDeErro = forwardRef<HTMLParagraphElement, { children: React.ReactNode; className?: string }>(
  function CaixaDeErro({ children, className = '' }, ref) {
    return (
      <p ref={ref} className={`rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo ${className}`} role="alert">
        {children}
      </p>
    );
  },
);

/** Caixa das barras de filtro: altura de 44px no celular e 40px a partir do tablet. */
const CLASSE_DA_CAIXA_DE_FILTRO =
  'flex h-11 items-center gap-2 rounded-[10px] border border-borda bg-superficie px-3 text-sm text-apagado focus-within:border-ouro sm:h-10';

/** Busca com lupa das listagens; o rótulo fica só para leitores de tela. */
export function CampoDeBusca({
  valor,
  aoMudar,
  rotulo,
  dica,
  className = 'w-full sm:w-72',
}: {
  valor: string;
  aoMudar: (valor: string) => void;
  /** Texto para leitores de tela: "Buscar unidades". */
  rotulo: string;
  dica: string;
  /** Largura da caixa; por padrão, a linha toda no celular e 288px a partir do tablet. */
  className?: string;
}) {
  return (
    <label className={`${CLASSE_DA_CAIXA_DE_FILTRO} ${className}`}>
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

const CLASSE_DO_CONTROLE_DO_FILTRO = 'min-w-0 grow border-0 bg-transparent font-semibold text-tinta outline-none';

/** Filtro de escolha compacto, com o rótulo dentro da caixa: "Área  Todas ▾". */
export function FiltroDeSelecao({
  rotulo,
  className = '',
  children,
  ...resto
}: { rotulo: string } & React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <label className={`${CLASSE_DA_CAIXA_DE_FILTRO} ${className}`}>
      <span className="shrink-0">{rotulo}</span>
      <select className={`${CLASSE_DO_CONTROLE_DO_FILTRO} cursor-pointer sm:max-w-52`} {...resto}>
        {children}
      </select>
    </label>
  );
}

/** Filtro de data compacto, com o rótulo dentro da caixa: "De  01/09/2026". */
export function FiltroDeData({
  rotulo,
  className = '',
  ...resto
}: { rotulo: string } & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  return (
    <label className={`${CLASSE_DA_CAIXA_DE_FILTRO} ${className}`}>
      <span className="shrink-0">{rotulo}</span>
      <input type="date" className={CLASSE_DO_CONTROLE_DO_FILTRO} {...resto} />
    </label>
  );
}

export type TomDoSelo = 'aviso' | 'neutro' | 'info' | 'perigo';

const CLASSES_DO_SELO: Record<TomDoSelo, string> = {
  aviso: 'bg-aviso-fundo text-aviso',
  neutro: 'bg-neutro-fundo text-neutro',
  info: 'bg-info-fundo text-info',
  perigo: 'bg-perigo-fundo text-perigo',
};

/** Selo de situação ("Aguardando", "Retirada"...). */
export function Selo({ tom, children }: { tom: TomDoSelo; children: React.ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-[3px] text-xs font-bold whitespace-nowrap ${CLASSES_DO_SELO[tom]}`}>
      {children}
    </span>
  );
}

export interface Aba<T extends string> {
  valor: T;
  rotulo: string;
  contagem?: number | null;
  /** Contagem em destaque (pendências). */
  destaque?: boolean;
}

/** Abas de filtro no formato de trilho (Todas · Aguardando · Retiradas...). */
export function Abas<T extends string>({
  abas,
  valor,
  aoMudar,
  rotulo,
}: {
  abas: Aba<T>[];
  valor: T;
  aoMudar: (valor: T) => void;
  rotulo: string;
}) {
  return (
    <div role="group" aria-label={rotulo} className="flex max-w-full gap-0.5 overflow-x-auto rounded-xl bg-trilho p-[3px] [scrollbar-width:none]">
      {abas.map((aba) => {
        const ativa = aba.valor === valor;
        return (
          <button
            key={aba.valor}
            type="button"
            aria-pressed={ativa}
            onClick={() => aoMudar(aba.valor)}
            className={`flex h-9 shrink-0 cursor-pointer items-center gap-2 rounded-[9px] px-3.5 text-sm ${
              ativa ? 'bg-superficie font-bold text-tinta shadow-[0_1px_2px_rgba(31,27,22,0.12)]' : 'font-semibold text-tinta-2 hover:text-tinta'
            }`}
          >
            {aba.rotulo}
            {aba.contagem != null &&
              (aba.destaque && aba.contagem > 0 ? (
                <span className="rounded-full bg-aviso-fundo px-[7px] py-px text-xs font-bold text-aviso">{aba.contagem}</span>
              ) : (
                <span className="text-xs text-apagado">{aba.contagem}</span>
              ))}
          </button>
        );
      })}
    </div>
  );
}

/** Título da tela, com a seção acima e as ações à direita. */
export function CabecalhoDaPagina({
  secao,
  titulo,
  children,
}: {
  secao?: string;
  titulo: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div className="flex flex-col gap-1.5">
        {secao && <span className="text-sm text-apagado">{secao}</span>}
        <h1 className="font-titulo text-[36px] leading-none font-normal sm:text-[42px]">{titulo}</h1>
      </div>
      {children && <div className="flex flex-wrap gap-2.5">{children}</div>}
    </div>
  );
}

/** Cartão branco com borda, base das seções das telas. */
export function Cartao({ className = '', children, ...resto }: React.HTMLAttributes<HTMLElement>) {
  return (
    <section className={`overflow-hidden rounded-2xl border border-borda bg-superficie ${className}`} {...resto}>
      {children}
    </section>
  );
}

/** Cabeçalho de um cartão: título e um link ou ação à direita. */
export function TituloDoCartao({ titulo, children }: { titulo: string; children?: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 px-5 py-3.5">
      <h2 className="text-base font-extrabold">{titulo}</h2>
      {children && <span className="shrink-0 whitespace-nowrap">{children}</span>}
    </div>
  );
}

/** Mensagem no lugar de uma lista vazia. */
export function Vazio({ children }: { children: React.ReactNode }) {
  return <p className="px-5 py-8 text-center text-sm text-apagado">{children}</p>;
}

/**
 * Uma linha "rótulo: valor" das janelas de detalhes, dentro de um <dl>. O valor vem em `valor` ou como conteúdo; a
 * linha some quando não há nenhum dos dois. Quebras de linha do texto (observações) são mantidas.
 */
export function LinhaDeDetalhe({ rotulo, valor, children }: { rotulo: string; valor?: React.ReactNode; children?: React.ReactNode }) {
  const conteudo = children ?? valor;
  if (conteudo == null || conteudo === '') return null;
  return (
    <div className="grid grid-cols-[120px_minmax(0,1fr)] gap-3 py-2.5 text-sm sm:grid-cols-[140px_minmax(0,1fr)]">
      <dt className="text-apagado">{rotulo}</dt>
      <dd className="m-0 break-words whitespace-pre-line">{conteudo}</dd>
    </div>
  );
}

export function Paginacao({
  pagina,
  totalPaginas,
  totalItens,
  tamanho,
  aoMudar,
  nome,
}: {
  pagina: number;
  totalPaginas: number;
  totalItens: number;
  tamanho: number;
  aoMudar: (pagina: number) => void;
  /** Plural do que está sendo listado: "encomendas". */
  nome: string;
}) {
  if (totalItens === 0) return null;
  const inicio = pagina * tamanho + 1;
  const fim = Math.min(totalItens, inicio + tamanho - 1);
  const botao =
    'flex size-[34px] cursor-pointer items-center justify-center rounded-lg text-tinta hover:bg-trilho disabled:cursor-not-allowed disabled:text-apagado disabled:opacity-50';
  return (
    <div className="flex items-center justify-between gap-3 border-t border-borda-suave px-5 py-3 text-[13px] text-apagado">
      <span>
        {inicio}–{fim} de {totalItens} {nome}
      </span>
      <nav aria-label="Páginas" className="flex items-center gap-1">
        <button type="button" className={botao} aria-label="Página anterior" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>
          <ChevronLeft size={17} aria-hidden />
        </button>
        <span className="px-1.5 font-semibold text-tinta">
          Página {pagina + 1} de {Math.max(totalPaginas, 1)}
        </span>
        <button
          type="button"
          className={botao}
          aria-label="Próxima página"
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => aoMudar(pagina + 1)}
        >
          <ChevronRight size={17} aria-hidden />
        </button>
      </nav>
    </div>
  );
}
