import axios from 'axios';
import { format, isToday, isYesterday, parseISO } from 'date-fns';
import { ptBR } from 'date-fns/locale';

/**
 * Mensagem para mostrar ao usuário quando uma chamada falha. A API responde sempre no mesmo formato
 * ({ status, message, ... }) e as mensagens dela já são escritas para quem usa o sistema; quando não há resposta
 * (API fora do ar, rede), fica a mensagem padrão da tela.
 */
export function mensagemErroApi(erro: unknown, padrao: string): string {
  if (axios.isAxiosError(erro)) {
    const mensagem = (erro.response?.data as { message?: unknown } | undefined)?.message;
    if (typeof mensagem === 'string' && mensagem.trim() && erro.response?.status !== 500) {
      return mensagem;
    }
    if (!erro.response) {
      return 'Sem conexão com o sistema. Confira a internet e tente de novo.';
    }
  }
  return padrao;
}

/** Status da resposta de erro da API, ou undefined quando não houve resposta. */
export function statusDoErro(erro: unknown): number | undefined {
  return axios.isAxiosError(erro) ? erro.response?.status : undefined;
}

/**
 * Parte da API manda as situações como texto ("PENDENTE") e parte como objeto ({ nome, descricao }). Até que
 * isso seja padronizado, as telas leem pelos dois helpers abaixo.
 */
export type Enumerado = string | { nome: string; descricao?: string } | null | undefined;

export function valorDoEnum(valor: Enumerado): string {
  if (!valor) return '';
  return typeof valor === 'string' ? valor : valor.nome;
}

export function descricaoDoEnum(valor: Enumerado, reserva?: string | null): string {
  if (valor && typeof valor === 'object' && valor.descricao) return valor.descricao;
  return reserva || textoLegivelDeCodigo(valorDoEnum(valor));
}

/** "EM_ANALISE" vira "Em analise": para códigos que ainda chegam sem descrição. */
export function textoLegivelDeCodigo(codigo: string): string {
  if (!codigo) return '';
  const texto = codigo.replace(/_/g, ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function paraData(valor: string | Date): Date {
  return typeof valor === 'string' ? parseISO(valor) : valor;
}

/** 26/09/2026 */
export function formatarData(valor?: string | Date | null): string {
  return valor ? format(paraData(valor), 'dd/MM/yyyy') : '';
}

/** 26/09/2026 às 09:14 */
export function formatarDataHora(valor?: string | Date | null): string {
  return valor ? format(paraData(valor), "dd/MM/yyyy 'às' HH:mm") : '';
}

/** "Hoje, 09:14", "Ontem, 19:48" ou "24/09, 11:20": para listas do dia a dia. */
export function formatarMomento(valor?: string | Date | null): string {
  if (!valor) return '';
  const data = paraData(valor);
  if (isToday(data)) return `Hoje, ${format(data, 'HH:mm')}`;
  if (isYesterday(data)) return `Ontem, ${format(data, 'HH:mm')}`;
  return format(data, 'dd/MM, HH:mm');
}

/** Dia da semana abreviado, em maiúsculas: "DOM". */
export function diaDaSemanaCurto(valor: string | Date): string {
  return format(paraData(valor), 'EEE', { locale: ptBR }).replace('.', '').toUpperCase();
}

/** Data de hoje no formato dos campos de data (2026-09-26). */
export function hojeParaCampo(): string {
  return format(new Date(), 'yyyy-MM-dd');
}

/** Hora atual no formato dos campos de hora (09:14). */
export function agoraParaCampo(): string {
  return format(new Date(), 'HH:mm');
}

/** "302 · Bloco A", ou só o número quando a unidade não tem bloco. */
export function rotuloUnidade(numero?: string | null, bloco?: string | null): string {
  if (!numero) return '';
  return bloco ? `${numero} · ${bloco}` : numero;
}

/** Iniciais para o avatar: "Marina Costa" vira "MC". */
export function iniciais(nome?: string | null): string {
  const partes = (nome || '').trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return '';
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
  return (primeira + ultima).toUpperCase();
}

/** Remove os filtros vazios antes de mandar para a API. */
export function limparParametros<T extends Record<string, unknown>>(parametros: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(parametros).filter(([, valor]) => valor !== '' && valor !== null && valor !== undefined),
  ) as Partial<T>;
}
