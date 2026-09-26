'use client';

import { useEffect, useRef, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, CaixaDeMarcar, Campo, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import { areaComumService, type AreaComum, type PedidoDeAreaComum } from '@/services/areaComumService';
import { horario, mensagemErroApi, numeroOuNulo } from '@/services/utilitarios';

interface TurnoNoFormulario {
  /** Só para a lista da tela; turnos novos ainda não têm código. */
  chave: string;
  codigo?: number;
  nome: string;
  horaInicio: string;
  horaFim: string;
  ativo: boolean;
}

let proximaChave = 0;
const novaChave = () => `novo-${++proximaChave}`;

function Secao({ titulo, ajuda, children }: { titulo: string; ajuda?: string; children: React.ReactNode }) {
  return (
    <fieldset className="m-0 flex min-w-0 flex-col gap-4 border-0 p-0">
      <legend className="mb-3 p-0">
        <span className="block text-base font-extrabold">{titulo}</span>
        {ajuda && <span className="mt-0.5 block text-[13px] text-apagado">{ajuda}</span>}
      </legend>
      {children}
    </fieldset>
  );
}

/** Cadastro de uma área nova ou edição de uma existente, com as regras de uso e os turnos. */
export function FormularioDaAreaComum({
  area,
  aoFechar,
  aoSalvar,
}: {
  /** Sem área, é um cadastro novo no condomínio escolhido. */
  area?: AreaComum;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const { condominio } = useSessao();
  const editando = Boolean(area);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const caixaDoErro = useRef<HTMLParagraphElement>(null);

  // O formulário é longo: o erro aparece no topo, então a janela rola até ele
  useEffect(() => {
    if (erro) caixaDoErro.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }, [erro]);
  const [formulario, setFormulario] = useState({
    nome: area?.nome ?? '',
    descricao: area?.descricao ?? '',
    termosUso: area?.termosUso ?? '',
    capacidadeMaxima: area?.capacidadeMaxima?.toString() ?? '',
    taxaValor: area?.taxaValor?.toString() ?? '',
    diasAntecedenciaMin: (area?.diasAntecedenciaMin ?? 1).toString(),
    diasAntecedenciaMax: (area?.diasAntecedenciaMax ?? 30).toString(),
    permiteConvidados: area?.permiteConvidados ?? false,
    limiteConvidados: area?.limiteConvidados?.toString() ?? '',
    ativa: area?.ativa ?? true,
  });
  const [turnos, setTurnos] = useState<TurnoNoFormulario[]>(
    () =>
      area?.turnos.map((t) => ({
        chave: `turno-${t.codigo}`,
        codigo: t.codigo,
        nome: t.nome,
        horaInicio: horario(t.horaInicio),
        horaFim: horario(t.horaFim),
        ativo: t.ativo,
      })) ?? [],
  );

  const mudar = (campo: keyof typeof formulario) => (evento: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const alvo = evento.target;
    const valor = alvo instanceof HTMLInputElement && alvo.type === 'checkbox' ? alvo.checked : alvo.value;
    setFormulario((atual) => ({ ...atual, [campo]: valor }));
  };

  const mudarTurno = (chave: string, campo: 'nome' | 'horaInicio' | 'horaFim' | 'ativo', valor: string | boolean) =>
    setTurnos((atuais) => atuais.map((t) => (t.chave === chave ? { ...t, [campo]: valor } : t)));

  const adicionarTurno = () => setTurnos((atuais) => [...atuais, { chave: novaChave(), nome: '', horaInicio: '', horaFim: '', ativo: true }]);

  const removerTurno = (chave: string) => setTurnos((atuais) => atuais.filter((t) => t.chave !== chave));

  function conferir(): string {
    const min = numeroOuNulo(formulario.diasAntecedenciaMin);
    const max = numeroOuNulo(formulario.diasAntecedenciaMax);
    if (min != null && max != null && min > max) {
      return 'A antecedência mínima não pode ser maior que a máxima.';
    }
    const turnoInvertido = turnos.find((t) => t.horaInicio && t.horaFim && t.horaFim <= t.horaInicio);
    if (turnoInvertido) {
      return `No turno "${turnoInvertido.nome || 'sem nome'}", o fim precisa ser depois do início.`;
    }
    return '';
  }

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    const problema = conferir();
    setErro(problema);
    if (problema) return;
    if (!editando && !condominio) {
      setErro('Escolha um condomínio antes de cadastrar a área.');
      return;
    }
    const pedido: PedidoDeAreaComum = {
      condominioId: editando ? undefined : condominio?.id,
      nome: formulario.nome.trim(),
      descricao: formulario.descricao.trim() || null,
      termosUso: formulario.termosUso.trim() || null,
      capacidadeMaxima: numeroOuNulo(formulario.capacidadeMaxima),
      taxaValor: numeroOuNulo(formulario.taxaValor),
      diasAntecedenciaMin: numeroOuNulo(formulario.diasAntecedenciaMin),
      diasAntecedenciaMax: numeroOuNulo(formulario.diasAntecedenciaMax),
      permiteConvidados: formulario.permiteConvidados,
      limiteConvidados: formulario.permiteConvidados ? numeroOuNulo(formulario.limiteConvidados) : null,
      ativa: formulario.ativa,
      turnos: turnos.map((t) => ({
        codigo: t.codigo,
        nome: t.nome.trim(),
        horaInicio: t.horaInicio,
        horaFim: t.horaFim,
        ativo: t.ativo,
      })),
    };
    setSalvando(true);
    try {
      if (area) {
        await areaComumService.atualizar(area.codigo, pedido);
        toast.success('Área comum atualizada.');
      } else {
        await areaComumService.cadastrar(pedido);
        toast.success('Área comum cadastrada.');
      }
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível salvar a área comum.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo={editando ? 'Editar área comum' : 'Nova área comum'}
      subtitulo={area?.condominioNome ?? condominio?.nome}
      largura="lg"
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="formulario-area-comum" variante="primario" carregando={salvando}>
            {editando ? 'Salvar alterações' : 'Cadastrar área'}
          </Botao>
        </>
      }
    >
      <form id="formulario-area-comum" onSubmit={salvar} className="flex flex-col gap-7">
        {erro && <CaixaDeErro ref={caixaDoErro}>{erro}</CaixaDeErro>}

        <Secao titulo="Sobre a área">
          <Campo rotulo="Nome" value={formulario.nome} onChange={mudar('nome')} obrigatorio maxLength={100} placeholder="Salão de festas, churrasqueira…" />
          <CampoDeTexto
            rotulo="Descrição"
            value={formulario.descricao}
            onChange={mudar('descricao')}
            placeholder="Onde fica e o que tem: cozinha, banheiros, mesas…"
          />
          {editando && (
            <CaixaDeMarcar
              rotulo="Disponível para reservas"
              ajuda="Desmarcada, a área some do pedido de reserva dos moradores. As reservas já feitas continuam."
              checked={formulario.ativa}
              onChange={mudar('ativa')}
            />
          )}
        </Secao>

        <Secao titulo="Regras de uso">
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo
              rotulo="Capacidade máxima"
              type="number"
              inputMode="numeric"
              min={1}
              value={formulario.capacidadeMaxima}
              onChange={mudar('capacidadeMaxima')}
              ajuda="Em pessoas. Deixe em branco se não houver limite."
            />
            <Campo
              rotulo="Taxa de uso (R$)"
              type="number"
              inputMode="decimal"
              min={0}
              step="0.01"
              value={formulario.taxaValor}
              onChange={mudar('taxaValor')}
              ajuda="Deixe em branco se o uso for gratuito."
            />
            <Campo
              rotulo="Antecedência mínima (dias)"
              type="number"
              inputMode="numeric"
              min={0}
              value={formulario.diasAntecedenciaMin}
              onChange={mudar('diasAntecedenciaMin')}
              ajuda="Com 0, dá para reservar para o mesmo dia."
            />
            <Campo
              rotulo="Antecedência máxima (dias)"
              type="number"
              inputMode="numeric"
              min={1}
              value={formulario.diasAntecedenciaMax}
              onChange={mudar('diasAntecedenciaMax')}
              ajuda="Até quantos dias à frente o morador pode reservar."
            />
          </div>
          <div className="grid items-start gap-4 sm:grid-cols-2">
            <CaixaDeMarcar rotulo="Permite convidados de fora" checked={formulario.permiteConvidados} onChange={mudar('permiteConvidados')} />
            {formulario.permiteConvidados && (
              <Campo
                rotulo="Limite de convidados"
                type="number"
                inputMode="numeric"
                min={0}
                value={formulario.limiteConvidados}
                onChange={mudar('limiteConvidados')}
                ajuda="Deixe em branco se não houver limite."
              />
            )}
          </div>
        </Secao>

        <Secao
          titulo="Turnos"
          ajuda="Os horários que o morador pode escolher. Ele também pode reservar o dia inteiro, o que ocupa todos os turnos da data. Sem turnos, a reserva é sempre do dia inteiro."
        >
          {turnos.length > 0 && (
            <ul className="m-0 flex list-none flex-col gap-3 p-0">
              {turnos.map((turno, indice) => (
                <li
                  key={turno.chave}
                  className="grid gap-3 rounded-xl border border-borda-suave bg-cabecalho p-3 sm:grid-cols-[minmax(0,1fr)_112px_112px_auto] sm:items-end"
                >
                  <Campo
                    rotulo={`Nome do turno ${indice + 1}`}
                    value={turno.nome}
                    onChange={(e) => mudarTurno(turno.chave, 'nome', e.target.value)}
                    obrigatorio
                    maxLength={50}
                    placeholder="Manhã, tarde, noite…"
                  />
                  <div className="grid grid-cols-2 gap-3 sm:contents">
                    <Campo
                      rotulo="Início"
                      type="time"
                      value={turno.horaInicio}
                      onChange={(e) => mudarTurno(turno.chave, 'horaInicio', e.target.value)}
                      obrigatorio
                    />
                    <Campo rotulo="Fim" type="time" value={turno.horaFim} onChange={(e) => mudarTurno(turno.chave, 'horaFim', e.target.value)} obrigatorio />
                  </div>
                  <div className="flex items-center justify-between gap-2 sm:justify-end">
                    {turno.codigo != null && (
                      <CaixaDeMarcar
                        rotulo="Em uso"
                        checked={turno.ativo}
                        onChange={(e) => mudarTurno(turno.chave, 'ativo', e.target.checked)}
                        title="Desmarcado, o turno não aparece mais para quem vai reservar"
                      />
                    )}
                    <button
                      type="button"
                      aria-label={`Remover o turno ${turno.nome || indice + 1}`}
                      onClick={() => removerTurno(turno.chave)}
                      className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-perigo"
                    >
                      <Trash2 size={17} aria-hidden />
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
          {editando && turnos.some((t) => t.codigo != null) && (
            <p className="-mt-1 text-xs text-apagado">
              Turno que já tem reservas não pode ser removido. Para tirá-lo das próximas reservas, desmarque “Em uso”.
            </p>
          )}
          <div>
            <Botao pequeno onClick={adicionarTurno} className="max-lg:h-11">
              <Plus size={15} aria-hidden />
              Adicionar turno
            </Botao>
          </div>
        </Secao>

        <Secao titulo="Termos de uso" ajuda="O morador precisa aceitar estes termos ao pedir a reserva.">
          <CampoDeTexto
            rotulo="Texto dos termos"
            value={formulario.termosUso}
            onChange={mudar('termosUso')}
            rows={5}
            placeholder="Horário de silêncio, limpeza depois do uso, responsabilidade por danos…"
          />
        </Secao>
      </form>
    </Modal>
  );
}
