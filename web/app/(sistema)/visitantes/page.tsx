'use client';

import { useEffect, useState } from 'react';
import { ChevronDown, EllipsisVertical, LogOut, Plus, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import { DetalhesDoVisitante } from '@/components/visitantes/DetalhesDoVisitante';
import { FormularioDoVisitante } from '@/components/visitantes/FormularioDoVisitante';
import { SeloDoVisitante, estaNoCondominio } from '@/components/visitantes/SeloDoVisitante';
import { Abas, Botao, CabecalhoDaPagina, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { confirmar } from '@/services/confirmacao';
import type { Pagina } from '@/services/tipos';
import { formatarMomento, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';
import {
  visitanteService,
  type OpcoesVisitante,
  type SituacaoVisitante,
  type TotaisVisitantes,
  type VisitanteDetalhe,
  type VisitanteResumo,
} from '@/services/visitanteService';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[1.4fr_120px_1.1fr_110px_110px_128px_176px]';

type Janela =
  | { tipo: 'registrar' }
  | { tipo: 'editar'; visitante: VisitanteDetalhe }
  | { tipo: 'detalhes'; visitante: VisitanteResumo }
  | null;

function plural(quantidade: number, singular: string, varios: string): string {
  return `${quantidade} ${quantidade === 1 ? singular : varios}`;
}

export default function PaginaDeVisitantes() {
  const { condominio, permissoes } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesVisitante | null>(null);
  const [situacao, setSituacao] = useState<SituacaoVisitante | ''>('NO_LOCAL');
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  // A unidade escolhida vale só para o condomínio em que foi escolhida
  const [filtroDeUnidade, setFiltroDeUnidade] = useState<{ condominioId?: number; unidadeId: string }>({ unidadeId: '' });
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<VisitanteResumo> | null>(null);
  const [totais, setTotais] = useState<TotaisVisitantes | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  const [saindo, setSaindo] = useState<number | null>(null);
  // Sobe a cada entrada, edição ou saída, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const unidadeId = filtroDeUnidade.condominioId === condominioId && filtroDeUnidade.unidadeId ? Number(filtroDeUnidade.unidadeId) : undefined;
  const consulta = JSON.stringify({ condominioId, busca, unidadeId, situacao, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    let ativa = true;
    visitanteService
      .opcoes(condominioId)
      .then((novas) => {
        if (ativa) setOpcoes(novas);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as unidades do condomínio.'));
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
      visitanteService.listar({ condominioId, busca, unidadeId, status: situacao, pagina, tamanho: TAMANHO }),
      visitanteService.totais({ condominioId, busca, unidadeId }),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        setLista(novaLista);
        setTotais(novosTotais);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os visitantes.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, busca, unidadeId, situacao, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  async function registrarSaida(visitante: VisitanteResumo) {
    // A janela de detalhes fecha antes da pergunta, para as duas não ficarem abertas uma sobre a outra
    setJanela(null);
    const unidade = rotuloUnidade(visitante.unidadeNumero, visitante.unidadeBloco);
    const confirmado = await confirmar({
      titulo: 'Registrar saída',
      mensagem: `Confirma a saída de ${visitante.nome}${unidade ? `, que visitou a unidade ${unidade},` : ''} agora? Depois de registrada, a saída não pode ser desfeita.`,
      textoConfirmar: 'Registrar saída',
    });
    if (!confirmado) return;
    setSaindo(visitante.id);
    try {
      await visitanteService.registrarSaida(visitante.id);
      toast.success(`Saída de ${visitante.nome} registrada.`);
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível registrar a saída.'));
    } finally {
      setSaindo(null);
    }
  }

  const saidas = totais ? Math.max(totais.TOTAL - totais.NO_LOCAL, 0) : null;
  const abas: Aba<SituacaoVisitante | ''>[] = [
    { valor: 'NO_LOCAL', rotulo: 'No condomínio', contagem: totais?.NO_LOCAL, destaque: true },
    { valor: 'SAIU', rotulo: 'Já saíram', contagem: saidas },
    { valor: '', rotulo: 'Todos', contagem: totais?.TOTAL },
  ];

  // A flag da API vale para qualquer condomínio; o papel no condomínio escolhido restringe a este
  const podeGerenciar = permissoes.portaria && (opcoes?.podeGerenciar ?? true);
  const unidades = opcoes?.unidades ?? [];
  const itens = lista?.itens ?? [];

  let mensagemVazia = 'Nenhum visitante registrado ainda.';
  if (busca || unidadeId) mensagemVazia = 'Nenhum visitante com esses filtros.';
  else if (situacao === 'NO_LOCAL') mensagemVazia = 'Nenhum visitante no condomínio agora.';
  else if (situacao === 'SAIU') mensagemVazia = 'Nenhuma saída registrada ainda.';

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Portaria" titulo="Visitantes">
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'registrar' })} disabled={!opcoes}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Registrar entrada
          </Botao>
        )}
      </CabecalhoDaPagina>

      {totais && (
        <p className="-mt-2 text-sm text-apagado">
          Hoje: {plural(totais.DO_DIA, 'entrada', 'entradas')} e {plural(totais.SAIDAS_DIA, 'saída', 'saídas')}
          {unidadeId || busca ? ', com os filtros escolhidos' : ''}.
        </p>
      )}

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
        <div className="flex w-full flex-wrap gap-2.5 sm:w-auto">
          {unidades.length > 1 && (
            <label className="relative flex h-11 w-full items-center rounded-[10px] border border-borda bg-superficie text-apagado sm:h-10 sm:w-44">
              <span className="sr-only">Filtrar por unidade</span>
              <select
                value={unidadeId ? String(unidadeId) : ''}
                onChange={(e) => {
                  setFiltroDeUnidade({ condominioId, unidadeId: e.target.value });
                  setPagina(0);
                }}
                className="h-full w-full cursor-pointer appearance-none rounded-[10px] bg-transparent pr-9 pl-3 text-sm text-tinta outline-none"
              >
                <option value="">Todas as unidades</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {rotuloUnidade(u.numero, u.bloco)}
                  </option>
                ))}
              </select>
              <ChevronDown size={16} className="pointer-events-none absolute right-3" aria-hidden />
            </label>
          )}
          <label className="flex h-11 w-full items-center gap-2 rounded-[10px] border border-borda bg-superficie px-3 text-apagado sm:h-10 sm:w-64">
            <Search size={16} aria-hidden />
            <span className="sr-only">Buscar visitantes pelo nome</span>
            <input
              type="search"
              value={buscaDigitada}
              onChange={(e) => setBuscaDigitada(e.target.value)}
              placeholder="Nome do visitante"
              className="min-w-0 grow border-0 bg-transparent text-sm text-tinta outline-none placeholder:text-apagado"
            />
          </label>
        </div>
      </div>

      <Cartao aria-label="Lista de visitantes" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Visitante</span>
          <span>Unidade</span>
          <span>Quem autorizou</span>
          <span>Entrada</span>
          <span>Saída</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && <Vazio>{mensagemVazia}</Vazio>}

        <ul className="m-0 list-none p-0">
          {itens.map((visitante) => {
            const unidade = rotuloUnidade(visitante.unidadeNumero, visitante.unidadeBloco);
            return (
              <li
                key={visitante.id}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[56px] lg:py-2 ${COLUNAS}`}
              >
                <span className="flex min-w-0 flex-col">
                  <span className="font-bold break-words">{visitante.nome}</span>
                  {visitante.telefone && <span className="text-[13px] text-apagado">{visitante.telefone}</span>}
                </span>
                <span className="justify-self-end lg:hidden">
                  <SeloDoVisitante visitante={visitante} />
                </span>
                <span className="col-span-2 text-tinta-2 lg:col-span-1 lg:font-extrabold lg:text-tinta">
                  {unidade || '—'}
                  <span className="lg:hidden">{visitante.moradorNome ? ` · autorizado por ${visitante.moradorNome}` : ''}</span>
                </span>
                <span className="hidden text-tinta-2 lg:block">{visitante.moradorNome ?? '—'}</span>
                <span className="text-apagado lg:text-tinta-2">
                  <span className="lg:hidden">Entrou </span>
                  {formatarMomento(visitante.dataEntrada)}
                  {visitante.dataSaida && <span className="lg:hidden"> · saiu {formatarMomento(visitante.dataSaida)}</span>}
                </span>
                <span className="hidden text-tinta-2 lg:block">{visitante.dataSaida ? formatarMomento(visitante.dataSaida) : '—'}</span>
                <span className="hidden lg:flex">
                  <SeloDoVisitante visitante={visitante} />
                </span>
                <span className="flex items-center justify-end gap-1.5">
                  {visitante.podeAlterar && estaNoCondominio(visitante) && (
                    <Botao
                      pequeno
                      className="max-lg:h-11"
                      carregando={saindo === visitante.id}
                      disabled={saindo !== null}
                      onClick={() => registrarSaida(visitante)}
                    >
                      <LogOut size={15} aria-hidden />
                      Registrar saída
                    </Botao>
                  )}
                  <button
                    type="button"
                    aria-label={`Detalhes da visita de ${visitante.nome}`}
                    onClick={() => setJanela({ tipo: 'detalhes', visitante })}
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
            nome="visitantes"
          />
        )}
      </Cartao>

      {janela?.tipo === 'registrar' && (
        <FormularioDoVisitante unidades={unidades} aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />
      )}
      {janela?.tipo === 'editar' && (
        <FormularioDoVisitante
          unidades={unidades}
          visitante={janela.visitante}
          aoFechar={() => setJanela(null)}
          aoSalvar={aposAlterar}
        />
      )}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDoVisitante
          visitante={janela.visitante}
          aoFechar={() => setJanela(null)}
          aoEditar={(detalhe) => setJanela({ tipo: 'editar', visitante: detalhe })}
          aoRegistrarSaida={registrarSaida}
        />
      )}
    </div>
  );
}
