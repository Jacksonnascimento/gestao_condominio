'use client';

import { useEffect, useState } from 'react';
import { EllipsisVertical, Plus, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import { DetalhesDaEncomenda } from '@/components/encomendas/DetalhesDaEncomenda';
import { EntregarEncomenda } from '@/components/encomendas/EntregarEncomenda';
import { RegistrarEncomenda } from '@/components/encomendas/RegistrarEncomenda';
import { SeloDaEncomenda, estaAguardando } from '@/components/encomendas/SeloDaEncomenda';
import { Abas, Botao, CabecalhoDaPagina, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import {
  encomendaService,
  type Encomenda,
  type OpcoesEncomenda,
  type SituacaoEncomenda,
  type TotaisEncomendas,
} from '@/services/encomendaService';
import type { Pagina } from '@/services/tipos';
import { formatarMomento, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[96px_1.3fr_1.2fr_1fr_1fr_0.9fr_118px_140px]';

type Janela =
  | { tipo: 'registrar' }
  | { tipo: 'entregar'; encomenda: Encomenda }
  | { tipo: 'detalhes'; encomenda: Encomenda }
  | null;

export default function PaginaDeEncomendas() {
  const { condominio } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesEncomenda | null>(null);
  const [situacao, setSituacao] = useState<SituacaoEncomenda | ''>('');
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Encomenda> | null>(null);
  const [totais, setTotais] = useState<TotaisEncomendas | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada registro, entrega ou mudança de situação, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const consulta = JSON.stringify({ condominioId, busca, situacao, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    encomendaService
      .opcoes()
      .then(setOpcoes)
      .catch((e) => toast.error(mensagemErroApi(e, 'Não foi possível carregar os tipos de encomenda.')));
  }, []);

  // A busca vai para a API só quando a pessoa para de digitar
  useEffect(() => {
    const espera = setTimeout(() => {
      setBusca(buscaDigitada.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(espera);
  }, [buscaDigitada]);

  useEffect(() => {
    let ativa = true;
    Promise.all([
      encomendaService.listar({ condominioId, busca, status: situacao, pagina, tamanho: TAMANHO }),
      encomendaService.totais({ condominioId, busca }),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        setLista(novaLista);
        setTotais(novosTotais);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as encomendas.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, busca, situacao, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  const abas: Aba<SituacaoEncomenda | ''>[] = [
    { valor: '', rotulo: 'Todas', contagem: totais?.TOTAL },
    { valor: 'PENDENTE', rotulo: 'Aguardando', contagem: totais?.PENDENTES, destaque: true },
    { valor: 'RETIRADA', rotulo: 'Retiradas', contagem: totais?.RETIRADAS },
    { valor: 'DEVOLVIDA', rotulo: 'Devolvidas', contagem: totais?.DEVOLVIDAS },
    { valor: 'EXTRAVIADA', rotulo: 'Extraviadas', contagem: totais?.EXTRAVIADAS },
  ];

  const podeGerenciar = opcoes?.podeGerenciar === true;
  const itens = lista?.itens ?? [];

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Portaria" titulo="Encomendas">
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'registrar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Registrar encomenda
          </Botao>
        )}
      </CabecalhoDaPagina>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Abas
          rotulo="Situação"
          abas={abas}
          valor={situacao}
          aoMudar={(valor) => {
            setSituacao(valor);
            setPagina(0);
          }}
        />
        <label className="flex h-10 w-full items-center gap-2 rounded-[10px] border border-borda bg-superficie px-3 text-apagado sm:w-72">
          <Search size={16} aria-hidden />
          <span className="sr-only">Buscar encomendas</span>
          <input
            type="search"
            value={buscaDigitada}
            onChange={(e) => setBuscaDigitada(e.target.value)}
            placeholder="Unidade ou destinatário"
            className="min-w-0 grow border-0 bg-transparent text-sm text-tinta outline-none placeholder:text-apagado"
          />
        </label>
      </div>

      <Cartao aria-label="Lista de encomendas" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Unidade</span>
          <span>Destinatário</span>
          <span>Descrição</span>
          <span>Tipo</span>
          <span>Recebida em</span>
          <span>Por</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && (
          <Vazio>{busca || situacao ? 'Nenhuma encomenda com esses filtros.' : 'Nenhuma encomenda registrada ainda.'}</Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((encomenda) => (
            <li
              key={encomenda.id}
              className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[52px] lg:py-2 ${COLUNAS}`}
            >
              <span className="font-extrabold">{rotuloUnidade(encomenda.unidadeNumero, encomenda.unidadeBloco) || '—'}</span>
              <span className="justify-self-end lg:hidden">
                <SeloDaEncomenda encomenda={encomenda} />
              </span>
              <span className="col-span-2 font-semibold lg:col-span-1">{encomenda.destinatario}</span>
              <span className="col-span-2 text-tinta-2 lg:col-span-1">
                <span className="lg:hidden">{encomenda.tipoDescricao ?? encomenda.tipo}</span>
                {encomenda.descricao && <span className="lg:hidden"> · </span>}
                {encomenda.descricao}
              </span>
              <span className="hidden text-tinta-2 lg:block">{encomenda.tipoDescricao ?? encomenda.tipo}</span>
              <span className="text-apagado">
                {formatarMomento(encomenda.dataRecebimento)}
                <span className="lg:hidden">{encomenda.nomeRecebidoPor ? ` · por ${encomenda.nomeRecebidoPor}` : ''}</span>
              </span>
              <span className="hidden text-apagado lg:block">{encomenda.nomeRecebidoPor}</span>
              <span className="hidden lg:flex">
                <SeloDaEncomenda encomenda={encomenda} />
              </span>
              <span className="flex items-center justify-end gap-1.5">
                {podeGerenciar && estaAguardando(encomenda) && (
                  <Botao pequeno onClick={() => setJanela({ tipo: 'entregar', encomenda })}>
                    Entregar
                  </Botao>
                )}
                <button
                  type="button"
                  aria-label={`Detalhes da encomenda de ${encomenda.destinatario}`}
                  onClick={() => setJanela({ tipo: 'detalhes', encomenda })}
                  className="flex size-[34px] cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-tinta"
                >
                  <EllipsisVertical size={18} aria-hidden />
                </button>
              </span>
            </li>
          ))}
        </ul>

        {lista && (
          <Paginacao
            pagina={lista.pagina}
            totalPaginas={lista.totalPaginas}
            totalItens={lista.totalItens}
            tamanho={TAMANHO}
            aoMudar={setPagina}
            nome="encomendas"
          />
        )}
      </Cartao>

      {janela?.tipo === 'registrar' && opcoes && (
        <RegistrarEncomenda tipos={opcoes.tipos} aoFechar={() => setJanela(null)} aoRegistrar={aposAlterar} />
      )}
      {janela?.tipo === 'entregar' && (
        <EntregarEncomenda encomenda={janela.encomenda} aoFechar={() => setJanela(null)} aoEntregar={aposAlterar} />
      )}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDaEncomenda
          encomenda={janela.encomenda}
          podeGerenciar={podeGerenciar}
          situacoes={opcoes?.statusDeAtualizacao ?? []}
          aoFechar={() => setJanela(null)}
          aoAlterar={aposAlterar}
        />
      )}
    </div>
  );
}
