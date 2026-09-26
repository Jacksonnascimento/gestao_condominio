import api from '@/services/api';
import type { Opcao } from '@/services/tipos';

/*
 * Cadastro de condomínios, só do administrador geral. Fica fora do condominioService, que atende a sessão e as
 * listas de escolha dos outros módulos.
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

/** 45000000 vira 45000-000. */
export function formatarCep(cep?: string | null): string {
  const numeros = (cep ?? '').replace(/\D/g, '');
  return numeros.length === 8 ? `${numeros.slice(0, 5)}-${numeros.slice(5)}` : (cep ?? '');
}

export const cadastroDeCondominiosService = {
  listar: (incluirInativos: boolean) =>
    api.get<Condominio[]>('/condominios', { params: { incluirInativos } }).then((r) => r.data),

  opcoes: () => api.get<OpcoesCondominio>('/condominios/opcoes').then((r) => r.data),

  cadastrar: (dados: DadosDoCondominio) => api.post<Condominio>('/condominios', dados).then((r) => r.data),

  atualizar: (id: number, dados: DadosDoCondominio) => api.put<Condominio>(`/condominios/${id}`, dados).then((r) => r.data),

  /** Só inativa condomínio sem unidades e sem usuários; senão a API diz o motivo. */
  inativar: (id: number) => api.put<Condominio>(`/condominios/${id}/inativar`).then((r) => r.data),

  ativar: (id: number) => api.put<Condominio>(`/condominios/${id}/ativar`).then((r) => r.data),
};
