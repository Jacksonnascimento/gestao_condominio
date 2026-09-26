import api from '@/services/api';
import type { Papel } from '@/services/autenticacaoService';
import type { Opcao, Pagina } from '@/services/tipos';
import { limparParametros, type Enumerado } from '@/services/utilitarios';

/** Um acesso: a pessoa, o condomínio e o papel dela nele. Os três juntos identificam o vínculo. */
export interface AcessoDeUsuario {
  pessoaId: number;
  pessoaNome: string;
  pessoaEmail: string;
  condominioId: number;
  condominioNome: string;
  papel: Enumerado;
  papelDescricao: string | null;
  /** Quando o acesso foi dado; não muda com a troca de papel nem ao desativar e reativar. */
  dataAssociacao: string | null;
  /** Desativado, o acesso fica guardado mas não vale para entrar no sistema. */
  ativo: boolean | null;
}

export interface TotaisAcessos {
  TOTAL: number;
  ATIVOS: number;
  DESATIVADOS: number;
}

/** ENVIAR_LINK: a pessoa recebe o link por e-mail. CRIAR_SENHA: a senha é definida agora (só para pessoa nova). */
export type AcaoSenha = 'ENVIAR_LINK' | 'CRIAR_SENHA';

export interface OpcoesUsuario {
  /** Condomínios em que quem está logado administra os usuários. */
  condominios: { codigo: number; nome: string }[];
  papeis: Opcao[];
  acoesSenha: Opcao[];
  podeGerenciar: boolean;
}

export interface OcupanteSemAcesso {
  pessoaCodigo: number;
  nome: string;
  email: string | null;
  unidadeNumero: string | null;
  unidadeBloco: string | null;
}

export interface PessoaCadastrada {
  codigo: number;
  nome: string;
  email: string | null;
  telefone: string | null;
}

/**
 * Novo acesso. Para Morador, `pessoaId` de um ocupante sem acesso. Para os demais, o CPF: se já houver cadastro, o
 * acesso vai para essa pessoa e os outros dados são ignorados; senão, a pessoa é cadastrada.
 */
export interface NovoAcesso {
  condominioId: number;
  papel: Papel;
  pessoaId?: number;
  acaoSenha?: AcaoSenha;
  cpf?: string;
  nome?: string;
  email?: string;
  telefone?: string;
  senha?: string;
}

export interface EdicaoDeAcesso {
  nome: string;
  email: string;
  papel: Papel;
}

const rotaDoVinculo = (a: Pick<AcessoDeUsuario, 'pessoaId' | 'condominioId'>, papel: string) =>
  `/usuarios/${a.pessoaId}/vinculos/${a.condominioId}/${encodeURIComponent(papel)}`;

export const usuarioService = {
  /** Sem `ativo`, traz os ativos e os desativados. */
  listar: (filtro: { condominioId?: number | null; ativo?: boolean | null; pagina?: number; tamanho?: number }) =>
    api.get<Pagina<AcessoDeUsuario>>('/usuarios', { params: limparParametros({ ...filtro }) }).then((r) => r.data),

  totais: (condominioId?: number | null) =>
    api.get<TotaisAcessos>('/usuarios/totais', { params: limparParametros({ condominioId }) }).then((r) => r.data),

  opcoes: () => api.get<OpcoesUsuario>('/usuarios/opcoes').then((r) => r.data),

  ocupantesSemAcesso: (condominioId: number) =>
    api.get<OcupanteSemAcesso[]>('/usuarios/ocupantes-sem-login', { params: { condominioId } }).then((r) => r.data),

  /** Pessoa já cadastrada com o CPF; 404 quando não há. */
  pessoaPorCpf: (cpf: string) =>
    api.get<PessoaCadastrada>('/usuarios/pessoa-por-cpf', { params: { cpf } }).then((r) => r.data),

  cadastrar: (acesso: NovoAcesso) => api.post<AcessoDeUsuario>('/usuarios', acesso).then((r) => r.data),

  editar: (acesso: AcessoDeUsuario, papelAtual: string, edicao: EdicaoDeAcesso) =>
    api.put<AcessoDeUsuario>(rotaDoVinculo(acesso, papelAtual), edicao).then((r) => r.data),

  enviarLinkDeSenha: (pessoaId: number) =>
    api.post<{ mensagem: string }>(`/usuarios/${pessoaId}/link-de-senha`).then((r) => r.data),

  /** Define a senha na hora, sem link; as sessões abertas da pessoa deixam de valer. */
  definirSenha: (pessoaId: number, novaSenha: string) =>
    api.put<{ mensagem: string }>(`/usuarios/${pessoaId}/senha`, { novaSenha }).then((r) => r.data),

  /** A pessoa deixa de entrar com esse papel; o acesso fica guardado, com a data de início, e pode ser reativado. */
  desativar: (acesso: AcessoDeUsuario, papel: string) =>
    api.post<AcessoDeUsuario>(`${rotaDoVinculo(acesso, papel)}/desativar`).then((r) => r.data),

  reativar: (acesso: AcessoDeUsuario, papel: string) =>
    api.post<AcessoDeUsuario>(`${rotaDoVinculo(acesso, papel)}/reativar`).then((r) => r.data),

  /** Remove o acesso de vez; a pessoa continua cadastrada. */
  remover: (acesso: AcessoDeUsuario, papel: string) => api.delete<void>(rotaDoVinculo(acesso, papel)),
};
