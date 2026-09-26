import api from '@/services/api';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros, type Enumerado } from '@/services/utilitarios';

export type SituacaoEncomenda = 'PENDENTE' | 'RETIRADA' | 'DEVOLVIDA' | 'EXTRAVIADA';

export interface Encomenda {
  id: number;
  condominioNome: string | null;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  destinatario: string;
  tipo: string;
  tipoDescricao: string | null;
  descricao: string | null;
  status: Enumerado;
  statusDescricao: string | null;
  dataRecebimento: string;
  nomeRecebidoPor: string | null;
  observacoes: string | null;
  dataRetirada: string | null;
  nomeRetirada: string | null;
  observacaoAtualizacao: string | null;
}

export interface FiltroEncomendas {
  condominioId?: number | null;
  busca?: string;
  status?: SituacaoEncomenda | '';
  pagina?: number;
  tamanho?: number;
}

export interface TotaisEncomendas {
  TOTAL: number;
  PENDENTES: number;
  RETIRADAS: number;
  DEVOLVIDAS: number;
  EXTRAVIADAS: number;
}

export interface OpcoesEncomenda {
  tipos: Opcao[];
  status: Opcao[];
  statusDeAtualizacao: Opcao[];
  /** Quem está logado registra e dá baixa em encomendas (síndico, administração, portaria). */
  podeGerenciar: boolean;
}

export interface NovaEncomenda {
  condominioId: number;
  unidadeId: number;
  destinatario: string;
  tipo: string;
  descricao?: string;
  dataRecebimento: string;
  horaRecebimento: string;
  nomeRecebidoPor: string;
  observacoes?: string;
}

export interface Retirada {
  dataRetirada: string;
  horaRetirada: string;
  nomeRetirada: string;
}

export const encomendaService = {
  listar: (filtro: FiltroEncomendas) =>
    api.get<Pagina<Encomenda>>('/encomendas', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidades por situação, com os mesmos filtros da listagem (sem a situação). */
  totais: (filtro: Omit<FiltroEncomendas, 'status' | 'pagina' | 'tamanho'>) =>
    api.get<TotaisEncomendas>('/encomendas/totais', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  opcoes: () => api.get<OpcoesEncomenda>('/encomendas/opcoes').then((r) => r.data),

  registrar: (encomenda: NovaEncomenda) => api.post<Encomenda>('/encomendas', encomenda).then((r) => r.data),

  registrarRetirada: (id: number, retirada: Retirada) =>
    api.post<Encomenda>(`/encomendas/${id}/retirada`, retirada).then((r) => r.data),

  mudarSituacao: (id: number, novoStatus: string, observacoes?: string) =>
    api.put<Encomenda>(`/encomendas/${id}/status`, { novoStatus, observacoes }).then((r) => r.data),
};
