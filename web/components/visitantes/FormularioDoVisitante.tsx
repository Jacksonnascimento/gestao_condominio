'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, Campo, CampoDeSelecao, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import { mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';
import {
  visitanteService,
  type MoradorDaUnidade,
  type UnidadeDoVisitante,
  type VisitanteDetalhe,
} from '@/services/visitanteService';

/** 12345678901 vira 123.456.789-01, conforme a pessoa digita. */
function mascaraCpf(texto: string): string {
  const d = texto.replace(/\D/g, '').slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
}

/** 77999991234 vira (77) 99999-1234; com 10 dígitos, (77) 3421-1234. */
function mascaraTelefone(texto: string): string {
  const d = texto.replace(/\D/g, '').slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : '';
  const ddd = d.slice(0, 2);
  const resto = d.slice(2);
  const corte = resto.length > 8 ? 5 : 4;
  return resto.length > corte ? `(${ddd}) ${resto.slice(0, corte)}-${resto.slice(corte)}` : `(${ddd}) ${resto}`;
}

/**
 * Entrada de um visitante na portaria, ou a correção dos dados de um registro. A data e a hora da entrada são as do
 * momento do registro; o morador que autorizou precisa ser ocupante da unidade visitada.
 */
export function FormularioDoVisitante({
  unidades,
  visitante,
  aoFechar,
  aoSalvar,
}: {
  unidades: UnidadeDoVisitante[];
  /** Com o visitante, a janela edita o registro; sem ele, registra uma entrada. */
  visitante?: VisitanteDetalhe;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const { condominio } = useSessao();
  const editando = Boolean(visitante);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [formulario, setFormulario] = useState({
    nome: visitante?.nome ?? '',
    unidadeId: visitante ? String(visitante.unidadeId) : '',
    moradorId: visitante?.moradorId ? String(visitante.moradorId) : '',
    cpf: visitante?.cpf ?? '',
    rg: visitante?.rg ?? '',
    telefone: visitante?.telefone ?? '',
    observacoes: visitante?.observacoes ?? '',
  });
  // Moradores da unidade escolhida; guardados com a unidade para não mostrar os de outra enquanto carregam
  const [moradores, setMoradores] = useState<{ unidadeId: string; lista: MoradorDaUnidade[] } | null>(null);

  useEffect(() => {
    const unidadeId = formulario.unidadeId;
    if (!unidadeId) return;
    let ativa = true;
    visitanteService
      .moradores(Number(unidadeId))
      .then((lista) => {
        if (ativa) setMoradores({ unidadeId, lista });
      })
      .catch((e) => {
        if (!ativa) return;
        setMoradores({ unidadeId, lista: [] });
        setErro(mensagemErroApi(e, 'Não foi possível carregar os moradores da unidade.'));
      });
    return () => {
      ativa = false;
    };
  }, [formulario.unidadeId]);

  const mudar = (campo: keyof typeof formulario) => (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setFormulario((atual) => ({ ...atual, [campo]: evento.target.value }));

  const listaDeMoradores = formulario.unidadeId && moradores?.unidadeId === formulario.unidadeId ? [...moradores.lista] : null;
  // Na edição, quem autorizou continua aceito mesmo que tenha deixado a unidade depois
  if (
    listaDeMoradores &&
    visitante?.moradorId &&
    String(visitante.unidadeId) === formulario.unidadeId &&
    !listaDeMoradores.some((m) => m.id === visitante.moradorId)
  ) {
    listaDeMoradores.unshift({ id: visitante.moradorId, nome: visitante.moradorNome ?? 'Morador anterior', vinculo: null });
  }

  // Na edição, a unidade do registro aparece mesmo que tenha sido desativada depois
  const opcoesDeUnidade =
    visitante && !unidades.some((u) => u.id === visitante.unidadeId)
      ? [
          { id: visitante.unidadeId, numero: visitante.unidadeNumero ?? '', bloco: visitante.unidadeBloco, condominioId: visitante.condominioId },
          ...unidades,
        ]
      : unidades;

  let textoSemMorador = 'Não informado';
  if (!formulario.unidadeId) textoSemMorador = 'Escolha a unidade primeiro';
  else if (!listaDeMoradores) textoSemMorador = 'Carregando…';
  else if (listaDeMoradores.length === 0) textoSemMorador = 'Nenhum morador cadastrado nesta unidade';

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    const pedido = {
      nome: formulario.nome.trim(),
      unidadeId: Number(formulario.unidadeId),
      moradorId: formulario.moradorId ? Number(formulario.moradorId) : null,
      cpf: formulario.cpf.trim() || undefined,
      rg: formulario.rg.trim() || undefined,
      telefone: formulario.telefone.trim() || undefined,
      observacoes: formulario.observacoes.trim() || undefined,
    };
    try {
      if (visitante) {
        await visitanteService.editar(visitante.id, pedido);
        toast.success('Dados do visitante atualizados.');
      } else {
        await visitanteService.registrarEntrada(pedido);
        toast.success(`Entrada de ${pedido.nome} registrada.`);
      }
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, editando ? 'Não foi possível salvar as alterações.' : 'Não foi possível registrar a entrada.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo={editando ? 'Editar visitante' : 'Registrar entrada'}
      subtitulo={editando ? visitante?.nome : condominio?.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="formulario-visitante" variante="primario" carregando={salvando}>
            {editando ? 'Salvar' : 'Registrar entrada'}
          </Botao>
        </>
      }
    >
      <form id="formulario-visitante" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo sm:col-span-2" role="alert">
            {erro}
          </p>
        )}
        <Campo
          rotulo="Nome do visitante"
          value={formulario.nome}
          onChange={mudar('nome')}
          obrigatorio
          maxLength={100}
          autoComplete="off"
          className="sm:col-span-2"
        />
        <CampoDeSelecao
          rotulo="Unidade visitada"
          value={formulario.unidadeId}
          onChange={(e) => setFormulario((atual) => ({ ...atual, unidadeId: e.target.value, moradorId: '' }))}
          obrigatorio
        >
          <option value="">{opcoesDeUnidade.length ? 'Escolha a unidade' : 'Nenhuma unidade disponível'}</option>
          {opcoesDeUnidade.map((u) => (
            <option key={u.id} value={u.id}>
              {rotuloUnidade(u.numero, u.bloco)}
            </option>
          ))}
        </CampoDeSelecao>
        <CampoDeSelecao
          rotulo="Quem autorizou"
          value={formulario.moradorId}
          onChange={mudar('moradorId')}
          disabled={!listaDeMoradores || listaDeMoradores.length === 0}
        >
          <option value="">{textoSemMorador}</option>
          {listaDeMoradores?.map((m) => (
            <option key={m.id} value={m.id}>
              {m.vinculo ? `${m.nome} (${m.vinculo})` : m.nome}
            </option>
          ))}
        </CampoDeSelecao>
        <div className="grid gap-4 sm:col-span-2 sm:grid-cols-3">
          <Campo
            rotulo="CPF"
            value={formulario.cpf}
            onChange={(e) => setFormulario((atual) => ({ ...atual, cpf: mascaraCpf(e.target.value) }))}
            inputMode="numeric"
            placeholder="000.000.000-00"
            maxLength={14}
            autoComplete="off"
          />
          <Campo rotulo="RG" value={formulario.rg} onChange={mudar('rg')} maxLength={20} autoComplete="off" />
          <Campo
            rotulo="Telefone"
            type="tel"
            value={formulario.telefone}
            onChange={(e) => setFormulario((atual) => ({ ...atual, telefone: mascaraTelefone(e.target.value) }))}
            placeholder="(00) 00000-0000"
            maxLength={20}
            autoComplete="off"
          />
        </div>
        <CampoDeTexto
          rotulo="Observações"
          value={formulario.observacoes}
          onChange={mudar('observacoes')}
          placeholder="Empresa, placa do carro, motivo da visita…"
          className="sm:col-span-2"
        />
        {!editando && <p className="text-xs text-apagado sm:col-span-2">A entrada fica registrada com a data e a hora de agora.</p>}
      </form>
    </Modal>
  );
}
