'use client';

import { useState } from 'react';
import { format, parseISO } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { aprovarReserva, cancelarReserva } from '@/components/reservas/acoesDaReserva';
import { SeloDaReserva, turnoDaReserva } from '@/components/reservas/SeloDaReserva';
import { Botao, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import type { Reserva } from '@/services/reservaService';
import { formatarData, formatarDataHora, rotuloUnidade } from '@/services/utilitarios';

/** "sábado, 10 de outubro de 2026" */
function dataPorExtenso(data: string): string {
  return format(parseISO(data), "EEEE, d 'de' MMMM 'de' yyyy", { locale: ptBR });
}

/**
 * Tudo sobre a reserva, com os convidados. As ações seguem o que a API diz a quem está logado: aprovar ou rejeitar
 * (gestão, reserva pendente) e cancelar (quem pediu, reserva pendente ou aprovada).
 */
export function DetalhesDaReserva({
  reserva,
  aoFechar,
  aoAlterar,
  aoPedirRejeicao,
}: {
  reserva: Reserva;
  aoFechar: () => void;
  aoAlterar: () => void;
  aoPedirRejeicao: () => void;
}) {
  const [ocupado, setOcupado] = useState(false);

  async function executar(acao: (r: Reserva) => Promise<boolean>) {
    setOcupado(true);
    const feito = await acao(reserva);
    setOcupado(false);
    if (feito) aoAlterar();
  }

  const temAcoes = reserva.podeAprovarOuRejeitar || reserva.podeCancelar;

  return (
    <Modal
      titulo={reserva.areaNome}
      subtitulo={`${formatarData(reserva.data)} · ${turnoDaReserva(reserva)}`}
      aoFechar={aoFechar}
      ocupado={ocupado}
      rodape={
        <>
          <Botao variante={temAcoes ? 'texto' : 'secundario'} onClick={aoFechar} disabled={ocupado}>
            Fechar
          </Botao>
          {reserva.podeCancelar && (
            <Botao variante="secundario" onClick={() => executar(cancelarReserva)} disabled={ocupado}>
              Cancelar reserva
            </Botao>
          )}
          {reserva.podeAprovarOuRejeitar && (
            <>
              <Botao variante="secundario" onClick={aoPedirRejeicao} disabled={ocupado}>
                Rejeitar
              </Botao>
              <Botao variante="primario" onClick={() => executar(aprovarReserva)} carregando={ocupado}>
                Aprovar
              </Botao>
            </>
          )}
        </>
      }
    >
      <div className="flex flex-col gap-5">
        <div>
          <SeloDaReserva reserva={reserva} />
        </div>
        {reserva.motivoRejeicao && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo">
            <strong className="font-bold">Motivo da rejeição:</strong> {reserva.motivoRejeicao}
          </p>
        )}
        <dl className="m-0 divide-y divide-borda-suave">
          <LinhaDeDetalhe rotulo="Data" valor={dataPorExtenso(reserva.data)} />
          <LinhaDeDetalhe rotulo="Horário" valor={turnoDaReserva(reserva)} />
          <LinhaDeDetalhe rotulo="Unidade" valor={rotuloUnidade(reserva.unidadeNumero, reserva.unidadeBloco)} />
          <LinhaDeDetalhe rotulo="Pedida por" valor={reserva.solicitanteNome} />
          <LinhaDeDetalhe rotulo="Pedida em" valor={formatarDataHora(reserva.dataRegistro)} />
          <LinhaDeDetalhe rotulo="Condomínio" valor={reserva.condominioNome} />
        </dl>
        <section aria-label="Convidados" className="flex flex-col gap-2">
          <h3 className="text-sm font-extrabold">
            Convidados <span className="font-semibold text-apagado">({reserva.convidados.length})</span>
          </h3>
          {reserva.convidados.length === 0 ? (
            <p className="text-sm text-apagado">Nenhum convidado informado.</p>
          ) : (
            <ul className="m-0 list-none divide-y divide-borda-suave rounded-xl border border-borda-suave p-0">
              {reserva.convidados.map((c) => (
                <li key={c.codigo} className="flex items-center justify-between gap-3 px-4 py-2.5 text-sm">
                  <span className="font-semibold">{c.nome}</span>
                  {c.documento && <span className="text-apagado tabular-nums">{c.documento}</span>}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </Modal>
  );
}
