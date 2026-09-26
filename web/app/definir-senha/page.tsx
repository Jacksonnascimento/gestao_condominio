'use client';

import { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Botao, CaixaDeErro, Campo } from '@/components/Interface';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { autenticacaoService } from '@/services/autenticacaoService';
import { mensagemErroApi } from '@/services/utilitarios';

type Etapa = 'conferindo' | 'invalido' | 'formulario' | 'concluido';

/** Tela aberta pelo link do e-mail de redefinição (/definir-senha?token=...). */
function DefinirSenha() {
  const token = useSearchParams().get('token') ?? '';
  const [etapa, setEtapa] = useState<Etapa>(token ? 'conferindo' : 'invalido');
  const [mensagem, setMensagem] = useState(token ? '' : 'O link está incompleto. Peça um novo na tela de login.');
  const [senha, setSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');

  useEffect(() => {
    if (!token) return;
    autenticacaoService
      .conferirLinkDeSenha(token)
      .then(() => setEtapa('formulario'))
      .catch((e) => {
        setMensagem(mensagemErroApi(e, 'Este link não vale mais. Peça um novo na tela de login.'));
        setEtapa('invalido');
      });
  }, [token]);

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    if (senha !== confirmacao) {
      setErro('As duas senhas não são iguais.');
      return;
    }
    setEnviando(true);
    try {
      const resposta = await autenticacaoService.redefinirSenha(token, senha);
      setMensagem(resposta.mensagem);
      setEtapa('concluido');
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível salvar a senha. Tente de novo.'));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <TelaDeAcesso titulo="Criar senha nova" subtitulo={etapa === 'formulario' ? 'Use pelo menos 6 caracteres.' : undefined}>
      {etapa === 'conferindo' && (
        <p className="text-sm text-apagado" role="status">
          Conferindo o link…
        </p>
      )}
      {etapa === 'invalido' && (
        <CaixaDeErro>{mensagem}</CaixaDeErro>
      )}
      {etapa === 'concluido' && (
        <p className="rounded-xl bg-realce px-4 py-3 text-sm text-tinta" role="status">
          {mensagem}
        </p>
      )}
      {etapa === 'formulario' && (
        <form onSubmit={salvar} className="flex flex-col gap-4">
          {erro && <CaixaDeErro>{erro}</CaixaDeErro>}
          <Campo
            rotulo="Nova senha"
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            obrigatorio
          />
          <Campo
            rotulo="Repita a nova senha"
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={confirmacao}
            onChange={(e) => setConfirmacao(e.target.value)}
            obrigatorio
          />
          <Botao type="submit" variante="primario" carregando={enviando} className="mt-2 h-12 w-full">
            Salvar senha
          </Botao>
        </form>
      )}
      <Link href={etapa === 'invalido' ? '/esqueci-senha' : '/login'} className="self-center text-sm font-semibold">
        {etapa === 'invalido' ? 'Pedir um link novo' : 'Ir para o login'}
      </Link>
    </TelaDeAcesso>
  );
}

export default function PaginaDefinirSenha() {
  return (
    <Suspense>
      <DefinirSenha />
    </Suspense>
  );
}
