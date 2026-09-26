import axios from 'axios';
import api from '@/services/api';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros } from '@/services/utilitarios';

export type SituacaoDaUnidade = 'OCUPADA' | 'VAZIA' | 'EM_REFORMA' | 'MULTIPROPRIEDADE';

export interface Unidade {
  id: number;
  condominioId: number;
  condominioNome: string | null;
  numero: string;
  bloco: string | null;
  andar: string | null;
  tipo: string | null;
  tipoDescricao: string | null;
  statusOcupacao: SituacaoDaUnidade | null;
  statusOcupacaoDescricao: string | null;
  fracaoIdeal: number | null;
  areaPrivada: number | null;
  observacao: string | null;
  ativa: boolean;
  dataCadastro: string | null;
  dataAtualizacao: string | null;
}

export interface FiltroUnidades {
  condominioId?: number | null;
  busca?: string;
  status?: SituacaoDaUnidade | '';
  /** Com true, a lista traz também as unidades inativas (junto das ativas). */
  incluirInativas?: boolean;
  pagina?: number;
  tamanho?: number;
}

/** Quantidade por situação de ocupação, e o TOTAL. */
export type TotaisUnidades = Partial<Record<SituacaoDaUnidade | 'TOTAL', number>>;

export interface OpcoesUnidade {
  tipos: Opcao[];
  statusOcupacao: Opcao[];
  /** Quem está logado cadastra, edita, inativa e reativa unidades (síndico e administração). */
  podeGerenciar: boolean;
}

/** Dados do formulário de unidade. Na edição o condomínio é ignorado: a unidade não muda de condomínio. */
export interface DadosDaUnidade {
  condominioId?: number | null;
  numero: string;
  bloco?: string | null;
  andar?: string | null;
  tipo: string;
  statusOcupacao: string;
  fracaoIdeal?: number | null;
  areaPrivada?: number | null;
  observacao?: string | null;
}

export const unidadeService = {
  listar: (filtro: FiltroUnidades) =>
    api.get<Pagina<Unidade>>('/unidades', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidades por situação, com os mesmos filtros da listagem (sem a situação). */
  totais: (filtro: Omit<FiltroUnidades, 'status' | 'pagina' | 'tamanho'>) =>
    api.get<TotaisUnidades>('/unidades/totais', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  opcoes: () => api.get<OpcoesUnidade>('/unidades/opcoes').then((r) => r.data),

  buscar: (id: number) => api.get<Unidade>(`/unidades/${id}`).then((r) => r.data),

  cadastrar: (dados: DadosDaUnidade) => api.post<Unidade>('/unidades', dados).then((r) => r.data),

  atualizar: (id: number, dados: DadosDaUnidade) => api.put<Unidade>(`/unidades/${id}`, dados).then((r) => r.data),

  /** A API só inativa unidade sem ocupantes; é a "exclusão" da unidade. */
  inativar: (id: number) => api.put<Unidade>(`/unidades/${id}/inativar`).then((r) => r.data),

  reativar: (id: number) => api.put<Unidade>(`/unidades/${id}/reativar`).then((r) => r.data),
};

/**
 * No cadastro, quando a mesma unidade (número, bloco e tipo) já existiu e foi inativada, a API responde 409 com o
 * código dela, para a tela oferecer a reativação em vez de criar outra. Devolve esse código, ou null.
 */
export function unidadeInativaDoErro(erro: unknown): number | null {
  if (!axios.isAxiosError(erro) || erro.response?.status !== 409) return null;
  const id = (erro.response.data as { unidadeId?: unknown } | undefined)?.unidadeId;
  return typeof id === 'number' ? id : null;
}
