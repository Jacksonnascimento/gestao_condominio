'use client';

import { useEffect, useState } from 'react';
import { Trash2, UserPlus } from 'lucide-react';
import toast from 'react-hot-toast';
import { DarAcesso } from '@/components/usuarios/DarAcesso';
import { EditarAcesso } from '@/components/usuarios/EditarAcesso';
import { SeloDoPapel } from '@/components/usuarios/SeloDoPapel';
import { Abas, Botao, CabecalhoDaPagina, Cartao, Paginacao, Selo, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { confirmar } from '@/services/confirmacao';
import type { Pagina } from '@/services/tipos';
import { usuarioService, type AcessoDeUsuario, type OpcoesUsuario, type TotaisAcessos } from '@/services/usuarioService';
import { descricaoDoEnum, formatarData, iniciais, mensagemErroApi, valorDoEnum } from '@/services/utilitarios';

const TAMANHO = 20;
const COLUNAS = 'lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1.4fr)_130px_100px_190px]';

type Situacao = 'ativos' | 'desativados' | 'todos';

type Janela = { tipo: 'dar' } | { tipo: 'editar'; acesso: AcessoDeUsuario } | null;

const ATIVO_DA_SITUACAO: Record<Situacao, boolean | null> = { ativos: true, desativados: false, todos: null };

export default function PaginaDeUsuarios() {
  const { condominio, usuario, permissoes, carregando: carregandoSessao } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesUsuario | null>(null);
  const [situacao, setSituacao] = useState<Situacao>('ativos');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<AcessoDeUsuario> | null>(null);
  const [totais, setTotais] = useState<TotaisAcessos | null>(null);
  const [emAndamento, setEmAndamento] = useState('');
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada acesso dado, alterado ou removido, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const permitido = permissoes.administraUsuarios;
  const consulta = JSON.stringify({ condominioId, situacao, pagina, versao });
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
    Promise.all([
      usuarioService.listar({ condominioId, ativo: ATIVO_DA_SITUACAO[situacao], pagina, tamanho: TAMANHO }),
      usuarioService.totais(condominioId),
    ])
      .then(([nova, novosTotais]) => {
        if (!ativa) return;
        setLista(nova);
        setTotais(novosTotais);
        // Desativar ou reativar o último acesso de uma página a deixa vazia
        if (nova.itens.length === 0 && pagina > 0) setPagina(Math.max(nova.totalPaginas - 1, 0));
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
  }, [consulta, permitido, condominioId, situacao, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  const chaveDoAcesso = (acesso: AcessoDeUsuario) => `${acesso.pessoaId}-${acesso.condominioId}-${valorDoEnum(acesso.papel)}`;

  async function mudarSituacao(acesso: AcessoDeUsuario) {
    const papel = descricaoDoEnum(acesso.papel, acesso.papelDescricao);
    const ativo = acesso.ativo !== false;
    if (ativo) {
      const confirmado = await confirmar({
        titulo: 'Desativar acesso',
        mensagem: `${acesso.pessoaNome} deixa de entrar como ${papel} em ${acesso.condominioNome}. O acesso fica guardado, com a data de início, e pode ser reativado quando quiser.`,
        textoConfirmar: 'Desativar',
        perigo: true,
      });
      if (!confirmado) return;
    }
    setEmAndamento(chaveDoAcesso(acesso));
    try {
      if (ativo) await usuarioService.desativar(acesso, valorDoEnum(acesso.papel));
      else await usuarioService.reativar(acesso, valorDoEnum(acesso.papel));
      toast.success(ativo ? 'Acesso desativado.' : 'Acesso reativado.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, ativo ? 'Não foi possível desativar o acesso.' : 'Não foi possível reativar o acesso.'));
    } finally {
      setEmAndamento('');
    }
  }

  async function remover(acesso: AcessoDeUsuario) {
    const papel = descricaoDoEnum(acesso.papel, acesso.papelDescricao);
    const confirmado = await confirmar({
      titulo: 'Remover acesso',
      mensagem: `O acesso de ${acesso.pessoaNome} como ${papel} em ${acesso.condominioNome} será apagado, com a data de início. O cadastro da pessoa continua. Para só suspender o acesso, use “Desativar”.`,
      textoConfirmar: 'Remover acesso',
      perigo: true,
    });
    if (!confirmado) return;
    setEmAndamento(chaveDoAcesso(acesso));
    try {
      await usuarioService.remover(acesso, valorDoEnum(acesso.papel));
      toast.success('Acesso removido.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível remover o acesso.'));
    } finally {
      setEmAndamento('');
    }
  }

  const abas: Aba<Situacao>[] = [
    { valor: 'ativos', rotulo: 'Ativos', contagem: totais?.ATIVOS },
    { valor: 'desativados', rotulo: 'Desativados', contagem: totais?.DESATIVADOS },
    { valor: 'todos', rotulo: 'Todos', contagem: totais?.TOTAL },
  ];

  const mensagensVazias: Record<Situacao, string> = {
    ativos: 'Ninguém tem acesso ativo a este condomínio.',
    desativados: 'Nenhum acesso desativado.',
    todos: 'Ninguém tem acesso a este condomínio ainda.',
  };

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

      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-apagado">
          Quem entra no sistema neste condomínio e o papel de cada pessoa. O papel define o que ela vê e pode fazer.
        </p>
        <Abas
          rotulo="Situação do acesso"
          abas={abas}
          valor={situacao}
          aoMudar={(valor) => {
            setSituacao(valor);
            setPagina(0);
          }}
        />
      </div>

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
        {!carregando && condominioId && itens.length === 0 && <Vazio>{mensagensVazias[situacao]}</Vazio>}

        <ul className="m-0 list-none p-0">
          {itens.map((acesso) => {
            const eu = acesso.pessoaId === usuario?.codigo;
            const desativado = acesso.ativo === false;
            const ocupado = emAndamento === chaveDoAcesso(acesso);
            return (
              <li
                key={chaveDoAcesso(acesso)}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm last:border-b-0 lg:min-h-[56px] lg:py-2 ${COLUNAS}`}
              >
                <span className="flex min-w-0 items-center gap-3">
                  <span
                    aria-hidden
                    className="flex size-9 shrink-0 items-center justify-center rounded-full bg-realce text-xs font-bold text-ouro"
                  >
                    {iniciais(acesso.pessoaNome)}
                  </span>
                  <span className={`min-w-0 truncate font-bold ${desativado ? 'text-apagado' : ''}`}>
                    {acesso.pessoaNome}
                    {eu && <span className="font-semibold text-apagado"> (você)</span>}
                  </span>
                </span>
                <span className="flex flex-wrap justify-end gap-1.5 justify-self-end lg:hidden">
                  <SeloDoPapel acesso={acesso} />
                  {desativado && <Selo tom="perigo">Desativado</Selo>}
                </span>
                <span className="col-span-2 truncate text-tinta-2 lg:col-span-1">{acesso.pessoaEmail}</span>
                <span className="hidden flex-wrap gap-1.5 lg:flex">
                  <SeloDoPapel acesso={acesso} />
                  {desativado && <Selo tom="perigo">Desativado</Selo>}
                </span>
                <span className="text-apagado tabular-nums">
                  <span className="lg:hidden">Desde </span>
                  {formatarData(acesso.dataAssociacao) || '—'}
                </span>
                <span className="flex items-center justify-end gap-1.5">
                  {administraEste && (
                    <>
                      <Botao pequeno onClick={() => setJanela({ tipo: 'editar', acesso })} disabled={ocupado} className="max-lg:h-11">
                        Editar
                      </Botao>
                      {!eu && (
                        <>
                          <Botao
                            pequeno
                            variante={desativado ? 'primario' : 'texto'}
                            onClick={() => mudarSituacao(acesso)}
                            disabled={ocupado}
                            className="max-lg:h-11"
                          >
                            {desativado ? 'Reativar' : 'Desativar'}
                          </Botao>
                          <button
                            type="button"
                            aria-label={`Remover o acesso de ${acesso.pessoaNome}`}
                            title="Remover acesso"
                            onClick={() => remover(acesso)}
                            disabled={ocupado}
                            className="flex size-11 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-perigo-fundo hover:text-perigo disabled:cursor-not-allowed disabled:opacity-50 lg:size-[34px]"
                          >
                            <Trash2 size={17} aria-hidden />
                          </button>
                        </>
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
