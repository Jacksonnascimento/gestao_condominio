'use client';

import { Copy } from 'lucide-react';
import toast from 'react-hot-toast';
import { SeloDoBoleto } from '@/components/financeiro/SeloDoBoleto';
import { Botao, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { nomeDaUnidade, type Boleto } from '@/services/financeiroService';
import { formatarData, formatarMoeda } from '@/services/utilitarios';

/** Código para pagar (linha digitável ou Pix), com o botão de copiar. */
function CodigoParaCopiar({ rotulo, codigo, aviso }: { rotulo: string; codigo: string; aviso: string }) {
  async function copiar() {
    try {
      await navigator.clipboard.writeText(codigo);
      toast.success(aviso);
    } catch {
      toast.error('Não foi possível copiar. Selecione o código e copie manualmente.');
    }
  }
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-[13px] font-semibold text-tinta-2">{rotulo}</span>
      <div className="flex items-center gap-2 rounded-[10px] border border-borda bg-cabecalho py-1.5 pr-1.5 pl-3">
        <code className="min-w-0 grow font-mono text-[13px] break-all select-all">{codigo}</code>
        <Botao pequeno onClick={copiar} aria-label={`Copiar ${rotulo.toLowerCase()}`} className="max-lg:h-11">
          <Copy size={15} aria-hidden />
          Copiar
        </Botao>
      </div>
    </div>
  );
}

/** O boleto por inteiro: valor, vencimento e os códigos para pagar. */
export function DetalhesDoBoleto({ boleto, aoFechar }: { boleto: Boleto; aoFechar: () => void }) {
  const pago = boleto.status === 'PAGO';
  return (
    <Modal
      titulo={boleto.nomeTaxa}
      subtitulo={nomeDaUnidade(boleto.unidadeNome)}
      aoFechar={aoFechar}
      rodape={
        <Botao variante="secundario" onClick={aoFechar}>
          Fechar
        </Botao>
      }
    >
      <div className="flex flex-col gap-5">
        <dl className="m-0 divide-y divide-borda-suave">
          <LinhaDeDetalhe rotulo="Situação">
            <SeloDoBoleto boleto={boleto} />
          </LinhaDeDetalhe>
          <LinhaDeDetalhe rotulo="Valor">
            <span className="font-bold tabular-nums">{formatarMoeda(boleto.valor)}</span>
          </LinhaDeDetalhe>
          <LinhaDeDetalhe rotulo="Vencimento">{formatarData(boleto.dataVencimento)}</LinhaDeDetalhe>
        </dl>
        {!pago && boleto.linhaDigitavel && (
          <CodigoParaCopiar rotulo="Linha digitável" codigo={boleto.linhaDigitavel} aviso="Linha digitável copiada." />
        )}
        {!pago && boleto.codigoPix && (
          <CodigoParaCopiar rotulo="Pix copia e cola" codigo={boleto.codigoPix} aviso="Código Pix copiado." />
        )}
        {boleto.linkPdf && (
          <a href={boleto.linkPdf} target="_blank" rel="noreferrer" className="text-sm font-bold underline underline-offset-2">
            Abrir o boleto para imprimir
          </a>
        )}
        <p className="rounded-xl bg-aviso-fundo px-4 py-3 text-[13px] text-aviso">
          Estes códigos são de demonstração e não servem para pagamento.
        </p>
      </div>
    </Modal>
  );
}
