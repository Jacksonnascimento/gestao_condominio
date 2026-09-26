'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Botao, CaixaDeErro, Campo } from '@/components/Interface';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { autenticacaoService } from '@/services/autenticacaoService';
import { mensagemErroApi } from '@/services/utilitarios';

export default function PaginaEsqueciSenha() {
  const [email, setEmail] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [resposta, setResposta] = useState('');
  const [erro, setErro] = useState('');

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      // A API responde a mesma mensagem exista ou não o e-mail, para não revelar quem tem cadastro
      const { mensagem } = await autenticacaoService.esqueciSenha(email.trim());
      setResposta(mensagem);
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível enviar o link. Tente de novo.'));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <TelaDeAcesso titulo="Esqueci a senha" subtitulo="Informe o seu e-mail e enviaremos um link para criar uma senha nova.">
      {resposta ? (
        <p className="rounded-xl bg-realce px-4 py-3 text-sm leading-relaxed text-tinta" role="status">
          {resposta}
        </p>
      ) : (
        <form onSubmit={enviar} className="flex flex-col gap-4">
          {erro && <CaixaDeErro>{erro}</CaixaDeErro>}
          <Campo rotulo="E-mail" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} obrigatorio />
          <Botao type="submit" variante="primario" carregando={enviando} className="mt-2 h-12 w-full">
            Enviar link
          </Botao>
        </form>
      )}
      <Link href="/login" className="self-center text-sm font-semibold">
        Voltar para o login
      </Link>
    </TelaDeAcesso>
  );
}
