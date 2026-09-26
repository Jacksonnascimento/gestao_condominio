import api from '@/services/api';
import { limparParametros } from '@/services/utilitarios';

/** ABERTO (do mês), VENCIDO ou PAGO. */
export type SituacaoBoleto = 'ABERTO' | 'VENCIDO' | 'PAGO';

export interface Boleto {
  id: string;
  /** "Unidade 101, Bloco A". */
  unidadeNome: string;
  /** Descrição da cobrança: "Taxa Condominial Mensal". */
  nomeTaxa: string;
  valor: number;
  dataVencimento: string;
  status: SituacaoBoleto | string;
  linhaDigitavel: string | null;
  codigoPix: string | null;
  linkPdf: string | null;
}

export interface CondominioDoBoleto {
  codigo: number;
  nome: string;
  logradouro: string | null;
  numero: string | null;
}

/**
 * Cobranças do condomínio. Ainda é demonstração (`demonstracao` sempre true): os boletos são gerados a cada
 * consulta, com valores e códigos fictícios.
 */
export interface PainelFinanceiro {
  demonstracao: boolean;
  condominio: CondominioDoBoleto;
  podeGerarBoleto: boolean;
  boletosAbertos: Boleto[];
  boletosVencidos: Boleto[];
  historico: Boleto[];
}

export interface UnidadePagadora {
  codigo: number;
  descricao: string;
}

export interface OpcoesFinanceiro {
  condominios: { codigo: number; nome: string }[];
  condominioId: number | null;
  /** Só vem preenchida para quem gera boleto avulso no condomínio. */
  unidades: UnidadePagadora[];
  podeGerarBoleto: boolean;
}

export interface NovoBoletoAvulso {
  unidadeId: number;
  nomeTaxa: string;
  valor: number;
  /** Sem vencimento, vale o último dia do mês. */
  dataVencimento?: string;
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** R$ 1.234,56 */
export function formatarMoeda(valor?: number | null): string {
  return moeda.format(Number(valor ?? 0));
}

/** "1.234,56", "1234,56" ou "1234.56" viram 1234.56; texto que não é valor vira NaN. */
export function lerValorEmReais(texto: string): number {
  const limpo = texto.replace(/[R$\s]/g, '');
  if (!limpo) return NaN;
  const normalizado = limpo.includes(',') ? limpo.replace(/\./g, '').replace(',', '.') : limpo;
  return /^\d+(\.\d{1,2})?$/.test(normalizado) ? Number(normalizado) : NaN;
}

/** Os blocos já vêm cadastrados como "Bloco A": evita o "Bloco Bloco A" montado pela API. */
export function nomeDaUnidade(texto?: string | null): string {
  return (texto ?? '').replace(/Bloco\s+Bloco\s+/gi, 'Bloco ');
}

export const financeiroService = {
  painel: (condominioId?: number | null) =>
    api.get<PainelFinanceiro>('/financeiro', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  opcoes: (condominioId?: number | null) =>
    api.get<OpcoesFinanceiro>('/financeiro/opcoes', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  gerarBoleto: (boleto: NovoBoletoAvulso) => api.post<Boleto>('/financeiro/boletos', boleto).then((r) => r.data),
};
