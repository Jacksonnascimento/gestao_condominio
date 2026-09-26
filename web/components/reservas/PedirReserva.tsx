'use client';

import { useEffect, useRef, useState } from 'react';
import { addDays, format } from 'date-fns';
import { Plus, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { CaixaDeMarcar } from '@/components/areas-comuns/CaixaDeMarcar';
import { Botao, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { faixaDeHorario, regrasDaArea } from '@/services/areaComumService';
import { reservaService, type OpcoesReserva } from '@/services/reservaService';
import { mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

/** Valor do turno "dia inteiro" na escolha; na API, é a reserva sem turno. */
const DIA_INTEIRO = 'dia-inteiro';

interface ConvidadoNoFormulario {
  chave: number;
  nome: string;
  documento: string;
}

let proximaChave = 0;

function dataDoCampo(diasAFrente: number): string {
  return format(addDays(new Date(), diasAFrente), 'yyyy-MM-dd');
}

/**
 * Pedido de reserva do morador: unidade, área, data, turno (ou o dia inteiro), convidados se a área aceitar, e o
 * aceite dos termos de uso. A reserva fica aguardando a aprovação da gestão.
 */
export function PedirReserva({
  opcoes,
  aoFechar,
  aoPedir,
}: {
  opcoes: OpcoesReserva;
  aoFechar: () => void;
  aoPedir: () => void;
}) {
  const unidades = opcoes.unidades;
  const [unidadeId, setUnidadeId] = useState(unidades.length === 1 ? String(unidades[0].codigo) : '');
  const [areaId, setAreaId] = useState('');
  const [data, setData] = useState('');
  const [turno, setTurno] = useState('');
  const [convidados, setConvidados] = useState<ConvidadoNoFormulario[]>([]);
  const [termosAceitos, setTermosAceitos] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const caixaDoErro = useRef<HTMLParagraphElement>(null);

  // O formulário é longo: o erro aparece no topo, então a janela rola até ele
  useEffect(() => {
    if (erro) caixaDoErro.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }, [erro]);

  const unidade = unidades.find((u) => String(u.codigo) === unidadeId);
  const areas = opcoes.areasParaSolicitar.filter((a) => !unidade || a.condominioCodigo === unidade.condominioCodigo);
  const area = areas.find((a) => String(a.codigo) === areaId);
  const turnos = area?.turnos.filter((t) => t.ativo) ?? [];
  const dataMinima = area ? dataDoCampo(area.diasAntecedenciaMin ?? 0) : undefined;
  const dataMaxima = area?.diasAntecedenciaMax != null ? dataDoCampo(area.diasAntecedenciaMax) : undefined;
  const limite = area?.permiteConvidados ? area.limiteConvidados : 0;
  const noLimite = limite != null && convidados.length >= limite;

  function escolherUnidade(valor: string) {
    setUnidadeId(valor);
    const nova = unidades.find((u) => String(u.codigo) === valor);
    if (area && nova && area.condominioCodigo !== nova.condominioCodigo) escolherArea('');
  }

  function escolherArea(valor: string) {
    setAreaId(valor);
    const nova = opcoes.areasParaSolicitar.find((a) => String(a.codigo) === valor);
    // Cada área tem os seus turnos e as suas regras: o que dependia da área anterior recomeça
    setTurno(nova && nova.turnos.every((t) => !t.ativo) ? DIA_INTEIRO : '');
    setTermosAceitos(false);
    if (!nova?.permiteConvidados) setConvidados([]);
    else if (nova.limiteConvidados != null) setConvidados((atuais) => atuais.slice(0, nova.limiteConvidados ?? undefined));
    if (nova && data) {
      const min = dataDoCampo(nova.diasAntecedenciaMin ?? 0);
      const max = nova.diasAntecedenciaMax != null ? dataDoCampo(nova.diasAntecedenciaMax) : null;
      if (data < min || (max && data > max)) setData('');
    }
  }

  const adicionarConvidado = () => setConvidados((atuais) => [...atuais, { chave: ++proximaChave, nome: '', documento: '' }]);

  const mudarConvidado = (chave: number, campo: 'nome' | 'documento', valor: string) =>
    setConvidados((atuais) => atuais.map((c) => (c.chave === chave ? { ...c, [campo]: valor } : c)));

  const removerConvidado = (chave: number) => setConvidados((atuais) => atuais.filter((c) => c.chave !== chave));

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!area || !unidade) return;
    if (!turno) {
      setErro('Escolha o turno ou o dia inteiro.');
      return;
    }
    setErro('');
    setSalvando(true);
    try {
      await reservaService.solicitar({
        areaId: area.codigo,
        turnoId: turno === DIA_INTEIRO ? undefined : Number(turno),
        unidadeId: unidade.codigo,
        data,
        termosAceitos,
        convidados: convidados
          .filter((c) => c.nome.trim())
          .map((c) => ({ nome: c.nome.trim(), documento: c.documento.trim() || undefined })),
      });
      toast.success('Pedido de reserva enviado. Agora é aguardar a aprovação.');
      aoPedir();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível enviar o pedido de reserva.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo="Pedir reserva"
      subtitulo="A reserva fica aguardando a aprovação do síndico ou da administração."
      largura="lg"
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="pedir-reserva" variante="primario" carregando={salvando} disabled={!area}>
            Enviar pedido
          </Botao>
        </>
      }
    >
      <form id="pedir-reserva" onSubmit={salvar} className="flex flex-col gap-5">
        {erro && (
          <p ref={caixaDoErro} className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo" role="alert">
            {erro}
          </p>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          {unidades.length > 1 ? (
            <CampoDeSelecao rotulo="Unidade" value={unidadeId} onChange={(e) => escolherUnidade(e.target.value)} obrigatorio>
              <option value="">Escolha a unidade</option>
              {unidades.map((u) => (
                <option key={u.codigo} value={u.codigo}>
                  {rotuloUnidade(u.numero, u.bloco)}
                  {u.condominioNome ? ` · ${u.condominioNome}` : ''}
                </option>
              ))}
            </CampoDeSelecao>
          ) : (
            <div className="flex flex-col gap-1.5">
              <span className="text-[13px] font-semibold text-tinta-2">Unidade</span>
              <span className="flex h-11 items-center text-sm font-bold">{unidade ? rotuloUnidade(unidade.numero, unidade.bloco) : '—'}</span>
            </div>
          )}
          <CampoDeSelecao rotulo="Área" value={areaId} onChange={(e) => escolherArea(e.target.value)} obrigatorio disabled={!unidade}>
            <option value="">{areas.length === 0 ? 'Nenhuma área disponível' : 'Escolha a área'}</option>
            {areas.map((a) => (
              <option key={a.codigo} value={a.codigo}>
                {a.nome}
              </option>
            ))}
          </CampoDeSelecao>
        </div>

        {area && (
          <>
            <div className="flex flex-col gap-2 rounded-xl bg-cabecalho px-4 py-3.5">
              {area.descricao && <p className="text-sm text-tinta-2">{area.descricao}</p>}
              <ul className="m-0 flex list-none flex-wrap gap-x-4 gap-y-1 p-0 text-[13px] font-semibold text-apagado">
                {regrasDaArea(area).map((regra) => (
                  <li key={regra}>{regra}</li>
                ))}
              </ul>
            </div>

            <Campo
              rotulo="Data"
              type="date"
              value={data}
              onChange={(e) => setData(e.target.value)}
              obrigatorio
              min={dataMinima}
              max={dataMaxima}
              className="sm:max-w-[240px]"
            />

            <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
              <legend className="mb-1.5 p-0 text-[13px] font-semibold text-tinta-2">
                Turno<span className="text-ouro"> *</span>
              </legend>
              {turnos.length === 0 ? (
                <p className="text-sm text-tinta-2">Esta área é reservada pelo dia inteiro.</p>
              ) : (
                <div className="grid gap-2 sm:grid-cols-2">
                  {[
                    ...turnos.map((t) => ({ valor: String(t.codigo), titulo: t.nome, detalhe: faixaDeHorario(t.horaInicio, t.horaFim) })),
                    { valor: DIA_INTEIRO, titulo: 'Dia inteiro', detalhe: 'Ocupa todos os turnos da data' },
                  ].map((opcao) => (
                    <label
                      key={opcao.valor}
                      className={`flex min-h-11 cursor-pointer items-center gap-3 rounded-xl border px-3.5 py-2.5 text-sm ${
                        turno === opcao.valor ? 'border-ouro bg-realce' : 'border-borda bg-superficie hover:bg-lateral'
                      }`}
                    >
                      <input
                        type="radio"
                        name="turno"
                        value={opcao.valor}
                        checked={turno === opcao.valor}
                        onChange={() => setTurno(opcao.valor)}
                        className="size-4 shrink-0 accent-ouro"
                      />
                      <span className="flex flex-col">
                        <span className="font-bold">{opcao.titulo}</span>
                        <span className="text-xs text-apagado">{opcao.detalhe}</span>
                      </span>
                    </label>
                  ))}
                </div>
              )}
            </fieldset>

            {area.permiteConvidados && (
              <fieldset className="m-0 flex min-w-0 flex-col gap-3 border-0 p-0">
                <legend className="mb-1.5 p-0 text-[13px] font-semibold text-tinta-2">
                  Convidados de fora
                  <span className="font-normal text-apagado">
                    {' '}
                    ({convidados.length}
                    {limite != null ? ` de ${limite}` : ''})
                  </span>
                </legend>
                {convidados.length > 0 && (
                  <ul className="m-0 flex list-none flex-col gap-2 p-0">
                    {convidados.map((c, indice) => (
                      <li key={c.chave} className="grid grid-cols-[minmax(0,1fr)_auto] items-end gap-2 sm:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_auto]">
                        <Campo
                          rotulo={`Nome do convidado ${indice + 1}`}
                          value={c.nome}
                          onChange={(e) => mudarConvidado(c.chave, 'nome', e.target.value)}
                          obrigatorio
                          maxLength={150}
                          className="col-span-2 sm:col-span-1"
                        />
                        <Campo
                          rotulo="Documento (opcional)"
                          value={c.documento}
                          onChange={(e) => mudarConvidado(c.chave, 'documento', e.target.value)}
                          maxLength={50}
                        />
                        <button
                          type="button"
                          aria-label={`Remover o convidado ${c.nome || indice + 1}`}
                          onClick={() => removerConvidado(c.chave)}
                          className="flex size-11 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-perigo"
                        >
                          <Trash2 size={17} aria-hidden />
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                <div>
                  <Botao pequeno onClick={adicionarConvidado} disabled={noLimite} className="max-lg:h-11">
                    <Plus size={15} aria-hidden />
                    Adicionar convidado
                  </Botao>
                  {noLimite && limite != null && limite > 0 && (
                    <span className="ml-3 text-xs text-apagado">Limite de {limite} convidados atingido.</span>
                  )}
                </div>
              </fieldset>
            )}

            {area.termosUso && (
              <div className="flex flex-col gap-1.5">
                <span className="text-[13px] font-semibold text-tinta-2">Termos de uso</span>
                <div
                  tabIndex={0}
                  aria-label="Termos de uso da área"
                  className="max-h-40 overflow-y-auto rounded-xl border border-borda-suave bg-lateral px-4 py-3 text-sm whitespace-pre-line text-tinta-2"
                >
                  {area.termosUso}
                </div>
              </div>
            )}
            <CaixaDeMarcar
              rotulo={area.termosUso ? 'Li e aceito os termos de uso' : 'Concordo em seguir as regras de uso da área'}
              checked={termosAceitos}
              onChange={(e) => setTermosAceitos(e.target.checked)}
              required
            />
          </>
        )}

        {!area && areas.length === 0 && unidade && (
          <p className="text-sm text-apagado">O condomínio ainda não tem áreas comuns disponíveis para reserva.</p>
        )}
      </form>
    </Modal>
  );
}
