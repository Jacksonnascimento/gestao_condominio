'use client';

import { useState } from 'react';
import { KeyRound } from 'lucide-react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import type { Papel } from '@/services/autenticacaoService';
import { confirmar } from '@/services/confirmacao';
import type { Opcao } from '@/services/tipos';
import { usuarioService, type AcessoDeUsuario } from '@/services/usuarioService';
import { formatarData, mensagemErroApi, valorDoEnum } from '@/services/utilitarios';

/**
 * Nome, e-mail (que é o login) e papel de quem tem acesso, e o envio do link para definir uma nova senha. Nome e
 * e-mail valem em todos os condomínios da pessoa; a API recusa a mudança quando ela está fora do alcance de quem edita.
 */
export function EditarAcesso({
  acesso,
  papeis,
  eMeuAcesso,
  aoFechar,
  aoSalvar,
}: {
  acesso: AcessoDeUsuario;
  papeis: Opcao[];
  /** Ninguém muda o próprio papel. */
  eMeuAcesso: boolean;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const papelAtual = valorDoEnum(acesso.papel);
  const [nome, setNome] = useState(acesso.pessoaNome ?? '');
  const [email, setEmail] = useState(acesso.pessoaEmail ?? '');
  const [papel, setPapel] = useState(papelAtual);
  const [salvando, setSalvando] = useState(false);
  const [enviandoLink, setEnviandoLink] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      await usuarioService.editar(acesso, papelAtual, { nome: nome.trim(), email: email.trim(), papel: papel as Papel });
      toast.success('Acesso atualizado.');
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível salvar as alterações.'));
      setSalvando(false);
    }
  }

  async function enviarLink() {
    const confirmado = await confirmar({
      titulo: 'Enviar link de senha',
      mensagem: `${acesso.pessoaNome} vai receber em ${acesso.pessoaEmail} um link para definir uma nova senha. O link vale por 24 horas, e a senha atual continua valendo até ser trocada.`,
      textoConfirmar: 'Enviar link',
    });
    if (!confirmado) return;
    setEnviandoLink(true);
    try {
      const resposta = await usuarioService.enviarLinkDeSenha(acesso.pessoaId);
      toast.success(resposta.mensagem || 'Link enviado.');
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível enviar o link.'));
    } finally {
      setEnviandoLink(false);
    }
  }

  const ocupado = salvando || enviandoLink;

  return (
    <Modal
      titulo="Editar acesso"
      subtitulo={[acesso.condominioNome, acesso.dataAssociacao ? `acesso desde ${formatarData(acesso.dataAssociacao)}` : null]
        .filter(Boolean)
        .join(' · ')}
      aoFechar={aoFechar}
      ocupado={ocupado}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={ocupado}>
            Cancelar
          </Botao>
          <Botao type="submit" form="editar-acesso" variante="primario" carregando={salvando} disabled={enviandoLink}>
            Salvar
          </Botao>
        </>
      }
    >
      <div className="flex flex-col gap-5">
        <form id="editar-acesso" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
          {erro && <CaixaDeErro className="sm:col-span-2">{erro}</CaixaDeErro>}
          <Campo rotulo="Nome" value={nome} onChange={(e) => setNome(e.target.value)} obrigatorio maxLength={100} className="sm:col-span-2" />
          <Campo
            rotulo="E-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            obrigatorio
            maxLength={100}
            ajuda="É com ele que a pessoa entra no sistema."
            className="sm:col-span-2"
          />
          <CampoDeSelecao
            rotulo="Papel"
            value={papel}
            onChange={(e) => setPapel(e.target.value)}
            obrigatorio
            disabled={eMeuAcesso}
            ajuda={eMeuAcesso ? 'Você não pode mudar o seu próprio papel.' : undefined}
            className="sm:col-span-2"
          >
            {papeis.map((p) => (
              <option key={p.valor} value={p.valor}>
                {p.descricao}
              </option>
            ))}
          </CampoDeSelecao>
        </form>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-cabecalho p-4">
          <div className="flex min-w-0 flex-col gap-0.5">
            <span className="text-sm font-bold">Senha</span>
            <span className="text-[13px] text-apagado">Esqueceu a senha? Envie um link para a pessoa definir outra.</span>
          </div>
          <Botao pequeno onClick={enviarLink} carregando={enviandoLink} disabled={salvando} className="max-sm:h-11 max-sm:w-full">
            <KeyRound size={15} aria-hidden />
            Enviar link de senha
          </Botao>
        </div>
      </div>
    </Modal>
  );
}
