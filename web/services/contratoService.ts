import api from '@/services/api';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros, type Enumerado } from '@/services/utilitarios';

/**
 * A situação é calculada pela API a partir da data de fim: ATIVO, A_VENCER (fim nos próximos 30 dias) ou
 * FINALIZADO (fim já passou). Só RESCINDIDO é gravado como escolhido.
 */
export type SituacaoContrato = 'ATIVO' | 'A_VENCER' | 'FINALIZADO' | 'RESCINDIDO';

/** Abas da listagem: ATIVOS, A_VENCER e HISTORICO (finalizados e rescindidos). */
export type AbaContrato = 'ATIVOS' | 'A_VENCER' | 'HISTORICO';

export interface Contrato {
  id: number;
  condominioCodigo: number | null;
  condominioNome: string | null;
  empresa: string;
  servico: string;
  valor: number;
  responsavel: string | null;
  status: Enumerado;
  statusDescricao: string | null;
  /** 2026-01-31 */
  dataInicio: string;
  dataFim: string;
  observacoes: string | null;
  dataCadastro: string | null;
  dataAtualizacao: string | null;
}

export interface FiltroContratos {
  aba: AbaContrato;
  condominioId?: number | null;
  /** Procura na empresa e no serviço. */
  busca?: string;
  /** Só vale na aba de histórico: FINALIZADO ou RESCINDIDO. */
  status?: SituacaoContrato | '';
  inicioApos?: string;
  fimAntes?: string;
  pagina?: number;
  tamanho?: number;
}

export interface TotaisContratos {
  total: number;
  ativos: number;
  aVencer: number;
  finalizados: number;
  rescindidos: number;
}

export interface OpcoesContrato {
  condominios: { codigo: number; nome: string }[];
  abas: Opcao[];
  status: Opcao[];
  statusDoHistorico: Opcao[];
  statusDeCadastro: Opcao[];
  /** Quem está logado gerencia contratos em algum condomínio (síndico, administração, administrador geral). */
  podeGerenciar: boolean;
}

/** Cadastro e edição. O condomínio só vale no cadastro. */
export interface PedidoContrato {
  condominioId?: number;
  empresa: string;
  servico: string;
  valor: number;
  responsavel?: string | null;
  /** RESCINDIDO fica gravado; qualquer outro valor deixa a situação ser calculada pela data de fim. */
  status?: SituacaoContrato | null;
  dataInicio: string;
  dataFim: string;
  observacoes?: string | null;
}

export const contratoService = {
  /** Contratos de uma aba, por data de início e de fim. */
  listar: (filtro: FiltroContratos) =>
    api.get<Pagina<Contrato>>('/contratos', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidades por situação no condomínio (sem os demais filtros). */
  /** Contratos por situação, com a mesma busca da lista. */
  totais: (condominioId?: number | null, busca?: string) =>
    api.get<TotaisContratos>('/contratos/totais', { params: limparParametros({ condominioId, busca }) }).then((r) => r.data),

  opcoes: () => api.get<OpcoesContrato>('/contratos/opcoes').then((r) => r.data),

  buscar: (id: number) => api.get<Contrato>(`/contratos/${id}`).then((r) => r.data),

  criar: (pedido: PedidoContrato) => api.post<Contrato>('/contratos', pedido).then((r) => r.data),

  atualizar: (id: number, pedido: PedidoContrato) => api.put<Contrato>(`/contratos/${id}`, pedido).then((r) => r.data),

  excluir: (id: number) => api.delete<void>(`/contratos/${id}`).then(() => undefined),
};

/** Os dados do contrato no formato do pedido de edição, para mudar só a situação. */
export function pedidoDoContrato(contrato: Contrato, status: SituacaoContrato | null): PedidoContrato {
  return {
    empresa: contrato.empresa,
    servico: contrato.servico,
    valor: contrato.valor,
    responsavel: contrato.responsavel,
    status,
    dataInicio: contrato.dataInicio,
    dataFim: contrato.dataFim,
    observacoes: contrato.observacoes,
  };
}
