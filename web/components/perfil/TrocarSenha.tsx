'use client';

import { useState } from 'react';
import { Botao, Campo, Cartao, TituloDoCartao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { perfilService } from '@/services/perfilService';
import { encerrarSessao } from '@/services/sessao';
import { mensagemErroApi } from '@/services/utilitarios';

/**
 * Troca de senha. A API não devolve tokens novos: a troca derruba a sessão atual (e as dos outros aparelhos), então,
 * depois dela, a pessoa entra de novo com a senha nova.
 */
export function TrocarSenha() {
  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [trocada, setTrocada] = useState<string | null>(null);

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    if (novaSenha !== confirmacao) {
      setErro('A confirmação não é igual à nova senha.');
      return;
    }
    if (novaSenha === senhaAtual) {
      setErro('A nova senha precisa ser diferente da atual.');
      return;
    }
    setErro('');
    setSalvando(true);
    try {
      const resposta = await perfilService.trocarSenha(senhaAtual, novaSenha);
      setTrocada(resposta.mensagem || 'Senha alterada. Entre novamente com a nova senha.');
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível trocar a senha.'));
      setSalvando(false);
    }
  }

  return (
    <Cartao>
      <TituloDoCartao titulo="Senha" />
      <form onSubmit={salvar} className="grid gap-4 border-t border-borda-suave px-5 py-5 sm:grid-cols-2">
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo sm:col-span-2" role="alert">
            {erro}
          </p>
        )}
        <Campo
          rotulo="Senha atual"
          type="password"
          autoComplete="current-password"
          value={senhaAtual}
          onChange={(e) => setSenhaAtual(e.target.value)}
          obrigatorio
          className="sm:col-span-2"
        />
        <Campo
          rotulo="Nova senha"
          type="password"
          autoComplete="new-password"
          value={novaSenha}
          onChange={(e) => setNovaSenha(e.target.value)}
          obrigatorio
          minLength={6}
          ajuda="Pelo menos 6 caracteres."
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
          <span className="text-[13px] text-apagado">Depois de trocar, você entra de novo com a senha nova, em todos os aparelhos.</span>
          <Botao type="submit" variante="primario" carregando={salvando} className="max-sm:w-full">
            Trocar senha
          </Botao>
        </div>
      </form>

      {trocada && (
        <Modal
          titulo="Senha alterada"
          largura="sm"
          aoFechar={() => encerrarSessao()}
          rodape={
            <Botao variante="primario" onClick={() => encerrarSessao()}>
              Entrar de novo
            </Botao>
          }
        >
          <p className="text-sm leading-relaxed text-tinta-2">{trocada}</p>
        </Modal>
      )}
    </Cartao>
  );
}
