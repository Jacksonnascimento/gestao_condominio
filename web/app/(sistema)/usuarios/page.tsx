'use client';

import { useEffect, useState } from 'react';
import { UserMinus, UserPlus } from 'lucide-react';
import toast from 'react-hot-toast';
import { DarAcesso } from '@/components/usuarios/DarAcesso';
import { EditarAcesso } from '@/components/usuarios/EditarAcesso';
import { SeloDoPapel } from '@/components/usuarios/SeloDoPapel';
import { Botao, CabecalhoDaPagina, Cartao, Paginacao, Vazio } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { confirmar } from '@/services/confirmacao';
import type { Pagina } from '@/services/tipos';
import { usuarioService, type AcessoDeUsuario, type OpcoesUsuario } from '@/services/usuarioService';
import { descricaoDoEnum, formatarData, iniciais, mensagemErroApi, valorDoEnum } from '@/services/utilitarios';

const TAMANHO = 20;
const COLUNAS = 'lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1.4fr)_150px_120px_150px]';

type Janela = { tipo: 'dar' } | { tipo: 'editar'; acesso: AcessoDeUsuario } | null;

export default function PaginaDeUsuarios() {
  const { condominio, usuario, permissoes, carregando: carregandoSessao } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesUsuario | null>(null);
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<AcessoDeUsuario> | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada acesso dado, alterado ou removido, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const permitido = permissoes.administraUsuarios;
  const consulta = JSON.stringify({ condominioId, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    if (!permitido) return;
    let ativa = true;
    usuarioService
      .opcoes()
      .then((novas) => {
        if (ativa) setOpcoes(novas);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os papéis.'));
      });
    return () => {
      ativa = false;
    };
  }, [permitido]);

  useEffect(() => {
    if (!permitido || !condominioId) return;
    let ativa = true;
    usuarioService
      .listar({ condominioId, pagina, tamanho: TAMANHO })
      .then((nova) => {
        if (ativa) setLista(nova);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os usuários.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, permitido, condominioId, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  async function remover(acesso: AcessoDeUsuario) {
    const papel = descricaoDoEnum(acesso.papel, acesso.papelDescricao);
    const confirmado = await confirmar({
      titulo: 'Remover acesso',
      mensagem: `${acesso.pessoaNome} deixa de ter acesso como ${papel} em ${acesso.condominioNome}. O cadastro da pessoa continua, e o acesso pode ser dado de novo depois.`,
      textoConfirmar: 'Remover acesso',
      perigo: true,
    });
    if (!confirmado) return;
    try {
      await usuarioService.remover(acesso, valorDoEnum(acesso.papel));
      toast.success('Acesso removido.');
      // Se era o último da página, volta uma página
      if (lista && lista.itens.length === 1 && pagina > 0) setPagina(pagina - 1);
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível remover o acesso.'));
    }
  }

  // O condomínio escolhido precisa estar entre os que quem está logado administra
  const administraEste = opcoes?.podeGerenciar === true && opcoes.condominios.some((c) => c.codigo === condominioId);
  const itens = lista?.itens ?? [];

  if (!carregandoSessao && !permitido) {
    return (
      <div className="flex flex-col gap-5">
        <CabecalhoDaPagina secao="Administração" titulo="Usuários" />
        <Cartao>
          <Vazio>Só o síndico e a administração do condomínio cuidam de quem tem acesso ao sistema.</Vazio>
        </Cartao>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Administração" titulo="Usuários">
        {administraEste && condominio && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'dar' })}>
            <UserPlus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Dar acesso
          </Botao>
        )}
      </CabecalhoDaPagina>

      <p className="text-sm text-apagado">
        Quem entra no sistema neste condomínio e o papel de cada pessoa. O papel define o que ela vê e pode fazer.
      </p>

      <Cartao aria-label="Pessoas com acesso" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Nome</span>
          <span>E-mail</span>
          <span>Papel</span>
          <span>Desde</span>
          <span className="text-right">Ações</span>
        </div>

        {!condominioId && !carregandoSessao && <Vazio>Nenhum condomínio disponível.</Vazio>}
        {!carregando && itens.length === 0 && <Vazio>Ninguém tem acesso a este condomínio ainda.</Vazio>}

        <ul className="m-0 list-none p-0">
          {itens.map((acesso) => {
            const eu = acesso.pessoaId === usuario?.codigo;
            return (
              <li
                key={`${acesso.pessoaId}-${acesso.condominioId}-${valorDoEnum(acesso.papel)}`}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm last:border-b-0 lg:min-h-[56px] lg:py-2 ${COLUNAS}`}
              >
                <span className="flex min-w-0 items-center gap-3">
                  <span
                    aria-hidden
                    className="flex size-9 shrink-0 items-center justify-center rounded-full bg-realce text-xs font-bold text-ouro"
                  >
                    {iniciais(acesso.pessoaNome)}
                  </span>
                  <span className="min-w-0 truncate font-bold">
                    {acesso.pessoaNome}
                    {eu && <span className="font-semibold text-apagado"> (você)</span>}
                  </span>
                </span>
                <span className="justify-self-end lg:hidden">
                  <SeloDoPapel acesso={acesso} />
                </span>
                <span className="col-span-2 truncate text-tinta-2 lg:col-span-1">{acesso.pessoaEmail}</span>
                <span className="hidden lg:flex">
                  <SeloDoPapel acesso={acesso} />
                </span>
                <span className="text-apagado tabular-nums">
                  <span className="lg:hidden">Desde </span>
                  {formatarData(acesso.dataAssociacao) || '—'}
                </span>
                <span className="flex items-center justify-end gap-1.5">
                  {administraEste && (
                    <>
                      <Botao pequeno onClick={() => setJanela({ tipo: 'editar', acesso })} className="max-lg:h-11">
                        Editar
                      </Botao>
                      {!eu && (
                        <button
                          type="button"
                          aria-label={`Remover acesso de ${acesso.pessoaNome}`}
                          title="Remover acesso"
                          onClick={() => remover(acesso)}
                          className="flex size-11 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-perigo-fundo hover:text-perigo lg:size-[34px]"
                        >
                          <UserMinus size={18} aria-hidden />
                        </button>
                      )}
                    </>
                  )}
                </span>
              </li>
            );
          })}
        </ul>

        {lista && (
          <Paginacao
            pagina={lista.pagina}
            totalPaginas={lista.totalPaginas}
            totalItens={lista.totalItens}
            tamanho={TAMANHO}
            aoMudar={setPagina}
            nome="acessos"
          />
        )}
      </Cartao>

      {janela?.tipo === 'dar' && condominio && opcoes && (
        <DarAcesso
          condominio={condominio}
          papeis={opcoes.papeis}
          acoesSenha={opcoes.acoesSenha}
          aoFechar={() => setJanela(null)}
          aoSalvar={aposAlterar}
        />
      )}
      {janela?.tipo === 'editar' && opcoes && (
        <EditarAcesso
          acesso={janela.acesso}
          papeis={opcoes.papeis}
          eMeuAcesso={janela.acesso.pessoaId === usuario?.codigo}
          aoFechar={() => setJanela(null)}
          aoSalvar={aposAlterar}
        />
      )}
    </div>
  );
}
