import api from '@/services/api';
import type { Pagina } from '@/services/tipos';

/** Indicadores do painel; vem nulo o que não é do papel de quem está logado, e a tela esconde o cartão. */
export interface Indicadores {
  totalUnidades: number | null;
  totalOcupantes: number | null;
  totalContratosAtivos: number | null;
  totalOcorrenciasPendentes: number | null;
}

export interface ReservaResumo {
  codigo: number;
  status: string;
  statusDescricao: string | null;
  data: string;
  areaNome: string | null;
  turnoNome: string | null;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
}

export interface OcorrenciaResumo {
  id: number;
  titulo: string;
  status: string;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  dataRegistro: string;
}

export const painelService = {
  indicadores: (condominioId?: number | null) =>
    api.get<Indicadores>('/dashboard', { params: condominioId ? { condominioId } : {} }).then((r) => r.data),

  /** Reservas a partir da data, das mais próximas para as mais distantes. */
  proximasReservas: (condominioId: number | null | undefined, aPartirDe: string) =>
    api
      .get<Pagina<ReservaResumo>>('/reservas', {
        params: { ...(condominioId ? { condominioId } : {}), dataInicio: aPartirDe, crescente: true, tamanho: 20 },
      })
      .then((r) => r.data),

  ocorrenciasAbertas: (condominioId?: number | null) =>
    api
      .get<Pagina<OcorrenciaResumo>>('/ocorrencias', {
        params: { ...(condominioId ? { condominioId } : {}), status: 'ABERTA', tamanho: 3 },
      })
      .then((r) => r.data),
};
