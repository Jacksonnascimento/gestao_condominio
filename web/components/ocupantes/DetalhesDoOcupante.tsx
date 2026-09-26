'use client';

import { descricaoDoTipoDePeriodo, SeloDoVinculo } from '@/components/ocupantes/SeloDoVinculo';
import { Botao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { LinhaDeDetalhe } from '@/components/unidades/PecasDeCadastro';
import type { Ocupante } from '@/services/ocupanteService';
import { formatarData, rotuloUnidade } from '@/services/utilitarios';

/** Tudo sobre o vínculo da pessoa com a unidade. Para a gestão, também editar e remover. */
export function DetalhesDoOcupante({
  ocupante,
  podeGerenciar,
  aoFechar,
  aoEditar,
  aoRemover,
}: {
  ocupante: Ocupante;
  podeGerenciar: boolean;
  aoFechar: () => void;
  aoEditar: () => void;
  aoRemover: () => void;
}) {
  const unidade = rotuloUnidade(ocupante.unidadeNumero, ocupante.unidadeBloco);
  return (
    <Modal
      titulo={ocupante.nome}
      subtitulo={unidade ? `Unidade ${unidade}` : undefined}
      aoFechar={aoFechar}
      rodape={
        podeGerenciar ? (
          <>
            <Botao variante="texto" onClick={aoRemover} className="sm:mr-auto">
              Remover da unidade
            </Botao>
            <Botao variante="secundario" onClick={aoFechar}>
              Fechar
            </Botao>
            <Botao variante="primario" onClick={aoEditar}>
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
      <div className="flex flex-col gap-5">
        <div>
          <SeloDoVinculo ocupante={ocupante} />
        </div>
        <dl className="m-0 divide-y divide-borda-suave">
          <LinhaDeDetalhe rotulo="Unidade" valor={unidade} />
          <LinhaDeDetalhe rotulo="E-mail" valor={ocupante.email} />
          <LinhaDeDetalhe rotulo="Telefone" valor={ocupante.telefone} />
          <LinhaDeDetalhe rotulo="Ocupa desde" valor={formatarData(ocupante.inicioOcupacao)} />
          <LinhaDeDetalhe rotulo="Até" valor={formatarData(ocupante.fimOcupacao)} />
          <LinhaDeDetalhe rotulo="Período de uso" valor={ocupante.periodoUso} />
          <LinhaDeDetalhe rotulo="Tipo do período" valor={descricaoDoTipoDePeriodo(ocupante.tipoPeriodo, ocupante.tipoPeriodoDescricao)} />
        </dl>
      </div>
    </Modal>
  );
}
