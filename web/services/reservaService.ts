import api from '@/services/api';
import type { AreaComum } from '@/services/areaComumService';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros, type Enumerado } from '@/services/utilitarios';

export type SituacaoReserva = 'PENDENTE_APROVACAO' | 'APROVADA' | 'REJEITADA' | 'CANCELADA_PELO_MORADOR' | 'CONCLUIDA';

export interface ConvidadoDaReserva {
  codigo: number;
  nome: string;
  documento: string | null;
}

export interface Reserva {
  codigo: number;
  status: Enumerado;
  statusDescricao: string | null;
  /** "2026-10-10" */
  data: string;
  areaCodigo: number;
  areaNome: string;
  condominioCodigo: number;
  condominioNome: string | null;
  /** Sem turno, a reserva é do dia inteiro. */
  turnoCodigo: number | null;
  turnoNome: string | null;
  turnoHoraInicio: string | null;
  turnoHoraFim: string | null;
  unidadeCodigo: number;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  solicitanteCodigo: number;
  solicitanteNome: string | null;
  motivoRejeicao: string | null;
  dataRegistro: string | null;
  convidados: ConvidadoDaReserva[];
  /** Quem está logado gerencia o condomínio e a reserva está pendente. */
  podeAprovarOuRejeitar: boolean;
  /** Quem está logado pediu a reserva e ela ainda está pendente ou aprovada. */
  podeCancelar: boolean;
}

export interface FiltroReservas {
  condominioId?: number | null;
  status?: SituacaoReserva | '';
  busca?: string;
  areaId?: number | '';
  /** "2026-10-01" */
  dataInicio?: string;
  dataFim?: string;
  pagina?: number;
  tamanho?: number;
}

/** Quantidade por situação e no total, com os filtros da lista menos o de situação. */
export type TotaisReservas = Record<SituacaoReserva | 'TOTAL', number>;

export interface TurnoNoDia {
  codigo: number;
  nome: string;
  horaInicio: string | null;
  horaFim: string | null;
  livre: boolean;
}

/** Ocupação da área num dia. Não diz quem reservou. */
export interface DisponibilidadeDoDia {
  /** "2026-10-10" */
  data: string;
  /** Livre só se não houver nenhuma reserva na data. */
  diaInteiroLivre: boolean;
  /** Turnos ativos da área; livre se não houver reserva no turno nem do dia inteiro. */
  turnos: TurnoNoDia[];
}

export interface UnidadeParaReserva {
  codigo: number;
  numero: string;
  bloco: string | null;
  condominioCodigo: number;
  condominioNome: string | null;
}

export interface OpcoesReserva {
  status: Opcao[];
  condominios: { codigo: number; nome: string }[];
  /** Unidades em que quem está logado mora. */
  unidades: UnidadeParaReserva[];
  /** Áreas ativas dos condomínios dessas unidades, com regras e turnos ativos. */
  areasParaSolicitar: AreaComum[];
  areasParaFiltro: { codigo: number; nome: string; condominioCodigo: number }[];
  /** Aprova e rejeita reservas de algum condomínio. */
  podeGerenciar: boolean;
  /** Mora em alguma unidade, e por isso pode pedir reservas. */
  podeSolicitar: boolean;
}

export interface PedidoDeReserva {
  areaId: number;
  /** Sem turno, a reserva é do dia inteiro. */
  turnoId?: number;
  unidadeId: number;
  data: string;
  termosAceitos: boolean;
  convidados?: { nome: string; documento?: string }[];
}

export const reservaService = {
  listar: (filtro: FiltroReservas) =>
    api.get<Pagina<Reserva>>('/reservas', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  totais: (filtro: Omit<FiltroReservas, 'status' | 'pagina' | 'tamanho'>) =>
    api.get<TotaisReservas>('/reservas/totais', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Ocupação da área dia a dia (até 62 dias; sem `dataFim`, só `dataInicio`). */
  disponibilidade: (areaId: number, dataInicio: string, dataFim?: string) =>
    api
      .get<DisponibilidadeDoDia[]>('/reservas/disponibilidade', { params: limparParametros({ areaId, dataInicio, dataFim }) })
      .then((r) => r.data),

  opcoes: (condominioId?: number | null) =>
    api.get<OpcoesReserva>('/reservas/opcoes', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  buscar: (id: number) => api.get<Reserva>(`/reservas/${id}`).then((r) => r.data),

  solicitar: (pedido: PedidoDeReserva) => api.post<Reserva>('/reservas', pedido).then((r) => r.data),

  aprovar: (id: number) => api.post<Reserva>(`/reservas/${id}/aprovar`).then((r) => r.data),

  rejeitar: (id: number, motivo: string) => api.post<Reserva>(`/reservas/${id}/rejeitar`, { motivo }).then((r) => r.data),

  cancelar: (id: number) => api.post<Reserva>(`/reservas/${id}/cancelar`).then((r) => r.data),
};
