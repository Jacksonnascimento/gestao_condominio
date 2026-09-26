'use client';

import { useEffect, useState } from 'react';
import { EllipsisVertical, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { CadastrarOcupante } from '@/components/ocupantes/CadastrarOcupante';
import { DetalhesDaUnidade } from '@/components/unidades/DetalhesDaUnidade';
import { FormularioDaUnidade } from '@/components/unidades/FormularioDaUnidade';
import { SeloDaUnidade, resumoDaUnidade } from '@/components/unidades/SeloDaUnidade';
import {
  Abas,
  Botao,
  CabecalhoDaPagina,
  CaixaDeMarcar,
  CampoDeBusca,
  Cartao,
  Paginacao,
  Vazio,
  type Aba,
} from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { confirmar } from '@/services/confirmacao';
import type { OpcoesOcupante } from '@/services/ocupanteService';
import type { Pagina } from '@/services/tipos';
import {
  unidadeService,
  type OpcoesUnidade,
  type SituacaoDaUnidade,
  type TotaisUnidades,
  type Unidade,
} from '@/services/unidadeService';
import { descricaoDoEnum, emFrase, formatarNumero, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[1.2fr_1fr_0.6fr_0.8fr_0.8fr_130px_150px]';

type Janela =
  | { tipo: 'cadastrar' }
  | { tipo: 'editar'; unidade: Unidade }
  | { tipo: 'detalhes'; unidade: Unidade }
  | { tipo: 'novoOcupante'; unidade: Unidade; opcoes: OpcoesOcupante }
  | null;

export default function PaginaDeUnidades() {
  const { condominio, usuario, papeis, permissoes } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesUnidade | null>(null);
  const [situacao, setSituacao] = useState<SituacaoDaUnidade | ''>('');
  const [incluirInativas, setIncluirInativas] = useState(false);
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Unidade> | null>(null);
  const [totais, setTotais] = useState<TotaisUnidades | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada cadastro, edição, inativação ou reativação, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const consulta = JSON.stringify({ condominioId, busca, situacao, incluirInativas, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    unidadeService
      .opcoes()
      .then(setOpcoes)
      .catch((e) => toast.error(mensagemErroApi(e, 'Não foi possível carregar os tipos de unidade.')));
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
      unidadeService.listar({ condominioId, busca, status: situacao, incluirInativas, pagina, tamanho: TAMANHO }),
      unidadeService.totais({ condominioId, busca, incluirInativas }),
    ])
      .then(([novaLista, novosTotais]) => {
        if (!ativa) return;
        setLista(novaLista);
        setTotais(novosTotais);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar as unidades.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, busca, situacao, incluirInativas, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  // A confirmação abre por cima da janela de detalhes; confirmada, a janela fecha
  async function inativar(unidade: Unidade) {
    const confirmado = await confirmar({
      titulo: 'Inativar unidade',
      mensagem: `A unidade ${rotuloUnidade(unidade.numero, unidade.bloco)} sai das listas e dos formulários do condomínio. Só é possível inativar uma unidade sem ocupantes. Ela pode ser reativada depois.`,
      textoConfirmar: 'Inativar',
      perigo: true,
    });
    if (!confirmado) return;
    setJanela(null);
    try {
      await unidadeService.inativar(unidade.id);
      toast.success('Unidade inativada.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível inativar a unidade.'));
    }
  }

  async function reativar(unidade: Unidade) {
    try {
      await unidadeService.reativar(unidade.id);
      toast.success('Unidade reativada.');
      aposAlterar();
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível reativar a unidade.'));
    }
  }

  const abas: Aba<SituacaoDaUnidade | ''>[] = [
    { valor: '', rotulo: 'Todas', contagem: totais?.TOTAL },
    { valor: 'OCUPADA', rotulo: 'Ocupadas', contagem: totais?.OCUPADA },
    { valor: 'VAZIA', rotulo: 'Vazias', contagem: totais?.VAZIA },
    { valor: 'EM_REFORMA', rotulo: 'Em reforma', contagem: totais?.EM_REFORMA },
    { valor: 'MULTIPROPRIEDADE', rotulo: 'Multipropriedade', contagem: totais?.MULTIPROPRIEDADE },
  ];

  // A API diz se a pessoa cadastra unidades em algum condomínio; a sessão, se é síndico ou administração neste
  const sindicoOuAdministracao = usuario?.administradorGeral === true || papeis.some((p) => p === 'SINDICO' || p === 'ADMIN');
  const podeGerenciar = opcoes?.podeGerenciar === true && sindicoOuAdministracao;
  const itens = lista?.itens ?? [];

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Cadastros" titulo="Unidades">
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'cadastrar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Nova unidade
          </Botao>
        )}
      </CabecalhoDaPagina>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Abas
          rotulo="Situação de ocupação"
          abas={abas}
          valor={situacao}
          aoMudar={(valor) => {
            setSituacao(valor);
            setPagina(0);
          }}
        />
        <div className="flex w-full flex-col gap-1 sm:w-auto sm:flex-row sm:items-center sm:gap-4">
          {permissoes.gestao && (
            <CaixaDeMarcar
              rotulo="Mostrar inativas"
              checked={incluirInativas}
              onChange={(e) => {
                setIncluirInativas(e.target.checked);
                setPagina(0);
              }}
            />
          )}
          <CampoDeBusca valor={buscaDigitada} aoMudar={setBuscaDigitada} rotulo="Buscar unidades" dica="Número ou bloco" />
        </div>
      </div>

      <Cartao aria-label="Lista de unidades" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Unidade</span>
          <span>Tipo</span>
          <span>Andar</span>
          <span>Área privada</span>
          <span>Fração ideal</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && (
          <Vazio>
            {busca || situacao
              ? 'Nenhuma unidade com esses filtros.'
              : podeGerenciar
                ? 'Nenhuma unidade cadastrada ainda.'
                : 'Nenhuma unidade para mostrar.'}
          </Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((unidade) => {
            const rotulo = rotuloUnidade(unidade.numero, unidade.bloco);
            return (
              <li
                key={unidade.id}
                className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm tabular-nums last:border-b-0 lg:min-h-[52px] lg:py-2 ${COLUNAS} ${
                  unidade.ativa ? '' : 'text-apagado'
                }`}
              >
                <span className="font-extrabold">{rotulo}</span>
                <span className="justify-self-end lg:hidden">
                  <SeloDaUnidade unidade={unidade} />
                </span>
                <span className="text-tinta-2 lg:hidden">{resumoDaUnidade(unidade)}</span>
                <span className="hidden text-tinta-2 lg:block">{emFrase(unidade.tipoDescricao ?? descricaoDoEnum(unidade.tipo)) || '—'}</span>
                <span className="hidden text-tinta-2 lg:block">{unidade.andar || '—'}</span>
                <span className="hidden text-tinta-2 lg:block">
                  {unidade.areaPrivada != null ? `${formatarNumero(unidade.areaPrivada)} m²` : '—'}
                </span>
                <span className="hidden text-tinta-2 lg:block">
                  {unidade.fracaoIdeal != null ? `${formatarNumero(unidade.fracaoIdeal)}%` : '—'}
                </span>
                <span className="hidden lg:flex">
                  <SeloDaUnidade unidade={unidade} />
                </span>
                <span className="flex items-center justify-end gap-1.5">
                  {podeGerenciar && unidade.ativa && (
                    <Botao pequeno onClick={() => setJanela({ tipo: 'editar', unidade })}>
                      Editar
                    </Botao>
                  )}
                  {podeGerenciar && !unidade.ativa && (
                    <Botao pequeno onClick={() => reativar(unidade)}>
                      Reativar
                    </Botao>
                  )}
                  <button
                    type="button"
                    aria-label={`Detalhes da unidade ${rotulo}`}
                    onClick={() => setJanela({ tipo: 'detalhes', unidade })}
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
            nome="unidades"
          />
        )}
      </Cartao>

      {janela?.tipo === 'cadastrar' && opcoes && (
        <FormularioDaUnidade opcoes={opcoes} aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />
      )}
      {janela?.tipo === 'editar' && opcoes && (
        <FormularioDaUnidade unidade={janela.unidade} opcoes={opcoes} aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />
      )}
      {janela?.tipo === 'detalhes' && (
        <DetalhesDaUnidade
          unidade={janela.unidade}
          podeGerenciar={podeGerenciar}
          aoFechar={() => setJanela(null)}
          aoEditar={() => setJanela({ tipo: 'editar', unidade: janela.unidade })}
          aoInativar={() => inativar(janela.unidade)}
          aoReativar={() => reativar(janela.unidade)}
          aoAdicionarOcupante={(opcoesDeOcupante) =>
            setJanela({ tipo: 'novoOcupante', unidade: janela.unidade, opcoes: opcoesDeOcupante })
          }
        />
      )}
      {janela?.tipo === 'novoOcupante' && (
        <CadastrarOcupante
          opcoes={janela.opcoes}
          unidadeFixa={{ id: janela.unidade.id, rotulo: rotuloUnidade(janela.unidade.numero, janela.unidade.bloco) }}
          aoFechar={() => setJanela({ tipo: 'detalhes', unidade: janela.unidade })}
          aoCadastrar={() => {
            setJanela({ tipo: 'detalhes', unidade: janela.unidade });
            setVersao((v) => v + 1);
          }}
        />
      )}
    </div>
  );
}
