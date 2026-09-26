'use client';

import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { FormularioDoCondominio } from '@/components/condominios/FormularioDoCondominio';
import { Abas, Botao, CabecalhoDaPagina, CampoDeBusca, Cartao, Paginacao, Selo, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { condominioService, type Condominio, type OpcoesCondominio } from '@/services/condominioService';
import { confirmar } from '@/services/confirmacao';
import { mensagemErroApi, textoLegivelDeCodigo } from '@/services/utilitarios';

const TAMANHO = 10;
const COLUNAS = 'lg:grid-cols-[minmax(0,2fr)_minmax(0,1.2fr)_110px_90px_110px_100px_200px]';

type Situacao = 'ativos' | 'inativos' | 'todos';

type Janela = { tipo: 'novo' } | { tipo: 'editar'; condominio: Condominio } | null;

function endereco(c: Condominio): string {
  const rua = [c.logradouro, c.numero].filter(Boolean).join(', ');
  return [rua, c.complemento, c.bairro].filter(Boolean).join(' · ');
}

function cidade(c: Condominio): string {
  return [c.cidade, c.estado].filter(Boolean).join(' / ');
}

export default function PaginaDeCondominios() {
  const { usuario, carregando: carregandoSessao, recarregar } = useSessao();
  const administradorGeral = usuario?.administradorGeral === true;
  const [opcoes, setOpcoes] = useState<OpcoesCondominio | null>(null);
  const [condominios, setCondominios] = useState<Condominio[] | null>(null);
  const [situacao, setSituacao] = useState<Situacao>('ativos');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [janela, setJanela] = useState<Janela>(null);
  const [alterando, setAlterando] = useState<number | null>(null);
  // Sobe a cada cadastro, edição, ativação ou inativação, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [versaoCarregada, setVersaoCarregada] = useState(-1);
  const carregando = versao !== versaoCarregada;

  useEffect(() => {
    if (!administradorGeral) return;
    let ativa = true;
    Promise.all([condominioService.listar(true), condominioService.opcoes()])
      .then(([lista, novasOpcoes]) => {
        if (!ativa) return;
        setCondominios(lista);
        setOpcoes(novasOpcoes);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar os condomínios.'));
      })
      .finally(() => {
        if (ativa) setVersaoCarregada(versao);
      });
    return () => {
      ativa = false;
    };
  }, [administradorGeral, versao]);

  const aposSalvar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
    // A escolha de condomínio do topo passa a mostrar o novo (ou o nome novo)
    recarregar();
  };

  async function mudarSituacao(condominio: Condominio) {
    if (condominio.ativo) {
      const confirmado = await confirmar({
        titulo: 'Inativar condomínio',
        mensagem: `${condominio.nome} sai das listas do sistema. Só é possível inativar um condomínio sem unidades e sem pessoas com acesso; ele pode ser reativado depois.`,
        textoConfirmar: 'Inativar',
        perigo: true,
      });
      if (!confirmado) return;
    }
    setAlterando(condominio.id);
    try {
      if (condominio.ativo) {
        await condominioService.inativar(condominio.id);
        toast.success('Condomínio inativado.');
      } else {
        await condominioService.ativar(condominio.id);
        toast.success('Condomínio reativado.');
      }
      setVersao((v) => v + 1);
      recarregar();
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível mudar a situação do condomínio.'));
    } finally {
      setAlterando(null);
    }
  }

  if (!carregandoSessao && !administradorGeral) {
    return (
      <div className="flex flex-col gap-5">
        <CabecalhoDaPagina secao="Administração" titulo="Condomínios" />
        <Cartao>
          <Vazio>Só a administração geral do sistema cadastra e altera condomínios.</Vazio>
        </Cartao>
      </div>
    );
  }

  const podeGerenciar = opcoes?.podeGerenciar === true;
  const descricaoDaTipologia = (valor: string) =>
    opcoes?.tipologias.find((t) => t.valor === valor)?.descricao ?? textoLegivelDeCodigo(valor);

  const termo = busca.trim().toLocaleLowerCase('pt-BR');
  const buscados = (condominios ?? []).filter(
    (c) => !termo || [c.nome, c.cidade, c.bairro, c.logradouro].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR').includes(termo),
  );
  const ativos = buscados.filter((c) => c.ativo);
  const inativos = buscados.filter((c) => !c.ativo);
  const filtrados = situacao === 'ativos' ? ativos : situacao === 'inativos' ? inativos : buscados;
  const totalPaginas = Math.ceil(filtrados.length / TAMANHO);
  const paginaAtual = Math.min(pagina, Math.max(totalPaginas - 1, 0));
  const itens = filtrados.slice(paginaAtual * TAMANHO, (paginaAtual + 1) * TAMANHO);

  const abas: Aba<Situacao>[] = [
    { valor: 'ativos', rotulo: 'Ativos', contagem: condominios ? ativos.length : null },
    { valor: 'inativos', rotulo: 'Inativos', contagem: condominios ? inativos.length : null },
    { valor: 'todos', rotulo: 'Todos', contagem: condominios ? buscados.length : null },
  ];

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Administração" titulo="Condomínios">
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'novo' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Novo condomínio
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
        <CampoDeBusca
          valor={busca}
          aoMudar={(valor) => {
            setBusca(valor);
            setPagina(0);
          }}
          rotulo="Buscar condomínios"
          dica="Nome, cidade ou bairro"
        />
      </div>

      <Cartao aria-label="Lista de condomínios" aria-busy={carregando}>
        <div
          className={`hidden gap-3 border-b border-borda-suave bg-cabecalho px-5 py-[11px] text-xs font-bold tracking-[0.04em] text-apagado uppercase lg:grid ${COLUNAS}`}
        >
          <span>Condomínio</span>
          <span>Cidade</span>
          <span>Tipo</span>
          <span className="text-right">Unidades</span>
          <span>Vencimento</span>
          <span>Situação</span>
          <span className="text-right">Ações</span>
        </div>

        {!carregando && itens.length === 0 && (
          <Vazio>
            {termo
              ? 'Nenhum condomínio com essa busca.'
              : situacao === 'inativos'
                ? 'Nenhum condomínio inativo.'
                : 'Nenhum condomínio cadastrado ainda.'}
          </Vazio>
        )}

        <ul className="m-0 list-none p-0">
          {itens.map((c) => (
            <li
              key={c.id}
              className={`grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1 border-b border-borda-suave px-5 py-3.5 text-sm last:border-b-0 lg:min-h-[60px] lg:py-2.5 ${COLUNAS}`}
            >
              <span className="flex min-w-0 flex-col gap-0.5">
                <span className="font-extrabold">{c.nome}</span>
                {endereco(c) && <span className="truncate text-[13px] text-apagado">{endereco(c)}</span>}
              </span>
              <span className="justify-self-end lg:hidden">
                <Selo tom={c.ativo ? 'info' : 'neutro'}>{c.ativo ? 'Ativo' : 'Inativo'}</Selo>
              </span>
              <span className="col-span-2 text-tinta-2 lg:col-span-1">
                {cidade(c) || '—'}
                <span className="lg:hidden">
                  {' · '}
                  {descricaoDaTipologia(String(c.tipologia))}
                  {c.numeroUnidades != null && ` · ${c.numeroUnidades} unidades`}
                  {c.diaVencimentoTaxa != null && ` · vence dia ${c.diaVencimentoTaxa}`}
                </span>
              </span>
              <span className="hidden text-tinta-2 lg:block">{descricaoDaTipologia(String(c.tipologia))}</span>
              <span className="hidden text-right tabular-nums lg:block">{c.numeroUnidades ?? '—'}</span>
              <span className="hidden text-apagado lg:block">{c.diaVencimentoTaxa != null ? `Dia ${c.diaVencimentoTaxa}` : '—'}</span>
              <span className="hidden lg:flex">
                <Selo tom={c.ativo ? 'info' : 'neutro'}>{c.ativo ? 'Ativo' : 'Inativo'}</Selo>
              </span>
              <span className="col-span-2 mt-1.5 flex items-center justify-end gap-1.5 lg:col-span-1 lg:mt-0">
                {podeGerenciar && (
                  <>
                    <Botao pequeno onClick={() => setJanela({ tipo: 'editar', condominio: c })} className="max-lg:h-11 max-lg:grow">
                      Editar
                    </Botao>
                    <Botao
                      pequeno
                      variante="texto"
                      onClick={() => mudarSituacao(c)}
                      carregando={alterando === c.id}
                      disabled={alterando !== null}
                      aria-label={`${c.ativo ? 'Inativar' : 'Reativar'} ${c.nome}`}
                      className="max-lg:h-11 max-lg:grow"
                    >
                      {c.ativo ? 'Inativar' : 'Reativar'}
                    </Botao>
                  </>
                )}
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
          nome="condomínios"
        />
      </Cartao>

      {janela && opcoes && (
        <FormularioDoCondominio
          condominio={janela.tipo === 'editar' ? janela.condominio : null}
          tipologias={opcoes.tipologias}
          aoFechar={() => setJanela(null)}
          aoSalvar={aposSalvar}
        />
      )}
    </div>
  );
}
