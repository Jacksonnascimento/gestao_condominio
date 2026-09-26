'use client';

import { useState } from 'react';
import Link from 'next/link';
import { KeyRound, Mail } from 'lucide-react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import type { Papel } from '@/services/autenticacaoService';
import { confirmar } from '@/services/confirmacao';
import type { Opcao } from '@/services/tipos';
import { usuarioService, type AcessoDeUsuario } from '@/services/usuarioService';
import { formatarData, mensagemErroApi, valorDoEnum } from '@/services/utilitarios';

/**
 * Nome, e-mail (que é o login) e papel de quem tem acesso, e a senha: definida ali mesmo ou por um link mandado por
 * e-mail. Nome, e-mail e senha valem em todos os condomínios da pessoa; a API recusa a mudança quando ela está fora do
 * alcance de quem edita.
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
  const [definindoSenha, setDefinindoSenha] = useState(false);
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [salvandoSenha, setSalvandoSenha] = useState(false);
  const [erroSenha, setErroSenha] = useState('');

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

  function fecharSenha() {
    setDefinindoSenha(false);
    setNovaSenha('');
    setConfirmacao('');
    setErroSenha('');
  }

  async function salvarSenha(evento: React.FormEvent) {
    evento.preventDefault();
    if (novaSenha !== confirmacao) {
      setErroSenha('A confirmação não é igual à nova senha.');
      return;
    }
    setErroSenha('');
    setSalvandoSenha(true);
    try {
      await usuarioService.definirSenha(acesso.pessoaId, novaSenha);
      toast.success(`Senha alterada. ${acesso.pessoaNome} já entra com a senha nova.`);
      fecharSenha();
    } catch (e) {
      setErroSenha(mensagemErroApi(e, 'Não foi possível alterar a senha.'));
    } finally {
      setSalvandoSenha(false);
    }
  }

  const ocupado = salvando || enviandoLink || salvandoSenha;

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
          <Botao type="submit" form="editar-acesso" variante="primario" carregando={salvando} disabled={enviandoLink || salvandoSenha}>
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

        <div className="flex flex-col gap-4 rounded-xl bg-cabecalho p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex min-w-0 flex-col gap-0.5">
              <span className="text-sm font-bold">Senha</span>
              <span className="text-[13px] text-apagado">
                {eMeuAcesso ? (
                  <>
                    A sua senha você troca em <Link href="/perfil">Meu perfil</Link>.
                  </>
                ) : (
                  'Defina uma senha nova agora ou envie um link para a pessoa escolher.'
                )}
              </span>
            </div>
            {!definindoSenha && (
              <div className="flex flex-wrap gap-2 max-sm:w-full">
                {!eMeuAcesso && (
                  <Botao pequeno onClick={() => setDefinindoSenha(true)} disabled={ocupado} className="max-sm:h-11 max-sm:flex-1">
                    <KeyRound size={15} aria-hidden />
                    Definir senha
                  </Botao>
                )}
                <Botao pequeno onClick={enviarLink} carregando={enviandoLink} disabled={salvando} className="max-sm:h-11 max-sm:flex-1">
                  <Mail size={15} aria-hidden />
                  Enviar link
                </Botao>
              </div>
            )}
          </div>

          {definindoSenha && (
            <form onSubmit={salvarSenha} className="grid gap-4 border-t border-borda pt-4 sm:grid-cols-2">
              {erroSenha && <CaixaDeErro className="sm:col-span-2">{erroSenha}</CaixaDeErro>}
              <Campo
                rotulo="Nova senha"
                type="password"
                autoComplete="new-password"
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                obrigatorio
                minLength={6}
                ajuda="Pelo menos 6 caracteres."
                autoFocus
              />
              <Campo
                rotulo="Confirme a nova senha"
                type="password"
                autoComplete="new-password"
                value={confirmacao}
                onChange={(e) => setConfirmacao(e.target.value)}
                obrigatorio
                minLength={6}
              />
              <div className="flex flex-wrap items-center justify-between gap-3 sm:col-span-2">
                <span className="text-[13px] text-apagado">
                  Passe a senha para a pessoa. Quem estiver com o sistema aberto com a senha antiga precisa entrar de novo.
                </span>
                <div className="flex gap-2 max-sm:w-full">
                  <Botao pequeno variante="texto" onClick={fecharSenha} disabled={salvandoSenha} className="max-sm:h-11 max-sm:flex-1">
                    Cancelar
                  </Botao>
                  <Botao pequeno type="submit" variante="primario" carregando={salvandoSenha} disabled={salvando || enviandoLink} className="max-sm:h-11 max-sm:flex-1">
                    Salvar senha
                  </Botao>
                </div>
              </div>
            </form>
          )}
        </div>
      </div>
    </Modal>
  );
}
