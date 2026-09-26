'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { financeiroService, nomeDaUnidade, type UnidadePagadora } from '@/services/financeiroService';
import { hojeParaCampo, lerValorEmReais, mensagemErroApi } from '@/services/utilitarios';

/** Cobrança fora da taxa do mês (multa, reserva, conserto...) para uma unidade. */
export function GerarBoletoAvulso({
  unidades,
  condominioNome,
  aoFechar,
  aoGerar,
}: {
  unidades: UnidadePagadora[];
  condominioNome?: string;
  aoFechar: () => void;
  aoGerar: () => void;
}) {
  const [unidadeId, setUnidadeId] = useState('');
  const [nomeTaxa, setNomeTaxa] = useState('');
  const [valor, setValor] = useState('');
  const [dataVencimento, setDataVencimento] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    const valorEmReais = lerValorEmReais(valor);
    if (Number.isNaN(valorEmReais)) {
      setErro('Informe o valor em reais, como 150,00.');
      return;
    }
    setErro('');
    setSalvando(true);
    try {
      await financeiroService.gerarBoleto({
        unidadeId: Number(unidadeId),
        nomeTaxa: nomeTaxa.trim(),
        valor: valorEmReais,
        dataVencimento: dataVencimento || undefined,
      });
      toast.success('Boleto gerado.');
      aoGerar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível gerar o boleto.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo="Gerar boleto avulso"
      subtitulo={condominioNome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="gerar-boleto" variante="primario" carregando={salvando}>
            Gerar boleto
          </Botao>
        </>
      }
    >
      <form id="gerar-boleto" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && <CaixaDeErro className="sm:col-span-2">{erro}</CaixaDeErro>}
        <CampoDeSelecao
          rotulo="Unidade"
          value={unidadeId}
          onChange={(e) => setUnidadeId(e.target.value)}
          obrigatorio
          className="sm:col-span-2"
        >
          <option value="">{unidades.length ? 'Escolha a unidade' : 'Nenhuma unidade ativa'}</option>
          {unidades.map((u) => (
            <option key={u.codigo} value={u.codigo}>
              {nomeDaUnidade(u.descricao)}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo
          rotulo="Descrição"
          value={nomeTaxa}
          onChange={(e) => setNomeTaxa(e.target.value)}
          obrigatorio
          maxLength={255}
          placeholder="Multa, reserva do salão…"
          className="sm:col-span-2"
        />
        <Campo
          rotulo="Valor (R$)"
          inputMode="decimal"
          value={valor}
          onChange={(e) => setValor(e.target.value)}
          obrigatorio
          placeholder="0,00"
          maxLength={14}
        />
        <Campo
          rotulo="Vencimento"
          type="date"
          value={dataVencimento}
          onChange={(e) => setDataVencimento(e.target.value)}
          min={hojeParaCampo()}
          ajuda="Sem data, vence no último dia do mês."
        />
        <p className="rounded-xl bg-aviso-fundo px-4 py-3 text-[13px] text-aviso sm:col-span-2">
          O financeiro ainda é uma demonstração: o boleto aparece para a unidade, mas nada é cobrado de verdade.
        </p>
      </form>
    </Modal>
  );
}
