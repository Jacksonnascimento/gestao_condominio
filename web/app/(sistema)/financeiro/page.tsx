'use client';

import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { DetalhesDoBoleto } from '@/components/financeiro/DetalhesDoBoleto';
import { GerarBoletoAvulso } from '@/components/financeiro/GerarBoletoAvulso';
import { SeloDoBoleto } from '@/components/financeiro/SeloDoBoleto';
import { Abas, Botao, CabecalhoDaPagina, CampoDeBusca, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import {
  financeiroService,
  nomeDaUnidade,
  type Boleto,
  type OpcoesFinanceiro,
  type PainelFinanceiro,
} from '@/services/financeiroService';
import { formatarData, formatarMoeda, mensagemErroApi } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[1.2fr_1.5fr_120px_130px_110px_112px]';

type Grupo = 'abertos' | 'vencidos' | 'pagos';

type Janela = { tipo: 'gerar' } | { tipo: 'detalhes'; boleto: Boleto } | null;

function boletosDoGrupo(painel: PainelFinanceiro | null, grupo: Grupo): Boleto[] {
  if (!painel) return [];
  if (grupo === 'vencidos') return painel.boletosVencidos;
  if (grupo === 'pagos') return painel.historico;
  return painel.boletosAbertos;
}

const somar = (boletos: Boleto[]) => boletos.reduce((total, b) => total + Number(b.valor || 0), 0);

function Resumo({ rotulo, valor, quantidade, destaque }: { rotulo: string; valor: number; quantidade: number; destaque?: boolean }) {
  return (
    <div
      className={`flex flex-col gap-1.5 rounded-2xl px-[22px] py-[18px] ${
        destaque ? 'bg-tinta text-fundo' : 'border border-borda bg-superficie'
      }`}
    >
      <span className={`text-[13px] font-semibold ${destaque ? 'text-[#cfc5b6]' : 'text-apagado'}`}>{rotulo}</span>
      <span className={`font-titulo text-[30px] leading-none tabular-nums ${destaque && valor > 0 ? 'text-ouro-claro' : ''}`}>
        {formatarMoeda(valor)}
      </span>
      <span className={`text-[13px] ${destaque ? 'text-[#cfc5b6]' : 'text-apagado'}`}>
        {quantidade === 1 ? '1 boleto' : `${quantidade} boletos`}
      </span>
    </div>
  );
}

export default function PaginaDoFinanceiro() {
  const { condominio } = useSessao();
  const [painel, setPainel] = useState<PainelFinanceiro | null>(null);
  const [opcoes, setOpcoes] = useState<OpcoesFinanceiro | null>(null);
  const [grupo, setGrupo] = useState<Grupo>('abertos');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada boleto gerado, para as cobranças serem lidas de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const consulta = JSON.stringify({ condominioId, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    let ativa = true;
    Promise.all([financeiroService.painel(condominioId), financeiroService.opcoes(condominioId)])
      .then(([novoPainel, novasOpcoes]) => {
        if (!ativa) return;
        setPainel(novoPainel);
        setOpcoes(novasOpcoes);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar o financeiro.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId]);

  const aposGerar = () => {
    setJanela(null);
    setGrupo('abertos');
    setPagina(0);
    setVersao((v) => v + 1);
  };

  const podeGerarBoleto = painel?.podeGerarBoleto === true && opcoes?.podeGerarBoleto === true;

  const abertos = boletosDoGrupo(painel, 'abertos');
  const vencidos = boletosDoGrupo(painel, 'vencidos');
  const pagos = boletosDoGrupo(painel, 'pagos');

  const termo = busca.trim().toLocaleLowerCase('pt-BR');
  const filtrar = (lista: Boleto[]) =>
    termo
      ? lista.filter((b) => `${nomeDaUnidade(b.unidadeNome)} ${b.nomeTaxa}`.toLocaleLowerCase('pt-BR').includes(termo))
      : lista;

  const filtrados = filtrar(boletosDoGrupo(painel, grupo));
  const totalPaginas = Math.ceil(filtrados.length / TAMANHO);
  const paginaAtual = Math.min(pagina, Math.max(totalPaginas - 1, 0));
  const itens = filtrados.slice(paginaAtual * TAMANHO, (paginaAtual + 1) * TAMANHO);

  const abas: Aba<Grupo>[] = [
    { valor: 'abertos', rotulo: 'Em aberto', contagem: painel ? filtrar(abertos).length : null },
    { valor: 'vencidos', rotulo: 'Vencidos', contagem: painel ? filtrar(vencidos).length : null, destaque: true },
    { valor: 'pagos', rotulo: 'Pagos', contagem: painel ? filtrar(pagos).length : null },
  ];

  const vazio = {
    abertos: 'Nenhum boleto em aberto.',
    vencidos: 'Nenhum boleto vencido.',
    pagos: 'Nenhum pagamento registrado.',
  }[grupo];

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Administração" titulo="Financeiro">
        {podeGerarBoleto && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'gerar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Gerar boleto avulso
          </Botao>
        )}
      </CabecalhoDaPagina>

      {painel?.demonstracao && (
        <p className="rounded-xl bg-aviso-fundo px-4 py-3 text-sm text-aviso">
          O financeiro ainda é uma demonstração: os valores e os códigos dos boletos são fictícios e nada é cobrado de verdade.
        </p>
      )}

      {painel && (
        <section aria-label="Resumo" className="grid gap-3 sm:grid-cols-3 sm:gap-4">
          <Resumo rotulo="Em aberto" valor={somar(abertos)} quantidade={abertos.length} />
          <Resumo rotulo="Vencidos" valor={somar(vencidos)} quantidade={vencidos.length} destaque />
          <Resumo rotulo="Pagos no último mês" valor={somar(pagos)} quantidade={pagos.length} />
        </section>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Abas
          rotulo="Situação"
          abas={abas}
          valor={grupo}
          aoMudar={(valor) => {
            setGrupo(valor);
            setPagina(0);
          }}
        />
        <CampoDeBusca
          valor={busca}
          aoMudar={(valor) => {
            setBusca(valor);
            setPagina(0);
          }}
          rotulo="Buscar boletos"
          dica="Unidade ou descrição"
        />
      </div>

      <Cartao aria-label="Boletos" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Unidade</span>
          <span>Descrição</span>
          <span>Vencimento</span>
          <span className="text-right">Valor</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && <Vazio>{termo ? 'Nenhum boleto com essa busca.' : vazio}</Vazio>}

        <ul className="m-0 list-none p-0">
          {itens.map((boleto) => (
            <li
              key={boleto.id}
              className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[52px] lg:py-2 ${COLUNAS}`}
            >
              <span className="font-extrabold">{nomeDaUnidade(boleto.unidadeNome)}</span>
              <span className="justify-self-end lg:hidden">
                <SeloDoBoleto boleto={boleto} />
              </span>
              <span className="col-span-2 text-tinta-2 lg:col-span-1">{boleto.nomeTaxa}</span>
              <span className="text-apagado">
                <span className="lg:hidden">Vence em </span>
                {formatarData(boleto.dataVencimento)}
              </span>
              <span className="text-right font-bold">{formatarMoeda(boleto.valor)}</span>
              <span className="hidden lg:flex">
                <SeloDoBoleto boleto={boleto} />
              </span>
              <span className="col-span-2 mt-1.5 flex justify-end lg:col-span-1 lg:mt-0">
                <Botao pequeno onClick={() => setJanela({ tipo: 'detalhes', boleto })} className="max-lg:h-11 max-lg:w-full">
                  Ver boleto
                </Botao>
              </span>
            </li>
          ))}
        </ul>

        <Paginacao
          pagina={paginaAtual}
          totalPaginas={totalPaginas}
          totalItens={filtrados.length}
          tamanho={TAMANHO}
          aoMudar={setPagina}
          nome="boletos"
        />
      </Cartao>

      {janela?.tipo === 'gerar' && opcoes && (
        <GerarBoletoAvulso
          unidades={opcoes.unidades}
          condominioNome={painel?.condominio.nome ?? condominio?.nome}
          aoFechar={() => setJanela(null)}
          aoGerar={aposGerar}
        />
      )}
      {janela?.tipo === 'detalhes' && <DetalhesDoBoleto boleto={janela.boleto} aoFechar={() => setJanela(null)} />}
    </div>
  );
}
