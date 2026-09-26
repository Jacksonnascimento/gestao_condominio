import api from '@/services/api';
import type { UsuarioLogado } from '@/services/autenticacaoService';

export interface DadosDoPerfil {
  nome: string;
  /** Nulo mantém o telefone atual; texto vazio apaga. */
  telefone?: string | null;
  telefone2?: string | null;
}

export const perfilService = {
  /** Os dados de quem está logado, sempre lidos de novo da API. */
  eu: () => api.get<UsuarioLogado>('/auth/eu').then((r) => r.data),

  atualizar: (dados: DadosDoPerfil) => api.patch<UsuarioLogado>('/perfil', dados).then((r) => r.data),

  /**
   * Troca a senha conferindo a atual. A API não devolve tokens novos: a troca derruba os tokens já emitidos, e é
   * preciso entrar de novo.
   */
  trocarSenha: (senhaAtual: string, novaSenha: string) =>
    api.put<{ mensagem: string }>('/perfil/senha', { senhaAtual, novaSenha }).then((r) => r.data),

  /** Foto de quem está logado; 404 quando não há. */
  foto: () => api.get<Blob>('/perfil/foto', { responseType: 'blob' }).then((r) => r.data),

  /** Troca a foto por uma imagem JPEG, PNG ou WebP de até 1 MB (a tela já a reduz antes). */
  trocarFoto: (imagem: Blob) => {
    const corpo = new FormData();
    corpo.append('foto', imagem, 'foto.jpg');
    return api.put<UsuarioLogado>('/perfil/foto', corpo).then((r) => r.data);
  },

  tirarFoto: () => api.delete<UsuarioLogado>('/perfil/foto').then((r) => r.data),
};
