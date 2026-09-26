'use client';

import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { CartaoDaAreaComum } from '@/components/areas-comuns/CartaoDaAreaComum';
import { FormularioDaAreaComum } from '@/components/areas-comuns/FormularioDaAreaComum';
import { Abas, Botao, CabecalhoDaPagina, CampoDeBusca, Cartao, Paginacao, Vazio, type Aba } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import {
  areaComumService,
  pedidoDaArea,
  type AreaComum,
  type OpcoesAreaComum,
  type TotaisAreasComuns,
} from '@/services/areaComumService';
import { confirmar } from '@/services/confirmacao';
import { mensagemErroApi, statusDoErro } from '@/services/utilitarios';

// Cada condomínio tem poucas áreas: vêm todas de uma vez (o máximo da API) e a tela filtra e pagina
const LIMITE_DA_API = 100;
const TAMANHO = 12;

type Situacao = '' | 'ATIVAS' | 'INATIVAS';

type Janela = { tipo: 'cadastrar' } | { tipo: 'editar'; area: AreaComum } | null;

export default function PaginaDeAreasComuns() {
  const { condominio } = useSessao();
  const [opcoes, setOpcoes] = useState<OpcoesAreaComum | null>(null);
  const [situacao, setSituacao] = useState<Situacao>('');
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(0);
  const [areas, setAreas] = useState<AreaComum[]>([]);
  const [totais, setTotais] = useState<TotaisAreasComuns | null>(null);
  const [semAcesso, setSemAcesso] = useState('');
  const [janela, setJanela] = useState<Janela>(null);
  const [emAndamento, setEmAndamento] = useState<number | null>(null);
  // Sobe a cada cadastro, edição ou exclusão, para a lista ser lida de novo
  const [versao, setVersao] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const condominioId = condominio?.id;
  const consulta = JSON.stringify({ condominioId, busca, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    areaComumService
      .opcoes()
      .then(setOpcoes)
      .catch((e) => toast.error(mensagemErroApi(e, 'Não foi possível carregar as permissões de áreas comuns.')));
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
      areaComumService.listar({ condominioId, busca, pagina: 0, tamanho: LIMITE_DA_API }),
      areaComumService.totais(condominioId, busca),
    ])
      .then(([lista, novosTotais]) => {
        if (!ativa) return;
        setAreas(lista.itens);
        setTotais(novosTotais);
        setSemAcesso('');
      })
      .catch((e) => {
        if (!ativa) return;
        setAreas([]);
        setTotais(null);
        if (statusDoErro(e) === 403) {
          setSemAcesso(mensagemErroApi(e, 'Você não gerencia as áreas comuns deste condomínio.'));
        } else {
          toast.error(mensagemErroApi(e, 'Não foi possível carregar as áreas comuns.'));
        }
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, busca]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  async function mudarSituacao(area: AreaComum) {
    if (area.ativa) {
      const certeza = await confirmar({
        titulo: 'Inativar área comum',
        mensagem: `“${area.nome}” deixa de aparecer para quem vai pedir reserva. As reservas já feitas continuam valendo, e você pode reativar a área depois.`,
        textoConfirmar: 'Inativar',
        perigo: true,
      });
      if (!certeza) return;
    }
    setEmAndamento(area.codigo);
    try {
      // Lê a área de novo antes de gravar: a edição troca a área inteira, turnos incluídos
      const atual = await areaComumService.buscar(area.codigo);
      await areaComumService.atualizar(area.codigo, { ...pedidoDaArea(atual), ativa: !atual.ativa });
      toast.success(atual.ativa ? 'Área comum inativada.' : 'Área comum reativada.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível mudar a situação da área.'));
    } finally {
      setEmAndamento(null);
    }
  }

  async function excluir(area: AreaComum) {
    const certeza = await confirmar({
      titulo: 'Excluir área comum',
      mensagem: `“${area.nome}” será excluída de vez, com os turnos. Área que já teve reservas não pode ser excluída; nesse caso, inative-a.`,
      textoConfirmar: 'Excluir',
      perigo: true,
    });
    if (!certeza) return;
    setEmAndamento(area.codigo);
    try {
      await areaComumService.excluir(area.codigo);
      toast.success('Área comum excluída.');
      setVersao((v) => v + 1);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível excluir a área comum.'));
    } finally {
      setEmAndamento(null);
    }
  }

  const abas: Aba<Situacao>[] = [
    { valor: '', rotulo: 'Todas', contagem: totais?.TOTAL },
    { valor: 'ATIVAS', rotulo: 'Disponíveis', contagem: totais?.ATIVAS },
    { valor: 'INATIVAS', rotulo: 'Inativas', contagem: totais?.INATIVAS },
  ];

  const podeCadastrar =
    opcoes?.podeGerenciar === true && condominio != null && opcoes.condominios.some((c) => c.codigo === condominio.id);
  const filtradas = areas.filter((a) => (situacao === 'ATIVAS' ? a.ativa : situacao === 'INATIVAS' ? !a.ativa : true));
  const totalPaginas = Math.ceil(filtradas.length / TAMANHO);
  const paginaVisivel = Math.min(pagina, Math.max(totalPaginas - 1, 0));
  const itens = filtradas.slice(paginaVisivel * TAMANHO, (paginaVisivel + 1) * TAMANHO);

  let vazio = '';
  if (semAcesso) vazio = semAcesso;
  else if (busca || situacao) vazio = 'Nenhuma área comum com esses filtros.';
  else vazio = 'Nenhuma área comum cadastrada ainda. Cadastre o salão de festas, a churrasqueira e as outras áreas que os moradores podem reservar.';

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Convivência" titulo="Áreas comuns">
        {podeCadastrar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'cadastrar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Nova área
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
        <CampoDeBusca valor={buscaDigitada} aoMudar={setBuscaDigitada} rotulo="Buscar áreas comuns" dica="Nome ou descrição" />
      </div>

      {!carregando && itens.length === 0 ? (
        <Cartao>
          <Vazio>{vazio}</Vazio>
        </Cartao>
      ) : (
        <ul aria-label="Áreas comuns" aria-busy={carregando} className="m-0 grid list-none gap-4 p-0 md:grid-cols-2 2xl:grid-cols-3">
          {itens.map((area) => (
            <CartaoDaAreaComum
              key={area.codigo}
              area={area}
              ocupada={emAndamento === area.codigo}
              aoEditar={() => setJanela({ tipo: 'editar', area })}
              aoMudarSituacao={() => mudarSituacao(area)}
              aoExcluir={() => excluir(area)}
            />
          ))}
        </ul>
      )}

      {totalPaginas > 1 && (
        <Cartao className="[&>div]:border-t-0">
          <Paginacao
            pagina={paginaVisivel}
            totalPaginas={totalPaginas}
            totalItens={filtradas.length}
            tamanho={TAMANHO}
            aoMudar={setPagina}
            nome="áreas"
          />
        </Cartao>
      )}

      {janela?.tipo === 'cadastrar' && <FormularioDaAreaComum aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />}
      {janela?.tipo === 'editar' && <FormularioDaAreaComum area={janela.area} aoFechar={() => setJanela(null)} aoSalvar={aposAlterar} />}
    </div>
  );
}
