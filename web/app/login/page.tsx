'use client';

import { Suspense, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Botao, Campo } from '@/components/Interface';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { autenticacaoService } from '@/services/autenticacaoService';
import { gravarSessao } from '@/services/sessao';
import { mensagemErroApi, statusDoErro } from '@/services/utilitarios';

function FormularioDeLogin() {
  const router = useRouter();
  const parametros = useSearchParams();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');

  const expirada = parametros.get('sessao') === 'expirada';

  async function entrar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      const resposta = await autenticacaoService.entrar(email.trim(), senha);
      gravarSessao(resposta.token, resposta.tokenRenovacao);
      router.replace('/');
    } catch (e) {
      setErro(
        statusDoErro(e) === 401 ? 'E-mail ou senha incorretos.' : mensagemErroApi(e, 'Não foi possível entrar. Tente de novo.'),
      );
      setEnviando(false);
    }
  }

  return (
    <TelaDeAcesso titulo="Entrar" subtitulo="Use o e-mail e a senha cadastrados no condomínio.">
      {expirada && !erro && (
        <p className="rounded-xl bg-aviso-fundo px-4 py-3 text-sm text-aviso" role="status">
          Sua sessão terminou. Entre de novo para continuar.
        </p>
      )}
      {erro && (
        <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo" role="alert">
          {erro}
        </p>
      )}
      <form onSubmit={entrar} className="flex flex-col gap-4">
        <Campo
          rotulo="E-mail"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          obrigatorio
        />
        <Campo
          rotulo="Senha"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          obrigatorio
        />
        <Botao type="submit" variante="primario" carregando={enviando} className="mt-2 h-12 w-full">
          Entrar
        </Botao>
      </form>
      <Link href="/esqueci-senha" className="self-center text-sm font-semibold">
        Esqueci minha senha
      </Link>
    </TelaDeAcesso>
  );
}

export default function PaginaDeLogin() {
  return (
    <Suspense>
      <FormularioDeLogin />
    </Suspense>
  );
}
