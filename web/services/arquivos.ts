import axios from 'axios';
import api from '@/services/api';

/** Tamanho máximo de arquivo aceito pela API (o envio de arquivos do sistema para em 10 MB). */
export const TAMANHO_MAXIMO_DE_ARQUIVO = 10 * 1024 * 1024;

/** "1,2 MB", "830 KB": para mostrar o tamanho de um arquivo. */
export function formatarTamanho(bytes?: number | null): string {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} bytes`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`;
}

/** Nome do arquivo informado pela API no cabeçalho Content-Disposition (com ou sem codificação UTF-8). */
function nomeDoCabecalho(disposicao: unknown): string | null {
  if (typeof disposicao !== 'string') return null;
  const codificado = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/.exec(disposicao);
  if (codificado) {
    try {
      return decodeURIComponent(codificado[1].trim().replace(/^"|"$/g, ''));
    } catch {
      // Segue para o nome simples
    }
  }
  const simples = /filename\s*=\s*"?([^";]+)"?/.exec(disposicao);
  return simples ? simples[1].trim() : null;
}

/**
 * Baixa um arquivo protegido da API e entrega ao navegador como download. A chamada passa pela instância `api`,
 * que leva o token no cabeçalho (um link direto não levaria). O arquivo sai sempre como download, nunca aberto na
 * própria aba: assim um HTML anexado não roda como página do sistema.
 *
 * Quando a API responde com erro, o corpo chega como arquivo; ele é lido de volta para o formato de sempre
 * ({ status, message }), para que `mensagemErroApi` mostre a mensagem da API.
 */
export async function baixarArquivo(caminho: string, nomeReserva = 'anexo'): Promise<void> {
  let resposta;
  try {
    resposta = await api.get<Blob>(caminho, { responseType: 'blob' });
  } catch (erro) {
    if (axios.isAxiosError(erro) && erro.response?.data instanceof Blob) {
      try {
        erro.response.data = JSON.parse(await erro.response.data.text());
      } catch {
        // Corpo sem JSON: fica a mensagem padrão da tela
      }
    }
    throw erro;
  }
  const nome = nomeDoCabecalho(resposta.headers['content-disposition']) || nomeReserva;
  const arquivo = new Blob([resposta.data], { type: 'application/octet-stream' });
  const endereco = URL.createObjectURL(arquivo);
  const link = document.createElement('a');
  link.href = endereco;
  link.download = nome;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(endereco), 10_000);
}
