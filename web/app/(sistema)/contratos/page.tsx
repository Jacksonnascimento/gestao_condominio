'use client';

import { useEffect, useState } from 'react';
import { EllipsisVertical, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { DetalhesDoContrato } from '@/components/contratos/DetalhesDoContrato';
import { FormularioDoContrato } from '@/components/contratos/FormularioDoContrato';
import { SeloDoContrato, textoDaVigencia, textoDoVencimento } from '@/components/contratos/SeloDoContrato';
import { Abas, Botao, CabecalhoDaPagina, CampoDeBusca, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { confirmar } from '@/services/confirmacao';
import {
  contratoService,
  pedidoDoContrato,
  type AbaContrato,
  type Contrato,
  type OpcoesContrato,
  type TotaisContratos,
} from '@/services/contratoService';
import type { Pagina } from '@/services/tipos';
import { formatarMoeda, mensagemErroApi } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[1.2fr_1.4fr_120px_1.3fr_112px_128px]';

type Janela = { tipo: 'novo' } | { tipo: 'editar'; contrato: Contrato } | { tipo: 'detalhes'; contrato: Contrato } | null;
type SituacaoDoHistorico = '' | 'FINALIZADO' | 'RESCINDIDO';

export default function PaginaDeContratos() {
  const { condominio, permissoes } = useSessao();
  const gestao = permissoes.gestao;
  const [opcoes, setOpcoes] = useState<OpcoesContrato | null>(null);
  const [aba, setAba] = useState<AbaContrato>('ATIVOS');
  const [situacaoDoHistorico, setSituacaoDoHistorico] = useState<SituacaoDoHistorico>('');
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Contrato> | null>(null);
  const [totais, setTotais] = useState<TotaisContratos | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada cadastro, edição, rescisão ou exclusão, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const status = aba === 'HISTORICO' ? situacaoDoHistorico : '';
  const consulta = JSON.stringify({ condominioId, busca, aba, status, pagina, versao });
  const carregando = gestao && consulta !== consultaCarregada;

  useEffect(() => {
    if (!gestao) return;
    let ativa = true;
    contratoService
      .opcoes()
      .then((novas) => {
        if (ativa) setOpcoes(novas);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as opções da tela de contratos.'));
      });
    return () => {
      ativa = false;
    };
  }, [gestao]);

  // A busca vai para a API só quando a pessoa para de digitar
  useEffect(() => {
    const espera = setTimeout(() => {
      setBusca(buscaDigitada.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(espera);
  }, [buscaDigitada]);

  useEffect(() => {
    // Contratos são só da gestão: para os demais a API recusa, e a tela explica em vez de chamar
    if (!gestao) return;
    let ativa = true;
    Promise.all([
      contratoService.listar({ aba, condominioId, busca, status, pagina, tamanho: TAMANHO }),
      contratoService.totais(condominioId),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        setLista(novaLista);
        setTotais(novosTotais);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os contratos.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [gestao, consulta, condominioId, busca, aba, status, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  // A confirmação abre por cima da janela de detalhes; confirmada, a janela fecha
  async function rescindir(contrato: Contrato) {
    const confirmado = await confirmar({
      titulo: 'Rescindir contrato',
      mensagem: `O contrato com ${contrato.empresa} (${contrato.servico}) passa a constar como rescindido e vai para o histórico. Se precisar, dá para voltar atrás editando o contrato.`,
      textoConfirmar: 'Rescindir',
      perigo: true,
    });
    if (!confirmado) return;
    setJanela(null);
    try {
      await contratoService.atualizar(contrato.id, pedidoDoContrato(contrato, 'RESCINDIDO'));
      toast.success('Contrato rescindido.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível rescindir o contrato.'));
    }
  }

  async function excluir(contrato: Contrato) {
    const confirmado = await confirmar({
      titulo: 'Excluir contrato',
      mensagem: `O contrato com ${contrato.empresa} (${contrato.servico}) será apagado do sistema. Essa ação não pode ser desfeita.`,
      textoConfirmar: 'Excluir',
      perigo: true,
    });
    if (!confirmado) return;
    setJanela(null);
    try {
      await contratoService.excluir(contrato.id);
      toast.success('Contrato excluído.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível excluir o contrato.'));
    }
  }

  const abas: Aba<AbaContrato>[] = [
    { valor: 'ATIVOS', rotulo: 'Ativos', contagem: totais?.ativos },
    { valor: 'A_VENCER', rotulo: 'Vencendo', contagem: totais?.aVencer, destaque: true },
    { valor: 'HISTORICO', rotulo: 'Histórico', contagem: totais ? totais.finalizados + totais.rescindidos : null },
  ];
  const abasDoHistorico: Aba<SituacaoDoHistorico>[] = [
    { valor: '', rotulo: 'Todos' },
    { valor: 'FINALIZADO', rotulo: 'Vencidos', contagem: totais?.finalizados },
    { valor: 'RESCINDIDO', rotulo: 'Rescindidos', contagem: totais?.rescindidos },
  ];

  const podeGerenciar = gestao && (opcoes ? opcoes.podeGerenciar : true);
  const itens = lista?.itens ?? [];

  const mensagensVazias: Record<AbaContrato, string> = {
    ATIVOS: 'Nenhum contrato ativo.',
    A_VENCER: 'Nenhum contrato vencendo nos próximos 30 dias.',
    HISTORICO: 'Nenhum contrato vencido ou rescindido.',
  };

  if (!gestao) {
    return (
      <div className="flex flex-col gap-5">
        <CabecalhoDaPagina secao="Cadastros" titulo="Contratos" />
        <Cartao>
          <Vazio>Os contratos do condomínio ficam com a gestão: síndico e administração.</Vazio>
        </Cartao>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Cadastros" titulo="Contratos">
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'novo' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Novo contrato
          </Botao>
        )}
      </CabecalhoDaPagina>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex max-w-full flex-wrap items-center gap-2.5">
          <Abas
            rotulo="Situação"
            abas={abas}
            valor={aba}
            aoMudar={(valor) => {
              setAba(valor);
              setPagina(0);
            }}
          />
          {aba === 'HISTORICO' && (
            <Abas
              rotulo="Situação no histórico"
              abas={abasDoHistorico}
              valor={situacaoDoHistorico}
              aoMudar={(valor) => {
                setSituacaoDoHistorico(valor);
                setPagina(0);
              }}
            />
          )}
        </div>
        <CampoDeBusca valor={buscaDigitada} aoMudar={setBuscaDigitada} rotulo="Buscar contratos" dica="Empresa ou serviço" />
      </div>

      <Cartao aria-label="Lista de contratos" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Empresa</span>
          <span>Serviço</span>
          <span className="text-right">Valor</span>
          <span>Vigência</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && (
          <Vazio>{busca ? 'Nenhum contrato encontrado com essa busca.' : mensagensVazias[aba]}</Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((contrato) => {
            const vencimento = textoDoVencimento(contrato);
            return (
              <li
                key={contrato.id}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[56px] lg:py-2 ${COLUNAS}`}
              >
                <span className="flex min-w-0 flex-col">
                  <span className="font-bold break-words">{contrato.empresa}</span>
                  {contrato.responsavel && <span className="text-[13px] text-apagado">{contrato.responsavel}</span>}
                </span>
                <span className="justify-self-end lg:hidden">
                  <SeloDoContrato contrato={contrato} />
                </span>
                <span className="col-span-2 break-words text-tinta-2 lg:col-span-1">{contrato.servico}</span>
                <span className="hidden text-right font-semibold lg:block">{formatarMoeda(contrato.valor)}</span>
                <span className="flex flex-col text-apagado lg:text-tinta-2">
                  <span>
                    <span className="font-semibold text-tinta lg:hidden">{formatarMoeda(contrato.valor)} · </span>
                    {textoDaVigencia(contrato)}
                  </span>
                  {vencimento && <span className="text-[13px] font-semibold text-aviso">{vencimento}</span>}
                </span>
                <span className="hidden lg:flex">
                  <SeloDoContrato contrato={contrato} />
                </span>
                <span className="flex items-center justify-end gap-1.5">
                  {podeGerenciar && (
                    <Botao pequeno className="max-lg:h-11" onClick={() => setJanela({ tipo: 'editar', contrato })}>
                      Editar
                    </Botao>
                  )}
                  <button
                    type="button"
                    aria-label={`Detalhes do contrato com ${contrato.empresa}`}
                    onClick={() => setJanela({ tipo: 'detalhes', contrato })}
                    className="flex size-11 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-tinta lg:size-[34px]"
                  >
                    <EllipsisVertical size={18} aria-hidden />
                  </button>
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
            nome="contratos"
          />
        )}
      </Cartao>

      {janela?.tipo === 'novo' && <FormularioDoContrato aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />}
      {janela?.tipo === 'editar' && (
        <FormularioDoContrato contrato={janela.contrato} aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />
      )}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDoContrato
          contrato={janela.contrato}
          podeGerenciar={podeGerenciar}
          aoFechar={() => setJanela(null)}
          aoEditar={(contrato) => setJanela({ tipo: 'editar', contrato })}
          aoRescindir={rescindir}
          aoExcluir={excluir}
        />
      )}
    </div>
  );
}
