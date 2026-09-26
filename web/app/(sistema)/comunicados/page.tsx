'use client';

import { useEffect, useState } from 'react';
import { format, parseISO } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Paperclip, Pencil, Plus, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import { DetalhesDoComunicado } from '@/components/comunicados/DetalhesDoComunicado';
import { FormularioDeComunicado } from '@/components/comunicados/FormularioDeComunicado';
import { SelosDoComunicado } from '@/components/comunicados/SelosDoComunicado';
import { Abas, Botao, CabecalhoDaPagina, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import {
  comunicadoService,
  type Comunicado,
  type OpcoesComunicado,
  type PublicoDoComunicado,
} from '@/services/comunicadoService';
import type { Pagina } from '@/services/tipos';
import { formatarMomento, mensagemErroApi } from '@/services/utilitarios';

const TAMANHO = 10;

type Destaque = '' | 'urgentes';

type Janela =
  | { tipo: 'publicar' }
  | { tipo: 'editar'; comunicado: Comunicado }
  | { tipo: 'ler'; comunicado: Comunicado }
  | null;

/** Dia e mês do comunicado, no bloco à esquerda de cada item. */
function DataDoComunicado({ data }: { data: string }) {
  const dia = parseISO(data);
  return (
    <span className="flex size-12 shrink-0 flex-col items-center justify-center rounded-xl border border-borda bg-superficie" aria-hidden>
      <span className="text-[10px] font-extrabold text-ouro uppercase">{format(dia, 'MMM', { locale: ptBR }).replace('.', '')}</span>
      <span className="font-titulo text-[22px] leading-none">{format(dia, 'dd')}</span>
    </span>
  );
}

export default function PaginaDeComunicados() {
  const { condominio } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesComunicado | null>(null);
  const [destaque, setDestaque] = useState<Destaque>('');
  const [publico, setPublico] = useState<PublicoDoComunicado | ''>('');
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Comunicado> | null>(null);
  const [janela, setJanela] = useState<Janela>(null);
  // Sobe a cada publicação, edição ou exclusão, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  // Os comunicados vêm do condomínio de quem está logado; a troca de condomínio também relê a lista
  const condominioId = condominio?.id;
  const consulta = JSON.stringify({ condominioId, busca, destaque, publico, pagina, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    comunicadoService
      .opcoes()
      .then(setOpcoes)
      .catch((e) => toast.error(mensagemErroApi(e, 'Não foi possível carregar as opções de comunicado.')));
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
    comunicadoService
      .listar({
        titulo: busca,
        publicoDestino: publico,
        urgente: destaque === 'urgentes' ? true : null,
        pagina,
        tamanho: TAMANHO,
      })
      .then((novaLista) => {
        if (ativa) setLista(novaLista);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os comunicados.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, busca, destaque, publico, pagina]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  const abas: Aba<Destaque>[] = [
    { valor: '', rotulo: 'Todos' },
    { valor: 'urgentes', rotulo: 'Urgentes' },
  ];

  const podePublicar = opcoes?.podeGerenciar === true;
  const itens = lista?.itens ?? [];
  const filtrando = Boolean(busca || destaque || publico);

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Convivência" titulo="Comunicados">
        {podePublicar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'publicar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Publicar comunicado
          </Botao>
        )}
      </CabecalhoDaPagina>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Abas
          rotulo="Destaque"
          abas={abas}
          valor={destaque}
          aoMudar={(valor) => {
            setDestaque(valor);
            setPagina(0);
          }}
        />
        <div className="flex w-full flex-wrap items-center gap-2.5 sm:w-auto">
          {podePublicar && opcoes && (
            <label className="flex h-10 w-full items-center gap-2 rounded-[10px] border border-borda bg-superficie pr-1 pl-3 text-sm text-apagado sm:w-auto">
              <span className="shrink-0">Para</span>
              <select
                value={publico}
                onChange={(e) => {
                  setPublico(e.target.value as PublicoDoComunicado | '');
                  setPagina(0);
                }}
                className="h-full min-w-0 grow cursor-pointer border-0 bg-transparent font-semibold text-tinta outline-none"
              >
                <option value="">qualquer público</option>
                {opcoes.publicos.map((p) => (
                  <option key={p.valor} value={p.valor}>
                    {p.descricao}
                  </option>
                ))}
              </select>
            </label>
          )}
          <label className="flex h-10 w-full items-center gap-2 rounded-[10px] border border-borda bg-superficie px-3 text-apagado sm:w-72">
            <Search size={16} aria-hidden />
            <span className="sr-only">Buscar comunicados pelo título</span>
            <input
              type="search"
              value={buscaDigitada}
              onChange={(e) => setBuscaDigitada(e.target.value)}
              placeholder="Buscar pelo título"
              className="min-w-0 grow border-0 bg-transparent text-sm text-tinta outline-none placeholder:text-apagado"
            />
          </label>
        </div>
      </div>

      <Cartao aria-label="Lista de comunicados" aria-busy={carregando}>
        {!carregando && itens.length === 0 && (
          <Vazio>{filtrando ? 'Nenhum comunicado com esses filtros.' : 'Nenhum comunicado publicado ainda.'}</Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((comunicado) => (
            <li
              key={comunicado.id}
              className={`flex items-start gap-1 border-b border-borda-suave last:border-b-0 ${comunicado.urgente ? 'bg-perigo-fundo/35' : ''}`}
            >
              <button
                type="button"
                onClick={() => setJanela({ tipo: 'ler', comunicado })}
                className="flex min-w-0 grow cursor-pointer items-start gap-4 px-5 py-4 text-left hover:bg-cabecalho"
              >
                <DataDoComunicado data={comunicado.dataCadastro} />
                <span className="flex min-w-0 grow flex-col gap-1.5">
                  <span className="text-base font-extrabold break-words">{comunicado.titulo}</span>
                  <span className="line-clamp-2 text-sm leading-relaxed break-words text-tinta-2">{comunicado.mensagem}</span>
                  <span className="flex flex-wrap items-center gap-x-2.5 gap-y-1.5 text-[13px] text-apagado">
                    <SelosDoComunicado comunicado={comunicado} />
                    {comunicado.possuiAnexo && (
                      <span className="inline-flex items-center gap-1">
                        <Paperclip size={13} aria-hidden />
                        Com anexo
                      </span>
                    )}
                    <span>
                      {formatarMomento(comunicado.dataCadastro)}
                      {comunicado.autor ? ` · por ${comunicado.autor}` : ''}
                    </span>
                  </span>
                </span>
              </button>
              {comunicado.podeGerenciar && (
                <button
                  type="button"
                  aria-label={`Editar o comunicado ${comunicado.titulo}`}
                  onClick={() => setJanela({ tipo: 'editar', comunicado })}
                  className="mt-3 mr-3 flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-tinta lg:size-[34px]"
                >
                  <Pencil size={16} aria-hidden />
                </button>
              )}
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
            nome="comunicados"
          />
        )}
      </Cartao>

      {janela?.tipo === 'ler' && (
        <DetalhesDoComunicado
          comunicado={janela.comunicado}
          aoFechar={() => setJanela(null)}
          aoEditar={() => setJanela({ tipo: 'editar', comunicado: janela.comunicado })}
          aoExcluir={aposAlterar}
        />
      )}
      {(janela?.tipo === 'publicar' || janela?.tipo === 'editar') && opcoes && (
        <FormularioDeComunicado
          comunicado={janela.tipo === 'editar' ? janela.comunicado : undefined}
          opcoes={opcoes}
          aoFechar={() => setJanela(null)}
          aoSalvar={aposAlterar}
        />
      )}
    </div>
  );
}
