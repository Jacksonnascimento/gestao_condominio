import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { COOKIE_RENOVACAO, COOKIE_SESSAO, sessaoVigente } from '@/services/sessao';

/** Telas que abrem sem login. */
const TELAS_PUBLICAS = ['/login', '/esqueci-senha', '/definir-senha'];

export function proxy(request: NextRequest) {
  const token = request.cookies.get(COOKIE_SESSAO)?.value;
  const tokenRenovacao = request.cookies.get(COOKIE_RENOVACAO)?.value;
  const caminho = request.nextUrl.pathname;
  const naLogin = caminho.startsWith('/login');
  const publica = TELAS_PUBLICAS.some((tela) => caminho.startsWith(tela));

  // Sessão válida: o login não faz sentido, as demais telas seguem normalmente
  if (sessaoVigente(token, tokenRenovacao)) {
    return naLogin ? NextResponse.redirect(new URL('/', request.url)) : NextResponse.next();
  }

  let resposta = NextResponse.next();
  if (!publica) {
    const loginUrl = new URL('/login', request.url);
    if (token || tokenRenovacao) loginUrl.searchParams.set('sessao', 'expirada');
    resposta = NextResponse.redirect(loginUrl);
  }
  if (token) resposta.cookies.delete(COOKIE_SESSAO);
  if (tokenRenovacao) resposta.cookies.delete(COOKIE_RENOVACAO);
  return resposta;
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|icon.png|logo.png).*)'],
};
