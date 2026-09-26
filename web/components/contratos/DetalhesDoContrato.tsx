'use client';

import {
  SeloDoContrato,
  formatarValor,
  situacaoDoContrato,
  textoDaVigencia,
  textoDoVencimento,
} from '@/components/contratos/SeloDoContrato';
import { Botao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import type { Contrato } from '@/services/contratoService';
import { formatarDataHora } from '@/services/utilitarios';

function Linha({ rotulo, valor }: { rotulo: string; valor?: string | null }) {
  if (!valor) return null;
  return (
    <div className="grid grid-cols-[130px_minmax(0,1fr)] gap-3 py-2.5 text-sm">
      <dt className="text-apagado">{rotulo}</dt>
      <dd className="m-0 break-words whitespace-pre-line">{valor}</dd>
    </div>
  );
}

/** Tudo sobre o contrato e, para a gestão, editar, rescindir e excluir. */
export function DetalhesDoContrato({
  contrato,
  podeGerenciar,
  aoFechar,
  aoEditar,
  aoRescindir,
  aoExcluir,
}: {
  contrato: Contrato;
  podeGerenciar: boolean;
  aoFechar: () => void;
  aoEditar: (contrato: Contrato) => void;
  aoRescindir: (contrato: Contrato) => void;
  aoExcluir: (contrato: Contrato) => void;
}) {
  const situacao = situacaoDoContrato(contrato);
  const vencimento = textoDoVencimento(contrato);
  const podeRescindir = situacao === 'ATIVO' || situacao === 'A_VENCER';

  return (
    <Modal
      titulo={contrato.empresa}
      subtitulo={contrato.servico}
      aoFechar={aoFechar}
      rodape={
        podeGerenciar ? (
          <>
            <Botao variante="texto" className="mr-auto text-perigo" onClick={() => aoExcluir(contrato)}>
              Excluir
            </Botao>
            {podeRescindir && <Botao onClick={() => aoRescindir(contrato)}>Rescindir</Botao>}
            <Botao variante="primario" onClick={() => aoEditar(contrato)}>
              Editar
            </Botao>
          </>
        ) : (
          <Botao variante="secundario" onClick={aoFechar}>
            Fechar
          </Botao>
        )
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-2">
          <SeloDoContrato contrato={contrato} />
          {vencimento && <span className="text-sm font-semibold text-aviso">{vencimento.charAt(0).toUpperCase() + vencimento.slice(1)}</span>}
        </div>
        <dl className="m-0 divide-y divide-borda-suave">
          <Linha rotulo="Serviço" valor={contrato.servico} />
          <Linha rotulo="Valor" valor={formatarValor(contrato.valor)} />
          <Linha rotulo="Vigência" valor={textoDaVigencia(contrato)} />
          <Linha rotulo="Responsável" valor={contrato.responsavel} />
          <Linha rotulo="Observações" valor={contrato.observacoes} />
          <Linha rotulo="Cadastrado em" valor={formatarDataHora(contrato.dataCadastro)} />
          <Linha rotulo="Última alteração" valor={formatarDataHora(contrato.dataAtualizacao)} />
        </dl>
      </div>
    </Modal>
  );
}
