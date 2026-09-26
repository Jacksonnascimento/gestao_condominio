import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import {
  encerrarSessao,
  gravarSessao,
  tokenDoNavegador,
  tokenRenovacaoDoNavegador,
  tokenVigente,
} from '@/services/sessao';

// Vazio, as chamadas vão para o mesmo endereço da tela (/api/v1/...), e o Next as repassa à API com o endereço
// original: é por ele que a API sabe de qual condomínio (e de qual banco) é o pedido.
const base = (process.env.NEXT_PUBLIC_API_URL || '') + '/api/v1';

const api = axios.create({ baseURL: base });

api.interceptors.request.use((config) => {
  const token = tokenDoNavegador();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let renovacaoEmAndamento: Promise<boolean> | null = null;

/**
 * Troca o token de renovação por um par novo. As chamadas que recebem 401 ao mesmo tempo esperam a mesma
 * renovação, em vez de cada uma pedir a sua.
 */
function renovarSessao(): Promise<boolean> {
  if (!renovacaoEmAndamento) {
    const tokenRenovacao = tokenRenovacaoDoNavegador();
    renovacaoEmAndamento = !tokenVigente(tokenRenovacao)
      ? Promise.resolve(false)
      : axios
          .post(`${base}/auth/renovar`, { tokenRenovacao })
          .then(({ data }) => {
            gravarSessao(data.token, data.tokenRenovacao);
            return true;
          })
          .catch(() => false)
          .finally(() => {
            renovacaoEmAndamento = null;
          });
  }
  return renovacaoEmAndamento;
}

type PedidoRepetivel = InternalAxiosRequestConfig & { _repetido?: boolean };

// 401 é sessão ausente, vencida ou recusada pela API: tenta renovar uma vez e repete o pedido; sem renovação,
// volta ao login. O 401 do próprio login é senha errada e fica com a tela.
api.interceptors.response.use(
  (resposta) => resposta,
  async (erro: AxiosError) => {
    const pedido = erro.config as PedidoRepetivel | undefined;
    const naAutenticacao = pedido?.url?.includes('/auth/');
    if (erro.response?.status === 401 && pedido && !naAutenticacao && typeof window !== 'undefined') {
      if (!pedido._repetido && (await renovarSessao())) {
        pedido._repetido = true;
        return api(pedido);
      }
      encerrarSessao(true);
    }
    return Promise.reject(erro);
  },
);

export default api;
