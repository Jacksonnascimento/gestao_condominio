'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import { unidadeInativaDoErro, unidadeService, type OpcoesUnidade, type Unidade } from '@/services/unidadeService';
import { emFrase, mensagemErroApi, numeroOuNulo, rotuloUnidade } from '@/services/utilitarios';

/**
 * Cadastro e edição de unidade. Se o cadastro repete uma unidade que já existiu e foi inativada, a janela oferece
 * reativar o cadastro antigo em vez de criar outro.
 */
export function FormularioDaUnidade({
  unidade,
  opcoes,
  aoFechar,
  aoSalvar,
}: {
  /** Sem unidade, é cadastro. */
  unidade?: Unidade;
  opcoes: OpcoesUnidade;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const { condominio } = useSessao();
  const editando = !!unidade;
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [unidadeInativa, setUnidadeInativa] = useState<number | null>(null);
  const [formulario, setFormulario] = useState({
    numero: unidade?.numero ?? '',
    bloco: unidade?.bloco ?? '',
    andar: unidade?.andar ?? '',
    tipo: unidade?.tipo ?? opcoes.tipos[0]?.valor ?? '',
    statusOcupacao: unidade?.statusOcupacao ?? 'VAZIA',
    fracaoIdeal: unidade?.fracaoIdeal != null ? String(unidade.fracaoIdeal) : '',
    areaPrivada: unidade?.areaPrivada != null ? String(unidade.areaPrivada) : '',
    observacao: unidade?.observacao ?? '',
  });

  const mudar = (campo: keyof typeof formulario) => (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setFormulario((atual) => ({ ...atual, [campo]: evento.target.value }));
    setUnidadeInativa(null);
  };

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setUnidadeInativa(null);
    setSalvando(true);
    const dados = {
      condominioId: condominio?.id,
      numero: formulario.numero.trim(),
      bloco: formulario.bloco.trim() || null,
      andar: formulario.andar.trim() || null,
      tipo: formulario.tipo,
      statusOcupacao: formulario.statusOcupacao,
      fracaoIdeal: numeroOuNulo(formulario.fracaoIdeal),
      areaPrivada: numeroOuNulo(formulario.areaPrivada),
      observacao: formulario.observacao.trim() || null,
    };
    try {
      if (unidade) {
        await unidadeService.atualizar(unidade.id, dados);
        toast.success('Unidade atualizada.');
      } else {
        await unidadeService.cadastrar(dados);
        toast.success('Unidade cadastrada.');
      }
      aoSalvar();
    } catch (e) {
      const inativa = editando ? null : unidadeInativaDoErro(e);
      if (inativa != null) {
        setUnidadeInativa(inativa);
      } else {
        setErro(mensagemErroApi(e, 'Não foi possível salvar a unidade.'));
      }
      setSalvando(false);
    }
  }

  async function reativarAntiga() {
    if (unidadeInativa == null) return;
    setErro('');
    setSalvando(true);
    try {
      await unidadeService.reativar(unidadeInativa);
      toast.success('Unidade reativada.');
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível reativar a unidade.'));
      setSalvando(false);
    }
  }

  const rotulo = rotuloUnidade(formulario.numero.trim(), formulario.bloco.trim());

  return (
    <Modal
      titulo={editando ? 'Editar unidade' : 'Nova unidade'}
      subtitulo={editando ? rotuloUnidade(unidade.numero, unidade.bloco) : condominio?.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="formulario-unidade" variante="primario" carregando={salvando}>
            {editando ? 'Salvar' : 'Cadastrar'}
          </Botao>
        </>
      }
    >
      <form id="formulario-unidade" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && <CaixaDeErro className="sm:col-span-2">{erro}</CaixaDeErro>}
        {unidadeInativa != null && (
          <div className="flex flex-col gap-3 rounded-xl bg-aviso-fundo px-4 py-3 text-sm text-aviso sm:col-span-2" role="alert">
            <p>
              A unidade {rotulo || 'informada'} já existiu neste condomínio e foi inativada. Em vez de criar outra, reative o
              cadastro antigo; depois é só conferir os dados dele.
            </p>
            <div>
              <Botao variante="secundario" pequeno onClick={reativarAntiga} disabled={salvando}>
                Reativar a unidade {rotulo}
              </Botao>
            </div>
          </div>
        )}
        <Campo rotulo="Número" value={formulario.numero} onChange={mudar('numero')} obrigatorio maxLength={10} placeholder="101" />
        <Campo rotulo="Bloco" value={formulario.bloco} onChange={mudar('bloco')} maxLength={50} placeholder="Bloco A" />
        <Campo rotulo="Andar" value={formulario.andar} onChange={mudar('andar')} maxLength={50} placeholder="1" />
        <CampoDeSelecao rotulo="Tipo" value={formulario.tipo} onChange={mudar('tipo')} obrigatorio>
          {opcoes.tipos.map((t) => (
            <option key={t.valor} value={t.valor}>
              {emFrase(t.descricao)}
            </option>
          ))}
        </CampoDeSelecao>
        <CampoDeSelecao rotulo="Situação de ocupação" value={formulario.statusOcupacao} onChange={mudar('statusOcupacao')} obrigatorio className="sm:col-span-2">
          {opcoes.statusOcupacao.map((s) => (
            <option key={s.valor} value={s.valor}>
              {emFrase(s.descricao)}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo
          rotulo="Área privada (m²)"
          type="number"
          inputMode="decimal"
          min={0}
          step="0.01"
          value={formulario.areaPrivada}
          onChange={mudar('areaPrivada')}
        />
        <Campo
          rotulo="Fração ideal (%)"
          type="number"
          inputMode="decimal"
          min={0}
          max={100}
          step="0.01"
          value={formulario.fracaoIdeal}
          onChange={mudar('fracaoIdeal')}
          ajuda="Parte da unidade no rateio das despesas, de 0 a 100."
        />
        <CampoDeTexto rotulo="Observações" value={formulario.observacao} onChange={mudar('observacao')} className="sm:col-span-2" />
      </form>
    </Modal>
  );
}
