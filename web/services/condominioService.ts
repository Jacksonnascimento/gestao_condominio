import api from '@/services/api';
import type { Opcao } from '@/services/tipos';

/*
 * Condomínios: a lista que atende a sessão e as listas de escolha dos outros módulos, e o cadastro (cadastrar,
 * editar, ativar e inativar), que é só do administrador geral.
 */

export type Tipologia = 'RESIDENCIAL' | 'COMERCIAL' | 'MISTO';

export interface Condominio {
  id: number;
  nome: string;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  estado: string | null;
  /** Só os números: 45000000. */
  cep: string | null;
  pais: string | null;
  referencia: string | null;
  numeroUnidades: number | null;
  tipologia: Tipologia | string;
  diaVencimentoTaxa: number | null;
  ativo: boolean;
  dataCadastro: string | null;
  dataAtualizacao: string | null;
}

/** Na edição, campo nulo mantém o valor atual; texto vazio apaga. */
export interface DadosDoCondominio {
  nome: string;
  logradouro?: string | null;
  numero?: string | null;
  complemento?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  estado?: string | null;
  cep?: string | null;
  pais?: string | null;
  referencia?: string | null;
  numeroUnidades?: number | null;
  tipologia: string;
  diaVencimentoTaxa?: number | null;
}

export interface OpcoesCondominio {
  tipologias: Opcao[];
  /** Só o administrador geral cadastra, edita, ativa e inativa. */
  podeGerenciar: boolean;
}

export interface UnidadeResumo {
  id: number;
  numero: string;
  bloco: string | null;
  andar: string | null;
  tipo: string | null;
  tipoDescricao: string | null;
}

export const condominioService = {
  /**
   * Condomínios que quem está logado pode ver, em ordem alfabética. Com `incluirInativos` (só para o cadastro do
   * administrador geral), vêm também os inativos.
   */
  listar: (incluirInativos = false) =>
    api
      .get<Condominio[]>('/condominios', { params: incluirInativos ? { incluirInativos } : undefined })
      .then((r) => r.data),

  /** Unidades ativas do condomínio, para as listas de escolha dos formulários. */
  unidades: (condominioId: number) =>
    api.get<UnidadeResumo[]>(`/condominios/${condominioId}/unidades`).then((r) => r.data),

  opcoes: () => api.get<OpcoesCondominio>('/condominios/opcoes').then((r) => r.data),

  cadastrar: (dados: DadosDoCondominio) => api.post<Condominio>('/condominios', dados).then((r) => r.data),

  atualizar: (id: number, dados: DadosDoCondominio) => api.put<Condominio>(`/condominios/${id}`, dados).then((r) => r.data),

  /** Só inativa condomínio sem unidades e sem usuários; senão a API diz o motivo. */
  inativar: (id: number) => api.put<Condominio>(`/condominios/${id}/inativar`).then((r) => r.data),

  ativar: (id: number) => api.put<Condominio>(`/condominios/${id}/ativar`).then((r) => r.data),
};
