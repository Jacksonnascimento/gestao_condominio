import { baixarArquivo } from '@/components/comunicados/baixarArquivo';
import api from '@/services/api';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros } from '@/services/utilitarios';

export type SituacaoOcorrencia = 'ABERTA' | 'EM_ANALISE' | 'RESOLVIDA';
export type TipoOcorrencia = 'BARULHO' | 'CONFLITO' | 'MANUTENCAO' | 'RECLAMACAO' | 'OUTRO';

/** Ocorrência como vem na listagem. */
export interface OcorrenciaResumida {
  id: number;
  titulo: string;
  status: SituacaoOcorrencia;
  tipo: TipoOcorrencia;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  condominioNome: string | null;
  dataRegistro: string;
  /** Início da descrição (até 100 letras). */
  descricaoCurta: string | null;
}

export interface ComentarioDaOcorrencia {
  id: number;
  ocorrenciaId: number;
  comentario: string;
  nomeUsuario: string | null;
  dataComentario: string;
}

export interface AnexoDaOcorrencia {
  id: number;
  nomeOriginal: string;
  tipoArquivo: string | null;
  tamanhoArquivo: number | null;
  nomeUsuario: string | null;
  dataAnexo: string;
}

/**
 * Ocorrência completa. Comentários e anexos só vêm para quem trata a ocorrência (`podeGerenciar`); para os demais,
 * as listas vêm vazias. O nome de quem registrou vem como "Morador" quando a pessoa não pode saber quem foi.
 */
export interface Ocorrencia {
  id: number;
  titulo: string;
  descricao: string;
  status: SituacaoOcorrencia;
  tipo: TipoOcorrencia;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
  condominioNome: string | null;
  dataRegistro: string;
  nomePessoaRegistro: string | null;
  parecerFinal: string | null;
  nomePessoaFinalizou: string | null;
  dataFinalizacao: string | null;
  comentarios: ComentarioDaOcorrencia[];
  anexos: AnexoDaOcorrencia[];
  podeGerenciar: boolean;
}

export interface FiltroOcorrencias {
  condominioId?: number | null;
  buscaUnidade?: string;
  buscaTitulo?: string;
  tipo?: TipoOcorrencia | '';
  status?: SituacaoOcorrencia | '';
  /** Registradas a partir deste dia (2026-09-01). */
  inicioApos?: string;
  /** Registradas até este dia, inclusive. */
  fimAntes?: string;
  pagina?: number;
  tamanho?: number;
}

export interface TotaisOcorrencias {
  TOTAL: number;
  ABERTA: number;
  EM_ANALISE: number;
  RESOLVIDA: number;
  /** Registradas no último mês. */
  ESTE_MES: number;
}

export interface UnidadeDaOcorrencia {
  codigo: number;
  numero: string;
  bloco: string | null;
}

export interface OpcoesOcorrencia {
  tipos: Opcao[];
  status: Opcao[];
  /** Quem está logado trata ocorrências: comenta, anexa e resolve (síndico, administração, funcionário administrativo). */
  podeGerenciar: boolean;
  condominios: { codigo: number; nome: string | null }[];
  /** Unidades que podem receber uma ocorrência nova. */
  unidades: UnidadeDaOcorrencia[];
}

export interface NovaOcorrencia {
  unidadeId: number;
  tipo: TipoOcorrencia;
  titulo: string;
  descricao: string;
  /** Pedido só ao administrador geral; para os demais, a API usa o condomínio da unidade. */
  condominioId?: number | null;
}

export const ocorrenciaService = {
  listar: (filtro: FiltroOcorrencias) =>
    api.get<Pagina<OcorrenciaResumida>>('/ocorrencias', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** Quantidades por situação, com os mesmos filtros da listagem (sem a situação). */
  totais: (filtro: Omit<FiltroOcorrencias, 'status' | 'pagina' | 'tamanho'>) =>
    api.get<TotaisOcorrencias>('/ocorrencias/totais', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** O `condominioId` escolhe as unidades do formulário quando quem está logado é o administrador geral. */
  opcoes: (condominioId?: number | null) =>
    api.get<OpcoesOcorrencia>('/ocorrencias/opcoes', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  buscar: (id: number) => api.get<Ocorrencia>(`/ocorrencias/${id}`).then((r) => r.data),

  registrar: (ocorrencia: NovaOcorrencia) => api.post<Ocorrencia>('/ocorrencias', ocorrencia).then((r) => r.data),

  /** O primeiro comentário da gestão numa ocorrência aberta a coloca em análise. */
  comentar: (id: number, comentario: string) =>
    api.post<ComentarioDaOcorrencia>(`/ocorrencias/${id}/comentarios`, { comentario }).then((r) => r.data),

  /** Como o comentário, o primeiro anexo da gestão numa ocorrência aberta a coloca em análise. */
  anexar: (id: number, arquivo: File) => {
    const corpo = new FormData();
    corpo.append('anexo', arquivo);
    return api.post<AnexoDaOcorrencia>(`/ocorrencias/${id}/anexos`, corpo).then((r) => r.data);
  },

  excluirAnexo: (id: number, anexoId: number) =>
    api.delete<void>(`/ocorrencias/${id}/anexos/${anexoId}`).then(() => undefined),

  baixarAnexo: (id: number, anexo: Pick<AnexoDaOcorrencia, 'id' | 'nomeOriginal'>) =>
    baixarArquivo(`/ocorrencias/${id}/anexos/${anexo.id}`, anexo.nomeOriginal),

  /** Marca a ocorrência como resolvida, com o parecer final. */
  resolver: (id: number, parecerFinal: string) =>
    api.post<Ocorrencia>(`/ocorrencias/${id}/finalizacao`, { parecerFinal }).then((r) => r.data),
};
