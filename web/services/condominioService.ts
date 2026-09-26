import api from '@/services/api';

export interface CondominioResumo {
  id: number;
  nome: string;
  ativo: boolean;
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
  /** Condomínios que quem está logado pode ver, em ordem alfabética. */
  listar: () => api.get<CondominioResumo[]>('/condominios').then((r) => r.data),

  /** Unidades ativas do condomínio, para as listas de escolha dos formulários. */
  unidades: (condominioId: number) =>
    api.get<UnidadeResumo[]>(`/condominios/${condominioId}/unidades`).then((r) => r.data),
};
