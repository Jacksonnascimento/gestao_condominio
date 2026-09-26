import api from '@/services/api';

export type Papel = 'SINDICO' | 'MORADOR' | 'FUNCIONARIO_ADM' | 'PORTEIRO' | 'ADMIN';

export interface Vinculo {
  condominioCodigo: number;
  condominioNome: string | null;
  papel: Papel;
  papelDescricao: string;
}

export interface UsuarioLogado {
  codigo: number;
  nome: string;
  email: string;
  cpfCnpj: string | null;
  telefone: string | null;
  telefone2: string | null;
  administradorGeral: boolean;
  possuiFoto: boolean;
  vinculos: Vinculo[];
}

export interface RespostaLogin {
  token: string;
  tokenRenovacao: string;
  expiraEm: string;
  usuario: UsuarioLogado;
}

export const autenticacaoService = {
  entrar: (email: string, senha: string) =>
    api.post<RespostaLogin>('/auth/login', { email, senha }).then((r) => r.data),

  eu: () => api.get<UsuarioLogado>('/auth/eu').then((r) => r.data),

  esqueciSenha: (email: string) =>
    api.post<{ mensagem: string }>('/auth/esqueci-senha', { email }).then((r) => r.data),

  conferirLinkDeSenha: (token: string) =>
    api.get<{ mensagem: string }>(`/auth/redefinir-senha/${encodeURIComponent(token)}`).then((r) => r.data),

  redefinirSenha: (token: string, novaSenha: string) =>
    api.post<{ mensagem: string }>('/auth/redefinir-senha', { token, novaSenha }).then((r) => r.data),
};
