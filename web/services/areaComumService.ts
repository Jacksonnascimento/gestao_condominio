import api from '@/services/api';
import type { Pagina } from '@/services/tipos';
import { formatarMoeda, horario, limparParametros } from '@/services/utilitarios';

export interface TurnoDaArea {
  codigo: number;
  nome: string;
  /** "18:00:00" */
  horaInicio: string;
  horaFim: string;
  ativo: boolean;
}

export interface AreaComum {
  codigo: number;
  condominioCodigo: number;
  condominioNome: string | null;
  nome: string;
  descricao: string | null;
  termosUso: string | null;
  capacidadeMaxima: number | null;
  permiteConvidados: boolean;
  limiteConvidados: number | null;
  taxaValor: number | null;
  diasAntecedenciaMin: number | null;
  diasAntecedenciaMax: number | null;
  ativa: boolean;
  turnos: TurnoDaArea[];
}

export interface PedidoDeTurno {
  /** Turno já cadastrado; sem código, a API cria um turno novo. */
  codigo?: number;
  nome: string;
  horaInicio: string;
  horaFim: string;
  ativo?: boolean;
}

/**
 * Cadastro e edição. Na edição a API troca tudo: os turnos que não vierem são removidos, e a área volta a ficar
 * ativa se `ativa` não vier. Por isso a edição manda sempre a área inteira.
 */
export interface PedidoDeAreaComum {
  condominioId?: number;
  nome: string;
  descricao?: string | null;
  termosUso?: string | null;
  capacidadeMaxima?: number | null;
  permiteConvidados: boolean;
  limiteConvidados?: number | null;
  taxaValor?: number | null;
  diasAntecedenciaMin?: number | null;
  diasAntecedenciaMax?: number | null;
  ativa: boolean;
  turnos: PedidoDeTurno[];
}

export interface FiltroAreasComuns {
  condominioId?: number | null;
  busca?: string;
  pagina?: number;
  tamanho?: number;
}

export interface TotaisAreasComuns {
  TOTAL: number;
  ATIVAS: number;
  INATIVAS: number;
}

export interface OpcoesAreaComum {
  condominios: { codigo: number; nome: string }[];
  /** Quem está logado gerencia as áreas de algum condomínio. */
  podeGerenciar: boolean;
}

export const areaComumService = {
  listar: (filtro: FiltroAreasComuns) =>
    api.get<Pagina<AreaComum>>('/areas-comuns', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidade de áreas ativas e inativas do condomínio (sem a busca). */
  totais: (condominioId?: number | null) =>
    api.get<TotaisAreasComuns>('/areas-comuns/totais', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  opcoes: () => api.get<OpcoesAreaComum>('/areas-comuns/opcoes').then((r) => r.data),

  buscar: (id: number) => api.get<AreaComum>(`/areas-comuns/${id}`).then((r) => r.data),

  cadastrar: (area: PedidoDeAreaComum) => api.post<AreaComum>('/areas-comuns', area).then((r) => r.data),

  atualizar: (id: number, area: PedidoDeAreaComum) => api.put<AreaComum>(`/areas-comuns/${id}`, area).then((r) => r.data),

  excluir: (id: number) => api.delete(`/areas-comuns/${id}`).then(() => undefined),
};

/** A área como está, no formato do pedido de edição: base para ativar ou inativar sem mexer no resto. */
export function pedidoDaArea(area: AreaComum): PedidoDeAreaComum {
  return {
    nome: area.nome,
    descricao: area.descricao,
    termosUso: area.termosUso,
    capacidadeMaxima: area.capacidadeMaxima,
    permiteConvidados: area.permiteConvidados,
    limiteConvidados: area.limiteConvidados,
    taxaValor: area.taxaValor,
    diasAntecedenciaMin: area.diasAntecedenciaMin,
    diasAntecedenciaMax: area.diasAntecedenciaMax,
    ativa: area.ativa,
    turnos: area.turnos.map((t) => ({
      codigo: t.codigo,
      nome: t.nome,
      horaInicio: horario(t.horaInicio),
      horaFim: horario(t.horaFim),
      ativo: t.ativo,
    })),
  };
}

/** "Até 80 pessoas" etc.: as regras da área em frases curtas, para cartões e para quem vai reservar. */
export function regrasDaArea(area: AreaComum): string[] {
  const regras: string[] = [];
  if (area.capacidadeMaxima) regras.push(`Até ${area.capacidadeMaxima} ${area.capacidadeMaxima === 1 ? 'pessoa' : 'pessoas'}`);
  regras.push(area.taxaValor ? `Taxa de ${formatarMoeda(area.taxaValor)}` : 'Sem taxa de uso');
  if (!area.permiteConvidados) regras.push('Sem convidados de fora');
  else if (area.limiteConvidados) regras.push(`Até ${area.limiteConvidados} ${area.limiteConvidados === 1 ? 'convidado' : 'convidados'}`);
  else regras.push('Aceita convidados');
  const min = area.diasAntecedenciaMin;
  const max = area.diasAntecedenciaMax;
  if (min != null && max != null) {
    regras.push(min === 0 ? `Reserva até ${max} dias antes` : `Reserva com ${min} a ${max} dias de antecedência`);
  }
  return regras;
}
