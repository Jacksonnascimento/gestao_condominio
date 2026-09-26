'use client';

import { useEffect, useState } from 'react';
import { EllipsisVertical, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { CadastrarOcupante } from '@/components/ocupantes/CadastrarOcupante';
import { DetalhesDoOcupante } from '@/components/ocupantes/DetalhesDoOcupante';
import { EditarOcupante } from '@/components/ocupantes/EditarOcupante';
import { SeloDoVinculo } from '@/components/ocupantes/SeloDoVinculo';
import { Abas, Botao, CabecalhoDaPagina, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { CampoDeBusca } from '@/components/unidades/PecasDeCadastro';
import { useSessao } from '@/context/SessaoContext';
import { confirmar } from '@/services/confirmacao';
import {
  ocupanteService,
  type OpcoesOcupante,
  type Ocupante,
  type TotaisOcupantes,
  type VinculoDoOcupante,
} from '@/services/ocupanteService';
import type { Pagina } from '@/services/tipos';
import { unidadeService, type Unidade } from '@/services/unidadeService';
import { formatarData, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[1.4fr_0.9fr_1fr_1.5fr_0.8fr_128px]';

/** Nome das abas, no plural, na ordem em que aparecem. */
const ABAS_DE_VINCULO: [VinculoDoOcupante, string][] = [
  ['PROPRIETARIO', 'Proprietários'],
  ['LOCATARIO', 'Locatários'],
  ['DEPENDENTE', 'Dependentes'],
  ['CONJUGE', 'Cônjuges'],
  ['MULTIPROPRIETARIO', 'Multiproprietários'],
  ['PROMITENTE_COMPRADOR', 'Promitentes compradores'],
  ['CESSIONARIO', 'Cessionários'],
];

type Janela =
  | { tipo: 'cadastrar' }
  | { tipo: 'editar'; ocupante: Ocupante }
  | { tipo: 'detalhes'; ocupante: Ocupante }
  | null;

export default function PaginaDeOcupantes() {
  const { condominio, permissoes } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesOcupante | null>(null);
  const [vinculo, setVinculo] = useState<VinculoDoOcupante | ''>('');
  // A unidade escolhida vale só para o condomínio em que foi escolhida
  const [filtroDeUnidade, setFiltroDeUnidade] = useState<{ condominioId?: number; unidadeId: string }>({ unidadeId: '' });
  const [unidades, setUnidades] = useState<Unidade[]>([]);
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Ocupante> | null>(null);
  const [totais, setTotais] = useState<TotaisOcupantes | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada cadastro, edição ou remoção, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const unidadeId = filtroDeUnidade.condominioId === condominioId && filtroDeUnidade.unidadeId ? Number(filtroDeUnidade.unidadeId) : null;
  const consulta = JSON.stringify({ condominioId, busca, vinculo, unidadeId, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    ocupanteService
      .opcoes()
      .then(setOpcoes)
      .catch((e) => toast.error(mensagemErroApi(e, 'Não foi possível carregar os tipos de vínculo.')));
  }, []);

  // Unidades para o filtro: as que quem está logado pode ver no condomínio escolhido
  useEffect(() => {
    let ativa = true;
    unidadeService
      .listar({ condominioId, tamanho: 100 })
      .then((resposta) => {
        if (ativa) setUnidades(resposta.itens);
      })
      .catch(() => {
        // Sem a lista, o filtro por unidade fica vazio; a lista de ocupantes continua funcionando
        if (ativa) setUnidades([]);
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
      ocupanteService.listar({ condominioId, busca, vinculo, unidadeId, pagina, tamanho: TAMANHO }),
      ocupanteService.totais({ condominioId, busca, unidadeId }),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        setLista(novaLista);
        setTotais(novosTotais);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os ocupantes.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, busca, vinculo, unidadeId, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  async function remover(ocupante: Ocupante) {
    setJanela(null);
    const unidade = rotuloUnidade(ocupante.unidadeNumero, ocupante.unidadeBloco);
    const confirmado = await confirmar({
      titulo: 'Remover ocupante',
      mensagem: `${ocupante.nome} deixa de ser ocupante da unidade ${unidade}. O cadastro da pessoa continua no sistema e pode ser vinculado de novo depois.`,
      textoConfirmar: 'Remover',
      perigo: true,
    });
    if (!confirmado) {
      setJanela({ tipo: 'detalhes', ocupante });
      return;
    }
    try {
      await ocupanteService.remover(ocupante.id);
      toast.success('Ocupante removido da unidade.');
      // Se era o último da página, volta uma página
      if (lista && lista.itens.length === 1 && pagina > 0) setPagina(pagina - 1);
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível remover o ocupante.'));
    }
  }

  const abas: Aba<VinculoDoOcupante | ''>[] = [
    { valor: '', rotulo: 'Todos', contagem: totais?.TOTAL },
    // Só os vínculos que têm alguém (e o escolhido), para as abas não virarem uma fileira de zeros
    ...ABAS_DE_VINCULO.filter(([valor]) => (totais?.[valor] ?? 0) > 0 || valor === vinculo).map(([valor, rotulo]) => ({
      valor,
      rotulo,
      contagem: totais?.[valor],
    })),
  ];

  // A API diz se a pessoa gerencia ocupantes em algum condomínio; a sessão, se é da gestão neste
  const podeGerenciar = opcoes?.podeGerenciar === true && permissoes.gestao;
  const itens = lista?.itens ?? [];
  const unidadesOrdenadas = [...unidades].sort((a, b) =>
    rotuloUnidade(a.numero, a.bloco).localeCompare(rotuloUnidade(b.numero, b.bloco), 'pt-BR', { numeric: true }),
  );
  const filtrando = !!(busca || vinculo || unidadeId);

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Cadastros" titulo="Ocupantes">
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'cadastrar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Cadastrar ocupante
          </Botao>
        )}
      </CabecalhoDaPagina>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Abas
          rotulo="Vínculo"
          abas={abas}
          valor={vinculo}
          aoMudar={(valor) => {
            setVinculo(valor);
            setPagina(0);
          }}
        />
        <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
          <label className="flex h-11 w-full items-center rounded-[10px] border border-borda bg-superficie px-3 text-sm sm:h-10 sm:w-48">
            <span className="sr-only">Filtrar por unidade</span>
            <select
              value={unidadeId ? String(unidadeId) : ''}
              onChange={(e) => {
                setFiltroDeUnidade({ condominioId, unidadeId: e.target.value });
                setPagina(0);
              }}
              className="h-full w-full cursor-pointer border-0 bg-transparent text-tinta outline-none"
            >
              <option value="">Todas as unidades</option>
              {unidadesOrdenadas.map((u) => (
                <option key={u.id} value={u.id}>
                  {rotuloUnidade(u.numero, u.bloco)}
                </option>
              ))}
            </select>
          </label>
          <CampoDeBusca valor={buscaDigitada} aoMudar={setBuscaDigitada} rotulo="Buscar ocupantes" dica="Nome ou e-mail" />
        </div>
      </div>

      <Cartao aria-label="Lista de ocupantes" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Nome</span>
          <span>Unidade</span>
          <span>Vínculo</span>
          <span>Contato</span>
          <span>Desde</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && (
          <Vazio>
            {filtrando
              ? 'Nenhum ocupante com esses filtros.'
              : podeGerenciar
                ? 'Nenhum ocupante cadastrado ainda.'
                : 'Nenhum ocupante para mostrar.'}
          </Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((ocupante) => {
            const unidade = rotuloUnidade(ocupante.unidadeNumero, ocupante.unidadeBloco);
            return (
              <li
                key={ocupante.id}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[52px] lg:py-2 ${COLUNAS}`}
              >
                <span className="min-w-0 truncate font-extrabold">{ocupante.nome}</span>
                <span className="justify-self-end lg:hidden">
                  <SeloDoVinculo ocupante={ocupante} />
                </span>
                <span className="flex min-w-0 flex-col text-tinta-2 lg:hidden">
                  <span>
                    {unidade && `Unidade ${unidade}`}
                    {ocupante.inicioOcupacao && ` · desde ${formatarData(ocupante.inicioOcupacao)}`}
                  </span>
                  {ocupante.email && <span className="truncate text-apagado">{ocupante.email}</span>}
                </span>
                <span className="hidden font-semibold lg:block">{unidade || '—'}</span>
                <span className="hidden lg:flex">
                  <SeloDoVinculo ocupante={ocupante} />
                </span>
                <span className="hidden min-w-0 flex-col text-tinta-2 lg:flex">
                  {ocupante.email && <span className="truncate">{ocupante.email}</span>}
                  {ocupante.telefone && <span className="text-apagado">{ocupante.telefone}</span>}
                  {!ocupante.email && !ocupante.telefone && <span className="text-apagado">—</span>}
                </span>
                <span className="hidden text-apagado lg:block">{formatarData(ocupante.inicioOcupacao) || '—'}</span>
                <span className="flex items-center justify-end gap-1.5">
                  {podeGerenciar && (
                    <Botao pequeno onClick={() => setJanela({ tipo: 'editar', ocupante })}>
                      Editar
                    </Botao>
                  )}
                  <button
                    type="button"
                    aria-label={`Detalhes de ${ocupante.nome}`}
                    onClick={() => setJanela({ tipo: 'detalhes', ocupante })}
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
            nome="ocupantes"
          />
        )}
      </Cartao>

      {janela?.tipo === 'cadastrar' && opcoes && (
        <CadastrarOcupante opcoes={opcoes} aoFechar={() => setJanela(null)} aoCadastrar={aposAlterar} />
      )}
      {janela?.tipo === 'editar' && opcoes && (
        <EditarOcupante ocupante={janela.ocupante} opcoes={opcoes} aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />
      )}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDoOcupante
          ocupante={janela.ocupante}
          podeGerenciar={podeGerenciar}
          aoFechar={() => setJanela(null)}
          aoEditar={() => setJanela({ tipo: 'editar', ocupante: janela.ocupante })}
          aoRemover={() => remover(janela.ocupante)}
        />
      )}
    </div>
  );
}
