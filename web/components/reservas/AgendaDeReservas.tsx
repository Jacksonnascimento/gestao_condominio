'use client';

import { useEffect, useState } from 'react';
import { addMonths, endOfMonth, format, isToday, parseISO, startOfMonth } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { SeloDaReserva, turnoDaReserva } from '@/components/reservas/SeloDaReserva';
import { Botao, Cartao, Vazio } from '@/components/Interface';
import { reservaService, type FiltroReservas, type Reserva } from '@/services/reservaService';
import { diaDaSemanaCurto, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

// O máximo que a API devolve numa página; um mês de reservas de um condomínio cabe com folga
const LIMITE_DA_API = 100;

/** Reservas do dia inteiro primeiro, depois pelos horários dos turnos. */
function ordemNoDia(a: Reserva, b: Reserva): number {
  return (a.turnoHoraInicio ?? '').localeCompare(b.turnoHoraInicio ?? '');
}

function mesPorExtenso(mes: Date): string {
  const texto = format(mes, "MMMM 'de' yyyy", { locale: ptBR });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/**
 * As reservas de um mês, dia a dia, com os mesmos filtros da lista (situação, área e busca). Cada reserva abre os
 * detalhes, onde ficam as ações.
 */
export function AgendaDeReservas({
  filtro,
  versao,
  aoAbrir,
}: {
  filtro: Omit<FiltroReservas, 'dataInicio' | 'dataFim' | 'pagina' | 'tamanho'>;
  /** Sobe quando algo muda fora da agenda, para ela ser lida de novo. */
  versao: number;
  aoAbrir: (reserva: Reserva) => void;
}) {
  const [mes, setMes] = useState(() => startOfMonth(new Date()));
  const [reservas, setReservas] = useState<Reserva[]>([]);
  const [total, setTotal] = useState(0);
  const [consultaCarregada, setConsultaCarregada] = useState('');

  const dataInicio = format(mes, 'yyyy-MM-dd');
  const dataFim = format(endOfMonth(mes), 'yyyy-MM-dd');
  const { condominioId, status, areaId, busca } = filtro;
  const consulta = JSON.stringify({ condominioId, status, areaId, busca, dataInicio, versao });
  const carregando = consulta !== consultaCarregada;

  useEffect(() => {
    let ativa = true;
    reservaService
      .listar({ condominioId, status, areaId, busca, dataInicio, dataFim, pagina: 0, tamanho: LIMITE_DA_API })
      .then((pagina) => {
        if (!ativa) return;
        setReservas(pagina.itens);
        setTotal(pagina.totalItens);
      })
      .catch((e) => {
        if (ativa) toast.error(mensagemErroApi(e, 'Não foi possível carregar a agenda de reservas.'));
      })
      .finally(() => {
        if (ativa) setConsultaCarregada(consulta);
      });
    return () => {
      ativa = false;
    };
  }, [consulta, condominioId, status, areaId, busca, dataInicio, dataFim]);

  const dias = new Map<string, Reserva[]>();
  [...reservas]
    .sort((a, b) => a.data.localeCompare(b.data) || ordemNoDia(a, b))
    .forEach((r) => dias.set(r.data, [...(dias.get(r.data) ?? []), r]));

  const botaoDoMes =
    'flex size-11 cursor-pointer items-center justify-center rounded-lg text-tinta hover:bg-trilho lg:size-[34px]';
  const esteMes = format(mes, 'yyyy-MM') === format(new Date(), 'yyyy-MM');

  return (
    <Cartao aria-label="Agenda de reservas" aria-busy={carregando}>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-borda-suave bg-cabecalho px-5 py-2.5">
        <div className="flex items-center gap-1">
          <button type="button" className={botaoDoMes} aria-label="Mês anterior" onClick={() => setMes((m) => addMonths(m, -1))}>
            <ChevronLeft size={18} aria-hidden />
          </button>
          <h2 className="min-w-[170px] text-center text-base font-extrabold" aria-live="polite">
            {mesPorExtenso(mes)}
          </h2>
          <button type="button" className={botaoDoMes} aria-label="Próximo mês" onClick={() => setMes((m) => addMonths(m, 1))}>
            <ChevronRight size={18} aria-hidden />
          </button>
        </div>
        {!esteMes && (
          <Botao pequeno variante="texto" onClick={() => setMes(startOfMonth(new Date()))} className="max-lg:h-11">
            Voltar para este mês
          </Botao>
        )}
      </div>

      {!carregando && dias.size === 0 && <Vazio>Nenhuma reserva em {mesPorExtenso(mes).toLowerCase()} com esses filtros.</Vazio>}

      <ol className="m-0 list-none p-0">
        {[...dias.entries()].map(([data, doDia]) => {
          const dia = parseISO(data);
          const hoje = isToday(dia);
          return (
            <li key={data} className="grid grid-cols-[56px_minmax(0,1fr)] gap-4 border-b border-borda-suave px-5 py-3.5 last:border-b-0">
              <div className={`flex flex-col items-center self-start rounded-xl py-1.5 ${hoje ? 'bg-tinta text-fundo' : 'bg-trilho'}`}>
                <span className={`text-[11px] font-bold tracking-[0.04em] ${hoje ? 'text-ouro-claro' : 'text-apagado'}`}>{diaDaSemanaCurto(dia).slice(0, 3)}</span>
                <span className="font-titulo text-[26px] leading-none">{format(dia, 'd')}</span>
              </div>
              <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
                {doDia.map((reserva) => (
                  <li key={reserva.codigo}>
                    <button
                      type="button"
                      onClick={() => aoAbrir(reserva)}
                      className="grid w-full cursor-pointer grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-0.5 rounded-xl px-3 py-2 text-left text-sm hover:bg-lateral sm:grid-cols-[150px_minmax(0,1fr)_auto]"
                    >
                      <span className="font-bold tabular-nums">{turnoDaReserva(reserva)}</span>
                      <span className="justify-self-end sm:order-last">
                        <SeloDaReserva reserva={reserva} />
                      </span>
                      <span className="col-span-2 min-w-0 text-tinta-2 sm:col-span-1">
                        <span className="font-semibold text-tinta">{reserva.areaNome}</span>
                        {' · '}
                        {rotuloUnidade(reserva.unidadeNumero, reserva.unidadeBloco)}
                        {reserva.solicitanteNome && <span className="text-apagado"> · {reserva.solicitanteNome}</span>}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </li>
          );
        })}
      </ol>

      {total > reservas.length && (
        <p className="border-t border-borda-suave px-5 py-3 text-[13px] text-apagado">
          Mostrando {reservas.length} de {total} reservas do mês. Use os filtros de situação ou de área para ver as demais.
        </p>
      )}
    </Cartao>
  );
}
