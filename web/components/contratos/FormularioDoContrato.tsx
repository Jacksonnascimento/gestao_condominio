'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { situacaoDoContrato } from '@/components/contratos/SeloDoContrato';
import { Botao, Campo, CampoDeSelecao, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import { contratoService, type Contrato } from '@/services/contratoService';
import { hojeParaCampo, mensagemErroApi } from '@/services/utilitarios';

/**
 * Cadastro ou edição de um contrato com um prestador de serviço. A situação (ativo, vencendo ou vencido) é
 * calculada pela data de fim; só a rescisão é escolhida aqui.
 */
export function FormularioDoContrato({
  contrato,
  aoFechar,
  aoSalvar,
}: {
  /** Com o contrato, a janela edita; sem ele, cadastra no condomínio escolhido. */
  contrato?: Contrato;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const { condominio } = useSessao();
  const editando = Boolean(contrato);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [formulario, setFormulario] = useState({
    empresa: contrato?.empresa ?? '',
    servico: contrato?.servico ?? '',
    valor: contrato ? String(contrato.valor) : '',
    responsavel: contrato?.responsavel ?? '',
    dataInicio: contrato?.dataInicio ?? hojeParaCampo(),
    dataFim: contrato?.dataFim ?? '',
    rescindido: contrato ? situacaoDoContrato(contrato) === 'RESCINDIDO' : false,
    observacoes: contrato?.observacoes ?? '',
  });

  const mudar = (campo: keyof typeof formulario) => (evento: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setFormulario((atual) => ({ ...atual, [campo]: evento.target.value }));

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!contrato && !condominio) return;
    setErro('');
    if (formulario.dataFim < formulario.dataInicio) {
      setErro('A data de fim não pode ser anterior à data de início.');
      return;
    }
    setSalvando(true);
    const pedido = {
      empresa: formulario.empresa.trim(),
      servico: formulario.servico.trim(),
      valor: Number(formulario.valor.replace(',', '.')),
      responsavel: formulario.responsavel.trim() || null,
      status: formulario.rescindido ? ('RESCINDIDO' as const) : null,
      dataInicio: formulario.dataInicio,
      dataFim: formulario.dataFim,
      observacoes: formulario.observacoes.trim() || null,
    };
    try {
      if (contrato) {
        await contratoService.atualizar(contrato.id, pedido);
        toast.success('Contrato atualizado.');
      } else {
        await contratoService.criar({ ...pedido, condominioId: condominio?.id });
        toast.success('Contrato cadastrado.');
      }
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, editando ? 'Não foi possível salvar o contrato.' : 'Não foi possível cadastrar o contrato.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo={editando ? 'Editar contrato' : 'Novo contrato'}
      subtitulo={editando ? contrato?.empresa : condominio?.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="formulario-contrato" variante="primario" carregando={salvando}>
            {editando ? 'Salvar' : 'Cadastrar'}
          </Botao>
        </>
      }
    >
      <form id="formulario-contrato" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo sm:col-span-2" role="alert">
            {erro}
          </p>
        )}
        <Campo rotulo="Empresa" value={formulario.empresa} onChange={mudar('empresa')} obrigatorio maxLength={100} />
        <Campo
          rotulo="Serviço"
          value={formulario.servico}
          onChange={mudar('servico')}
          obrigatorio
          maxLength={255}
          placeholder="Limpeza, portaria, elevadores…"
        />
        <Campo
          rotulo="Valor (R$)"
          type="number"
          inputMode="decimal"
          min={0}
          max={99999999.99}
          step={0.01}
          value={formulario.valor}
          onChange={mudar('valor')}
          obrigatorio
          placeholder="0,00"
        />
        <Campo
          rotulo="Responsável na empresa"
          value={formulario.responsavel}
          onChange={mudar('responsavel')}
          maxLength={100}
        />
        <Campo rotulo="Início" type="date" value={formulario.dataInicio} onChange={mudar('dataInicio')} obrigatorio />
        <Campo
          rotulo="Fim"
          type="date"
          value={formulario.dataFim}
          onChange={mudar('dataFim')}
          obrigatorio
          min={formulario.dataInicio || undefined}
          ajuda="Faltando 30 dias para o fim, o contrato aparece como vencendo."
        />
        <CampoDeSelecao
          rotulo="Situação"
          value={formulario.rescindido ? 'RESCINDIDO' : ''}
          onChange={(e) => setFormulario((atual) => ({ ...atual, rescindido: e.target.value === 'RESCINDIDO' }))}
          ajuda={formulario.rescindido ? 'O contrato vai para o histórico.' : 'Ativo, vencendo ou vencido, conforme a data de fim.'}
          className="sm:col-span-2"
        >
          <option value="">Em vigor pelas datas</option>
          <option value="RESCINDIDO">Rescindido</option>
        </CampoDeSelecao>
        <CampoDeTexto rotulo="Observações" value={formulario.observacoes} onChange={mudar('observacoes')} className="sm:col-span-2" />
      </form>
    </Modal>
  );
}
