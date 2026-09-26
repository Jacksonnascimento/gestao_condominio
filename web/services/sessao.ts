/** Cookie que guarda o token de acesso, enviado em toda chamada à API. */
export const COOKIE_SESSAO = 'condigtal_token';

/** Cookie que guarda o token de renovação, trocado por um par novo quando o de acesso vence. */
export const COOKIE_RENOVACAO = 'condigtal_renovacao';

export interface DadosToken {
  /** Código da pessoa. */
  sub?: string;
  /** Vencimento do token, em segundos desde 1970. */
  exp?: number;
}

/**
 * Lê as informações do token sem conferir a assinatura, que é validada pela API a cada chamada.
 * Serve para decidir a navegação; devolve null quando o token não é legível.
 */
export function lerToken(token?: string | null): DadosToken | null {
  const partes = token?.split('.');
  if (!partes || partes.length !== 3) return null;
  try {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const completo = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const bytes = Uint8Array.from(atob(completo), (c) => c.charCodeAt(0));
    const dados = JSON.parse(new TextDecoder().decode(bytes));
    return dados && typeof dados === 'object' ? dados : null;
  } catch {
    return null;
  }
}

/** Segundos que faltam para o token vencer; zero quando já venceu ou não é legível. */
export function segundosRestantes(token?: string | null): number {
  const exp = lerToken(token)?.exp;
  if (typeof exp !== 'number') return 0;
  return Math.max(0, Math.floor(exp - Date.now() / 1000));
}

export function tokenVigente(token?: string | null): boolean {
  return segundosRestantes(token) > 0;
}

/**
 * Há sessão enquanto um dos dois tokens vale: com o de acesso vencido, a primeira chamada à API renova o par
 * usando o de renovação, sem pedir login de novo.
 */
export function sessaoVigente(token?: string | null, tokenRenovacao?: string | null): boolean {
  return tokenVigente(token) || tokenVigente(tokenRenovacao);
}

function lerCookie(nome: string): string | null {
  if (typeof document === 'undefined') return null;
  const par = document.cookie.split('; ').find((item) => item.startsWith(`${nome}=`));
  return par ? decodeURIComponent(par.slice(nome.length + 1)) || null : null;
}

/** Token de acesso guardado no navegador, ou null fora dele e quando não há sessão. */
export function tokenDoNavegador(): string | null {
  return lerCookie(COOKIE_SESSAO);
}

export function tokenRenovacaoDoNavegador(): string | null {
  return lerCookie(COOKIE_RENOVACAO);
}

/**
 * Guarda no navegador os tokens entregues pela API, no login ou na renovação. Os cookies ficam no endereço do
 * condomínio (modelo.localhost, por exemplo): cada cliente tem a sua sessão, sem vazar para os outros.
 */
export function gravarSessao(token: string, tokenRenovacao: string) {
  // Cada cookie dura o mesmo que o seu token, para não sobrar sessão vencida no navegador
  document.cookie = `${COOKIE_SESSAO}=${token}; path=/; max-age=${segundosRestantes(token) || 7200}; SameSite=Strict`;
  document.cookie = `${COOKIE_RENOVACAO}=${tokenRenovacao}; path=/; max-age=${segundosRestantes(tokenRenovacao) || 7200}; SameSite=Strict`;
}

let encerrando = false;

/**
 * Apaga os dados da sessão e leva ao login. Com `expirada`, o login avisa que é preciso entrar
 * de novo. As várias chamadas que falham juntas numa tela disparam um único redirecionamento.
 */
export function encerrarSessao(expirada = false) {
  if (encerrando) return;
  encerrando = true;
  localStorage.clear();
  sessionStorage.clear();
  document.cookie = `${COOKIE_SESSAO}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
  document.cookie = `${COOKIE_RENOVACAO}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
  window.location.replace(expirada ? '/login?sessao=expirada' : '/login');
}
