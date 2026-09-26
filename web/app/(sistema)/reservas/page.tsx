'use client';

import { useEffect, useState } from 'react';
import { EllipsisVertical, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { AgendaDeReservas } from '@/components/reservas/AgendaDeReservas';
import { aprovarReserva, cancelarReserva } from '@/components/reservas/acoesDaReserva';
import { DetalhesDaReserva } from '@/components/reservas/DetalhesDaReserva';
import { PedirReserva } from '@/components/reservas/PedirReserva';
import { RejeitarReserva } from '@/components/reservas/RejeitarReserva';
import { SeloDaReserva, turnoDaReserva } from '@/components/reservas/SeloDaReserva';
import {
  Abas,
  Botao,
  CabecalhoDaPagina,
  CampoDeBusca,
  Cartao,
  FiltroDeData,
  FiltroDeSelecao,
  Paginacao,
  Vazio,
  type Aba,
} from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import {
  reservaService,
  type OpcoesReserva,
  type Reserva,
  type SituacaoReserva,
  type TotaisReservas,
} from '@/services/reservaService';
import type { Pagina } from '@/services/tipos';
import { diaDaSemanaCurto, formatarData, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[104px_minmax(0,1.5fr)_minmax(0,0.8fr)_minmax(0,1fr)_170px_218px]';

type Visao = 'lista' | 'agenda';

type Janela =
  | { tipo: 'pedir' }
  | { tipo: 'detalhes'; reserva: Reserva }
  | { tipo: 'rejeitar'; reserva: Reserva }
  | null;

export default function PaginaDeReservas() {
  const { condominio } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesReserva | null>(null);
  const [visao, setVisao] = useState<Visao>('lista');
  const [situacao, setSituacao] = useState<SituacaoReserva | ''>('');
  const [areaId, setAreaId] = useState('');
  const [dataInicio, setDataInicio] = useState('');
  const [dataFim, setDataFim] = useState('');
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Reserva> | null>(null);
  const [totais, setTotais] = useState<TotaisReservas | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  const [emAndamento, setEmAndamento] = useState<number | null>(null);
  // Sobe a cada pedido, aprovação, rejeição ou cancelamento, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const areasDoFiltro = (opcoes?.areasParaFiltro ?? []).filter((a) => condominioId == null || a.condominioCodigo === condominioId);
  // A área escolhida pode ser de outro condomínio, se a pessoa trocou de condomínio: aí o filtro deixa de valer
  const area = areasDoFiltro.some((a) => String(a.codigo) === areaId) ? Number(areaId) : '';
  const periodoInvertido = Boolean(dataInicio && dataFim && dataFim < dataInicio);
  const inicio = periodoInvertido ? '' : dataInicio;
  const fim = periodoInvertido ? '' : dataFim;

  const consulta = JSON.stringify({ condominioId, visao, situacao, area, inicio, fim, busca, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    let ativa = true;
    reservaService
      .opcoes(condominioId)
      .then((novas) => {
        if (ativa) setOpcoes(novas);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as áreas para reserva.'));
      });
    return () => {
      ativa = false;
    };
  }, [condominioId]);

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
      visao === 'lista'
        ? reservaService.listar({ condominioId, status: situacao, areaId: area, busca, dataInicio: inicio, dataFim: fim, pagina, tamanho: TAMANHO })
        : Promise.resolve(null),
      reservaService.totais(condominioId),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        if (novaLista) setLista(novaLista);
        setTotais(novosTotais);
        // Aprovar ou cancelar a última reserva de uma página (na aba Pendentes, por exemplo) a deixa vazia
        if (novaLista && novaLista.itens.length === 0 && pagina > 0) setPagina(Math.max(novaLista.totalPaginas - 1, 0));
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as reservas.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, visao, condominioId, situacao, area, busca, inicio, fim, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  async function agir(reserva: Reserva, acao: (r: Reserva) => Promise<boolean>) {
    setEmAndamento(reserva.codigo);
    const feito = await acao(reserva);
    setEmAndamento(null);
    if (feito) setVersao((v) => v + 1);
  }

  const mudarFiltro = <T,>(definir: (valor: T) => void) => (valor: T) => {
    definir(valor);
    setPagina(0);
  };

  const abas: Aba<SituacaoReserva | ''>[] = [
    { valor: '', rotulo: 'Todas', contagem: totais?.TOTAL },
    { valor: 'PENDENTE_APROVACAO', rotulo: 'Pendentes', contagem: totais?.PENDENTES, destaque: true },
    { valor: 'APROVADA', rotulo: 'Aprovadas', contagem: totais?.APROVADAS },
    { valor: 'REJEITADA', rotulo: 'Rejeitadas' },
    { valor: 'CANCELADA_PELO_MORADOR', rotulo: 'Canceladas' },
    { valor: 'CONCLUIDA', rotulo: 'Concluídas' },
  ];

  const podeSolicitar = opcoes?.podeSolicitar === true && opcoes.unidades.length > 0;
  const itens = lista?.itens ?? [];
  const temFiltro = Boolean(situacao || area || inicio || fim || busca);
  const temFiltroExtra = Boolean(areaId || dataInicio || dataFim);

  let vazio = 'Nenhuma reserva ainda.';
  if (temFiltro) vazio = 'Nenhuma reserva com esses filtros.';
  else if (podeSolicitar) vazio = 'Nenhuma reserva ainda. Use “Pedir reserva” para reservar o salão, a churrasqueira ou outra área comum.';

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Convivência" titulo="Reservas">
        {podeSolicitar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'pedir' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Pedir reserva
          </Botao>
        )}
      </CabecalhoDaPagina>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Abas rotulo="Situação" abas={abas} valor={situacao} aoMudar={mudarFiltro(setSituacao)} />
          <Abas
            rotulo="Forma de ver"
            abas={[
              { valor: 'lista', rotulo: 'Lista' },
              { valor: 'agenda', rotulo: 'Agenda do mês' },
            ]}
            valor={visao}
            aoMudar={setVisao}
          />
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <FiltroDeSelecao
            rotulo="Área"
            value={area === '' ? '' : String(area)}
            onChange={(e) => mudarFiltro(setAreaId)(e.target.value)}
            className="w-full sm:w-auto"
          >
            <option value="">Todas</option>
            {areasDoFiltro.map((a) => (
              <option key={a.codigo} value={a.codigo}>
                {a.nome}
              </option>
            ))}
          </FiltroDeSelecao>
          {visao === 'lista' && (
            <>
              <FiltroDeData
                rotulo="De"
                value={dataInicio}
                onChange={(e) => mudarFiltro(setDataInicio)(e.target.value)}
                className="grow sm:grow-0"
              />
              <FiltroDeData
                rotulo="Até"
                value={dataFim}
                min={dataInicio || undefined}
                onChange={(e) => mudarFiltro(setDataFim)(e.target.value)}
                className="grow sm:grow-0"
              />
            </>
          )}
          {temFiltroExtra && (
            <Botao
              pequeno
              variante="texto"
              className="max-lg:h-11"
              onClick={() => {
                setAreaId('');
                setDataInicio('');
                setDataFim('');
                setPagina(0);
              }}
            >
              Limpar filtros
            </Botao>
          )}
          <CampoDeBusca
            valor={buscaDigitada}
            aoMudar={setBuscaDigitada}
            rotulo="Buscar reservas"
            dica="Área, morador ou unidade"
            className="w-full sm:ml-auto sm:w-72"
          />
        </div>
        {periodoInvertido && (
          <p className="text-[13px] text-perigo" role="alert">
            A data final é anterior à inicial; o período não está sendo aplicado.
          </p>
        )}
      </div>

      {visao === 'agenda' ? (
        <AgendaDeReservas
          filtro={{ condominioId, status: situacao, areaId: area, busca }}
          versao={versao}
          aoAbrir={(reserva) => setJanela({ tipo: 'detalhes', reserva })}
        />
      ) : (
        <Cartao aria-label="Lista de reservas" aria-busy={carregando}>
          <div
            className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
          >
            <span>Data</span>
            <span>Área e horário</span>
            <span>Unidade</span>
            <span>Pedida por</span>
            <span>Situação</span>
            <span className="text-right">Ações</span>
          </div>

          {!carregando && itens.length === 0 && <Vazio>{vazio}</Vazio>}

          <ul className="m-0 list-none p-0">
            {itens.map((reserva) => {
              const ocupada = emAndamento === reserva.codigo;
              return (
                <li
                  key={reserva.codigo}
                  className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[56px] lg:py-2 ${COLUNAS}`}
                >
                  <span className="flex items-baseline gap-2 lg:flex-col lg:gap-0">
                    <span className="font-extrabold">{formatarData(reserva.data)}</span>
                    <span className="text-xs font-bold text-apagado">{diaDaSemanaCurto(reserva.data)}</span>
                  </span>
                  <span className="justify-self-end lg:hidden">
                    <SeloDaReserva reserva={reserva} />
                  </span>
                  <span className="col-span-2 flex min-w-0 flex-col lg:col-span-1">
                    <span className="font-semibold">{reserva.areaNome}</span>
                    <span className="text-tinta-2">{turnoDaReserva(reserva)}</span>
                  </span>
                  <span className="col-span-2 text-tinta-2 lg:col-span-1">
                    {rotuloUnidade(reserva.unidadeNumero, reserva.unidadeBloco) || '—'}
                    <span className="lg:hidden">{reserva.solicitanteNome ? ` · ${reserva.solicitanteNome}` : ''}</span>
                  </span>
                  <span className="hidden min-w-0 truncate text-apagado lg:block">{reserva.solicitanteNome}</span>
                  <span className="hidden lg:flex">
                    <SeloDaReserva reserva={reserva} />
                  </span>
                  <span className="col-span-2 flex flex-wrap items-center justify-end gap-1.5 lg:col-span-1">
                    {reserva.podeAprovarOuRejeitar && (
                      <>
                        <Botao pequeno variante="primario" onClick={() => agir(reserva, aprovarReserva)} disabled={ocupada} className="max-lg:h-11">
                          Aprovar
                        </Botao>
                        <Botao pequeno onClick={() => setJanela({ tipo: 'rejeitar', reserva })} disabled={ocupada} className="max-lg:h-11">
                          Rejeitar
                        </Botao>
                      </>
                    )}
                    {reserva.podeCancelar && (
                      <Botao pequeno variante="texto" onClick={() => agir(reserva, cancelarReserva)} disabled={ocupada} className="max-lg:h-11">
                        Cancelar
                      </Botao>
                    )}
                    <button
                      type="button"
                      aria-label={`Detalhes da reserva de ${reserva.areaNome} em ${formatarData(reserva.data)}`}
                      onClick={() => setJanela({ tipo: 'detalhes', reserva })}
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
              nome="reservas"
            />
          )}
        </Cartao>
      )}

      {janela?.tipo === 'pedir' && opcoes && <PedirReserva opcoes={opcoes} aoFechar={() => setJanela(null)} aoPedir={aposAlterar} />}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDaReserva
          reserva={janela.reserva}
          aoFechar={() => setJanela(null)}
          aoAlterar={aposAlterar}
          aoPedirRejeicao={() => setJanela({ tipo: 'rejeitar', reserva: janela.reserva })}
        />
      )}
      {janela?.tipo === 'rejeitar' && (
        <RejeitarReserva reserva={janela.reserva} aoFechar={() => setJanela(null)} aoRejeitar={aposAlterar} />
      )}
    </div>
  );
}
