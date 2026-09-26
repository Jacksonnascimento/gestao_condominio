import api from '@/services/api';
import { baixarArquivo } from '@/services/arquivos';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros } from '@/services/utilitarios';

export type PublicoDoComunicado = 'TODOS' | 'PROPRIETARIOS' | 'INQUILINOS' | 'FUNCIONARIOS';

export interface CondominioDoComunicado {
  codigo: number;
  nome: string | null;
}

export interface Comunicado {
  id: number;
  titulo: string;
  mensagem: string;
  publicoDestino: PublicoDoComunicado;
  publicoDestinoDescricao: string | null;
  urgente: boolean;
  dataCadastro: string;
  autor: string | null;
  possuiAnexo: boolean;
  /** Nome com que o arquivo foi enviado ("anexo.pdf" nos comunicados antigos, que não o guardaram). */
  nomeAnexo: string | null;
  /** Condomínios de destino; só vêm para o administrador geral. */
  condominios: CondominioDoComunicado[];
  /** Quem está logado pode editar e excluir este comunicado. */
  podeGerenciar: boolean;
}

export interface FiltroComunicados {
  /** Condomínio escolhido no topo. Sem ele, o administrador geral vê os de todos. */
  condominioId?: number | null;
  titulo?: string;
  mensagem?: string;
  publicoDestino?: PublicoDoComunicado | '';
  urgente?: boolean | null;
  pagina?: number;
  tamanho?: number;
}

export interface OpcoesComunicado {
  publicos: Opcao[];
  /** Quem está logado publica comunicados no condomínio (síndico, administração ou administrador geral). */
  podeGerenciar: boolean;
  /** Condomínios de destino, só para o administrador geral; vazio para os demais. */
  condominios: CondominioDoComunicado[];
}

export interface DadosDoComunicado {
  titulo: string;
  mensagem: string;
  publicoDestino: PublicoDoComunicado;
  urgente: boolean;
  /** Só para o administrador geral; os demais publicam sempre no próprio condomínio. */
  condominioIds?: number[];
}

/** Formulário multipart da API: os dados em JSON na parte `comunicado` e o arquivo, se houver, na parte `anexo`. */
function formulario(dados: DadosDoComunicado, anexo?: File | null): FormData {
  const corpo = new FormData();
  corpo.append('comunicado', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
  if (anexo) corpo.append('anexo', anexo);
  return corpo;
}

export const comunicadoService = {
  listar: (filtro: FiltroComunicados) =>
    api.get<Pagina<Comunicado>>('/comunicados', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  /** `podeGerenciar` vale para o condomínio informado. */
  opcoes: (condominioId?: number | null) =>
    api.get<OpcoesComunicado>('/comunicados/opcoes', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  buscar: (id: number) => api.get<Comunicado>(`/comunicados/${id}`).then((r) => r.data),

  /** Síndico e administração publicam no `condominioId`; o administrador geral, nos `condominioIds` dos dados. */
  publicar: (dados: DadosDoComunicado, anexo?: File | null, condominioId?: number | null) =>
    api
      .post<Comunicado>('/comunicados', formulario(dados, anexo), { params: limparParametros({ condominioId }) })
      .then((r) => r.data),

  /** Um arquivo novo substitui o anexo atual; sem arquivo, o anexo atual fica. */
  editar: (id: number, dados: DadosDoComunicado, anexo?: File | null) =>
    api.put<Comunicado>(`/comunicados/${id}`, formulario(dados, anexo)).then((r) => r.data),

  excluir: (id: number) => api.delete<void>(`/comunicados/${id}`).then(() => undefined),

  baixarAnexo: (comunicado: Pick<Comunicado, 'id' | 'nomeAnexo'>) =>
    baixarArquivo(`/comunicados/${comunicado.id}/anexo`, comunicado.nomeAnexo ?? 'anexo'),
};
