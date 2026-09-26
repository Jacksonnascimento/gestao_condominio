'use client';

import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { DetalhesDaOcorrencia } from '@/components/ocorrencias/DetalhesDaOcorrencia';
import { RegistrarOcorrencia } from '@/components/ocorrencias/RegistrarOcorrencia';
import { SeloDaOcorrencia } from '@/components/ocorrencias/SeloDaOcorrencia';
import { Abas, Botao, CabecalhoDaPagina, CampoDeBusca, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import {
  ocorrenciaService,
  type OcorrenciaResumida,
  type OpcoesOcorrencia,
  type SituacaoOcorrencia,
  type TipoOcorrencia,
  type TotaisOcorrencias,
} from '@/services/ocorrenciaService';
import type { Pagina } from '@/services/tipos';
import { formatarMomento, mensagemErroApi, rotuloUnidade, textoLegivelDeCodigo } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[minmax(0,2.2fr)_120px_120px_120px_110px_84px]';
const CLASSE_DO_FILTRO =
  'h-10 rounded-[10px] border border-borda bg-superficie px-3 text-sm text-tinta outline-none focus:border-ouro';

type Janela = { tipo: 'registrar' } | { tipo: 'detalhes'; ocorrencia: OcorrenciaResumida } | null;

export default function PaginaDeOcorrencias() {
  const { condominio, permissoes } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesOcorrencia | null>(null);
  const [situacao, setSituacao] = useState<SituacaoOcorrencia | ''>('');
  const [tipo, setTipo] = useState<TipoOcorrencia | ''>('');
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [tituloDigitado, setTituloDigitado] = useState('');
  const [unidadeDigitada, setUnidadeDigitada] = useState('');
  const [buscaTitulo, setBuscaTitulo] = useState('');
  const [buscaUnidade, setBuscaUnidade] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<OcorrenciaResumida> | null>(null);
  const [totais, setTotais] = useState<TotaisOcorrencias | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada registro, comentário, anexo ou resolução, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const consulta = JSON.stringify({ condominioId, buscaTitulo, buscaUnidade, situacao, tipo, inicio, fim, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  // As unidades do formulário dependem do condomínio escolhido (para o administrador geral)
  useEffect(() => {
    let ativa = true;
    ocorrenciaService
      .opcoes(condominioId)
      .then((novas) => {
        if (ativa) setOpcoes(novas);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os tipos de ocorrência.'));
      });
    return () => {
      ativa = false;
    };
  }, [condominioId]);

  // As buscas vão para a API só quando a pessoa para de digitar
  useEffect(() => {
    const espera = setTimeout(() => {
      setBuscaTitulo(tituloDigitado.trim());
      setBuscaUnidade(unidadeDigitada.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(espera);
  }, [tituloDigitado, unidadeDigitada]);

  useEffect(() => {
    let ativa = true;
    const filtro = {
      condominioId,
      buscaTitulo,
      buscaUnidade,
      tipo,
      inicioApos: inicio,
      fimAntes: fim,
    };
    Promise.all([
      ocorrenciaService.listar({ ...filtro, status: situacao, pagina, tamanho: TAMANHO }),
      ocorrenciaService.totais(filtro),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        setLista(novaLista);
        setTotais(novosTotais);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as ocorrências.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, buscaTitulo, buscaUnidade, situacao, tipo, inicio, fim, pagina]);

  const recarregar = () => setVersao((v) => v + 1);
  const aposRegistrar = () => {
    setJanela(null);
    recarregar();
  };

  const abas: Aba<SituacaoOcorrencia | ''>[] = [
    { valor: '', rotulo: 'Todas', contagem: totais?.TOTAL },
    { valor: 'ABERTA', rotulo: 'Abertas', contagem: totais?.ABERTA, destaque: true },
    { valor: 'EM_ANALISE', rotulo: 'Em análise', contagem: totais?.EM_ANALISE },
    { valor: 'RESOLVIDA', rotulo: 'Resolvidas', contagem: totais?.RESOLVIDA },
  ];

  // A API diz quem trata as ocorrências; até as opções chegarem, vale o papel no condomínio
  const gerencia = opcoes ? opcoes.podeGerenciar : permissoes.gestao;
  const descricaoDoTipo = (valor: string) => opcoes?.tipos.find((t) => t.valor === valor)?.descricao ?? textoLegivelDeCodigo(valor);
  const itens = lista?.itens ?? [];
  const filtrando = Boolean(buscaTitulo || buscaUnidade || situacao || tipo || inicio || fim);
  const mudarFiltro = <T,>(definir: (valor: T) => void) => (valor: T) => {
    definir(valor);
    setPagina(0);
  };

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Convivência" titulo="Ocorrências">
        <Botao variante="primario" onClick={() => setJanela({ tipo: 'registrar' })} disabled={!opcoes}>
          <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
          Registrar ocorrência
        </Botao>
      </CabecalhoDaPagina>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Abas rotulo="Situação" abas={abas} valor={situacao} aoMudar={mudarFiltro(setSituacao)} />
          <CampoDeBusca
            valor={tituloDigitado}
            aoMudar={setTituloDigitado}
            rotulo="Buscar ocorrências pelo título"
            dica="Buscar pelo título"
          />
        </div>

        <div className="grid grid-cols-2 gap-2.5 text-sm sm:flex sm:flex-wrap sm:items-end">
          {gerencia && (
            <label className="col-span-2 flex flex-col gap-1 sm:w-40">
              <span className="text-xs font-semibold text-apagado">Unidade</span>
              <input
                type="search"
                value={unidadeDigitada}
                onChange={(e) => setUnidadeDigitada(e.target.value)}
                placeholder="Número ou bloco"
                className={`${CLASSE_DO_FILTRO} placeholder:text-apagado`}
              />
            </label>
          )}
          <label className="col-span-2 flex flex-col gap-1 sm:w-44">
            <span className="text-xs font-semibold text-apagado">Tipo</span>
            <select
              value={tipo}
              onChange={(e) => mudarFiltro(setTipo)(e.target.value as TipoOcorrencia | '')}
              className={`${CLASSE_DO_FILTRO} cursor-pointer`}
            >
              <option value="">Todos os tipos</option>
              {opcoes?.tipos.map((t) => (
                <option key={t.valor} value={t.valor}>
                  {t.descricao}
                </option>
              ))}
            </select>
          </label>
          <label className="flex min-w-0 flex-col gap-1 sm:w-40">
            <span className="text-xs font-semibold text-apagado">Registradas de</span>
            <input
              type="date"
              value={inicio}
              max={fim || undefined}
              onChange={(e) => mudarFiltro(setInicio)(e.target.value)}
              className={CLASSE_DO_FILTRO}
            />
          </label>
          <label className="flex min-w-0 flex-col gap-1 sm:w-40">
            <span className="text-xs font-semibold text-apagado">até</span>
            <input
              type="date"
              value={fim}
              min={inicio || undefined}
              onChange={(e) => mudarFiltro(setFim)(e.target.value)}
              className={CLASSE_DO_FILTRO}
            />
          </label>
          {(tipo || inicio || fim || unidadeDigitada) && (
            <Botao
              variante="texto"
              className="col-span-2 h-10 max-sm:h-11"
              onClick={() => {
                setTipo('');
                setInicio('');
                setFim('');
                setUnidadeDigitada('');
                setPagina(0);
              }}
            >
              Limpar filtros
            </Botao>
          )}
        </div>
      </div>

      <Cartao aria-label="Lista de ocorrências" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Ocorrência</span>
          <span>Unidade</span>
          <span>Tipo</span>
          <span>Registrada</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && (
          <Vazio>{filtrando ? 'Nenhuma ocorrência com esses filtros.' : 'Nenhuma ocorrência registrada ainda.'}</Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((ocorrencia) => {
            const unidade = rotuloUnidade(ocorrencia.unidadeNumero, ocorrencia.unidadeBloco);
            return (
              <li
                key={ocorrencia.id}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[60px] lg:py-2.5 ${COLUNAS}`}
              >
                <span className="text-[13px] font-extrabold lg:hidden">
                  {[unidade, descricaoDoTipo(ocorrencia.tipo)].filter(Boolean).join(' · ')}
                </span>
                <span className="justify-self-end lg:hidden">
                  <SeloDaOcorrencia situacao={ocorrencia.status} />
                </span>
                <span className="col-span-2 flex min-w-0 flex-col gap-0.5 lg:col-span-1">
                  <span className="font-bold break-words">{ocorrencia.titulo}</span>
                  {ocorrencia.descricaoCurta && (
                    <span className="line-clamp-2 text-[13px] text-apagado lg:line-clamp-1">{ocorrencia.descricaoCurta}</span>
                  )}
                </span>
                <span className="hidden font-extrabold lg:block">{unidade || '—'}</span>
                <span className="hidden text-tinta-2 lg:block">{descricaoDoTipo(ocorrencia.tipo)}</span>
                <span className="text-apagado">{formatarMomento(ocorrencia.dataRegistro)}</span>
                <span className="hidden lg:flex">
                  <SeloDaOcorrencia situacao={ocorrencia.status} />
                </span>
                <span className="flex justify-end">
                  <Botao
                    pequeno
                    aria-label={`Abrir a ocorrência ${ocorrencia.titulo}`}
                    onClick={() => setJanela({ tipo: 'detalhes', ocorrencia })}
                    className="max-lg:h-11"
                  >
                    Abrir
                  </Botao>
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
            nome="ocorrências"
          />
        )}
      </Cartao>

      {janela?.tipo === 'registrar' && opcoes && (
        <RegistrarOcorrencia opcoes={opcoes} aoFechar={() => setJanela(null)} aoRegistrar={aposRegistrar} />
      )}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDaOcorrencia
          resumo={janela.ocorrencia}
          tipos={opcoes?.tipos ?? []}
          aoFechar={() => setJanela(null)}
          aoAlterar={recarregar}
        />
      )}
    </div>
  );
}
