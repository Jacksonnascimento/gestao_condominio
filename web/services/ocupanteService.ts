import api from '@/services/api';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros } from '@/services/utilitarios';

export type VinculoDoOcupante =
  | 'PROPRIETARIO'
  | 'LOCATARIO'
  | 'PROMITENTE_COMPRADOR'
  | 'CESSIONARIO'
  | 'MULTIPROPRIETARIO'
  | 'CONJUGE'
  | 'DEPENDENTE';

export interface Ocupante {
  id: number;
  pessoaId: number;
  nome: string;
  /** Só vem na busca por id, e só para quem gerencia os ocupantes do condomínio; nas listas vem nulo. */
  cpfCnpj: string | null;
  email: string | null;
  telefone: string | null;
  vinculo: VinculoDoOcupante | null;
  vinculoDescricao: string | null;
  unidadeId: number;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  condominioId: number;
  condominioNome: string | null;
  inicioOcupacao: string | null;
  fimOcupacao: string | null;
  periodoUso: string | null;
  tipoPeriodo: string | null;
  tipoPeriodoDescricao: string | null;
}

export interface FiltroOcupantes {
  condominioId?: number | null;
  busca?: string;
  vinculo?: VinculoDoOcupante | '';
  unidadeId?: number | null;
  pagina?: number;
  tamanho?: number;
}

/** Quantidade por vínculo, e o TOTAL. */
export type TotaisOcupantes = Partial<Record<VinculoDoOcupante | 'TOTAL', number>>;

export interface OpcoesOcupante {
  vinculos: Opcao[];
  tiposPeriodo: Opcao[];
  /** Quem está logado cadastra ocupantes (síndico, administração, administrador geral). */
  podeGerenciar: boolean;
}

/**
 * Vínculo de uma pessoa a uma unidade. Se o CPF/CNPJ ainda não tem cadastro, a pessoa é cadastrada com nome,
 * e-mail e telefone (nome e e-mail obrigatórios); se já tem, esses campos são ignorados pela API.
 */
export interface NovoOcupante {
  cpfCnpj: string;
  /** F (física) ou J (jurídica). */
  tipoPessoa?: 'F' | 'J';
  nome?: string;
  email?: string;
  telefone?: string;
  unidadeId: number;
  vinculo: string;
  inicioOcupacao: string;
  fimOcupacao?: string | null;
  /** Período de uso e tipo do período só valem para multiproprietário. */
  periodoUso?: string | null;
  tipoPeriodo?: string | null;
}

/** Edição: o CPF/CNPJ e a unidade não mudam. */
export interface AlteracaoDoOcupante {
  nome: string;
  email: string;
  telefone?: string | null;
  vinculo: string;
  inicioOcupacao: string;
  fimOcupacao?: string | null;
  periodoUso?: string | null;
  tipoPeriodo?: string | null;
}

export const ocupanteService = {
  listar: (filtro: FiltroOcupantes) =>
    api.get<Pagina<Ocupante>>('/ocupantes', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidades por vínculo, com os mesmos filtros da listagem (sem o vínculo). */
  totais: (filtro: Omit<FiltroOcupantes, 'vinculo' | 'pagina' | 'tamanho'>) =>
    api.get<TotaisOcupantes>('/ocupantes/totais', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  opcoes: () => api.get<OpcoesOcupante>('/ocupantes/opcoes').then((r) => r.data),

  buscar: (id: number) => api.get<Ocupante>(`/ocupantes/${id}`).then((r) => r.data),

  cadastrar: (ocupante: NovoOcupante) => api.post<Ocupante>('/ocupantes', ocupante).then((r) => r.data),

  atualizar: (id: number, alteracao: AlteracaoDoOcupante) =>
    api.put<Ocupante>(`/ocupantes/${id}`, alteracao).then((r) => r.data),

  /** Desfaz o vínculo com a unidade; a pessoa continua cadastrada. */
  remover: (id: number) => api.delete<void>(`/ocupantes/${id}`).then(() => undefined),
};
