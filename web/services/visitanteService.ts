import api from '@/services/api';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros, type Enumerado } from '@/services/utilitarios';

/** NO_LOCAL: está no condomínio (sem saída registrada). SAIU: já saiu. */
export type SituacaoVisitante = 'NO_LOCAL' | 'SAIU';

/** Visitante como vem na listagem: sem CPF, RG nem observações. */
export interface VisitanteResumo {
  id: number;
  nome: string;
  telefone: string | null;
  status: Enumerado;
  statusDescricao: string | null;
  dataEntrada: string;
  dataSaida: string | null;
  condominioId: number;
  condominioNome: string | null;
  unidadeId: number;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  moradorId: number | null;
  moradorNome: string | null;
  /** Quem está logado pode editar o registro e dar a saída (administração e portaria do condomínio). */
  podeAlterar: boolean;
}

/** Um visitante. CPF, RG e observações vêm só para quem pode alterar o registro; para o morador, nulos. */
export interface VisitanteDetalhe extends VisitanteResumo {
  cpf: string | null;
  rg: string | null;
  observacoes: string | null;
}

export interface FiltroVisitantes {
  condominioId?: number | null;
  busca?: string;
  unidadeId?: number | null;
  status?: SituacaoVisitante | '';
  pagina?: number;
  tamanho?: number;
}

/** TOTAL de registros, NO_LOCAL (no condomínio agora), DO_DIA (entradas hoje) e SAIDAS_DIA (saídas hoje). */
export interface TotaisVisitantes {
  TOTAL: number;
  NO_LOCAL: number;
  DO_DIA: number;
  SAIDAS_DIA: number;
}

export interface UnidadeDoVisitante {
  id: number;
  numero: string;
  bloco: string | null;
  condominioId: number;
}

export interface OpcoesVisitante {
  status: Opcao[];
  condominios: { id: number; nome: string }[];
  /** Unidades ativas do condomínio pedido (as que quem está logado gerencia ou ocupa). */
  unidades: UnidadeDoVisitante[];
  /** Quem está logado registra visitantes (administração ou portaria). */
  podeGerenciar: boolean;
}

/** Ocupante da unidade que pode ter autorizado a entrada. */
export interface MoradorDaUnidade {
  id: number;
  nome: string;
  vinculo: string | null;
}

/** Entrada ou edição. O condomínio é o da unidade. */
export interface PedidoVisitante {
  nome: string;
  cpf?: string;
  rg?: string;
  telefone?: string;
  unidadeId: number;
  moradorId?: number | null;
  observacoes?: string;
}

export const visitanteService = {
  /** Das entradas mais recentes para as mais antigas. A busca procura no nome do visitante. */
  listar: (filtro: FiltroVisitantes) =>
    api.get<Pagina<VisitanteResumo>>('/visitantes', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidades com os mesmos filtros da listagem (sem a situação). */
  totais: (filtro: Omit<FiltroVisitantes, 'status' | 'pagina' | 'tamanho'>) =>
    api.get<TotaisVisitantes>('/visitantes/totais', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  opcoes: (condominioId?: number | null) =>
    api
      .get<OpcoesVisitante>('/visitantes/opcoes', { params: limparParametros({ condominioId }) })
      .then((r) => r.data),

  moradores: (unidadeId: number) =>
    api.get<MoradorDaUnidade[]>('/visitantes/opcoes/moradores', { params: { unidadeId } }).then((r) => r.data),

  buscar: (id: number) => api.get<VisitanteDetalhe>(`/visitantes/${id}`).then((r) => r.data),

  registrarEntrada: (pedido: PedidoVisitante) => api.post<VisitanteDetalhe>('/visitantes', pedido).then((r) => r.data),

  editar: (id: number, pedido: PedidoVisitante) =>
    api.put<VisitanteDetalhe>(`/visitantes/${id}`, pedido).then((r) => r.data),

  registrarSaida: (id: number) => api.post<VisitanteDetalhe>(`/visitantes/${id}/saida`).then((r) => r.data),
};
