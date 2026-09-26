'use client';

import { SeloDoContrato, situacaoDoContrato, textoDaVigencia, textoDoVencimento } from '@/components/contratos/SeloDoContrato';
import { Botao, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import type { Contrato } from '@/services/contratoService';
import { formatarDataHora, formatarMoeda } from '@/services/utilitarios';

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
          <LinhaDeDetalhe rotulo="Serviço" valor={contrato.servico} />
          <LinhaDeDetalhe rotulo="Valor" valor={formatarMoeda(contrato.valor)} />
          <LinhaDeDetalhe rotulo="Vigência" valor={textoDaVigencia(contrato)} />
          <LinhaDeDetalhe rotulo="Responsável" valor={contrato.responsavel} />
          <LinhaDeDetalhe rotulo="Observações" valor={contrato.observacoes} />
          <LinhaDeDetalhe rotulo="Cadastrado em" valor={formatarDataHora(contrato.dataCadastro)} />
          <LinhaDeDetalhe rotulo="Última alteração" valor={formatarDataHora(contrato.dataAtualizacao)} />
        </dl>
      </div>
    </Modal>
  );
}
