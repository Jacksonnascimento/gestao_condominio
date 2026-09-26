'use client';

import { Clock, Pencil, Power, Trash2 } from 'lucide-react';
import { Botao, Selo } from '@/components/Interface';
import { faixaDeHorario, regrasDaArea, type AreaComum } from '@/services/areaComumService';

/** Uma área por cartão: nome, situação, regras em frases curtas, turnos e as ações de quem gerencia. */
export function CartaoDaAreaComum({
  area,
  ocupada,
  aoEditar,
  aoMudarSituacao,
  aoExcluir,
}: {
  area: AreaComum;
  /** Uma ação desta área está em andamento. */
  ocupada: boolean;
  aoEditar: () => void;
  aoMudarSituacao: () => void;
  aoExcluir: () => void;
}) {
  const turnosEmUso = area.turnos.filter((t) => t.ativo);
  const turnosForaDeUso = area.turnos.filter((t) => !t.ativo);

  return (
    <li className={`flex flex-col rounded-2xl border border-borda ${area.ativa ? 'bg-superficie' : 'bg-lateral'}`}>
      <div className="flex flex-col gap-3 px-5 pt-5 pb-4">
        <div className="flex items-start justify-between gap-3">
          <h2 className={`font-titulo text-[26px] leading-tight font-normal ${area.ativa ? '' : 'text-tinta-2'}`}>{area.nome}</h2>
          <span className="pt-1.5">{area.ativa ? <Selo tom="info">Disponível</Selo> : <Selo tom="neutro">Inativa</Selo>}</span>
        </div>
        {area.descricao && <p className="line-clamp-3 text-sm text-tinta-2">{area.descricao}</p>}
        <ul className="m-0 flex list-none flex-wrap gap-x-4 gap-y-1 p-0 text-[13px] text-apagado">
          {regrasDaArea(area).map((regra) => (
            <li key={regra}>{regra}</li>
          ))}
        </ul>
      </div>

      <div className="mt-auto flex flex-col gap-2 border-t border-borda-suave px-5 py-3.5">
        <span className="text-xs font-bold tracking-[0.04em] text-apagado uppercase">Turnos</span>
        {turnosEmUso.length === 0 ? (
          <span className="text-sm text-tinta-2">Só reserva do dia inteiro</span>
        ) : (
          <ul className="m-0 flex list-none flex-wrap gap-1.5 p-0">
            {turnosEmUso.map((t) => (
              <li key={t.codigo} className="inline-flex items-center gap-1.5 rounded-full bg-trilho px-2.5 py-1 text-xs font-semibold text-tinta-2">
                <Clock size={12} aria-hidden />
                {t.nome} · {faixaDeHorario(t.horaInicio, t.horaFim)}
              </li>
            ))}
          </ul>
        )}
        {turnosForaDeUso.length > 0 && (
          <span className="text-xs text-apagado">Fora de uso: {turnosForaDeUso.map((t) => t.nome).join(', ')}</span>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-end gap-1.5 border-t border-borda-suave px-4 py-3">
        <Botao pequeno variante="texto" onClick={aoMudarSituacao} disabled={ocupada} className="max-lg:h-11">
          <Power size={15} aria-hidden />
          {area.ativa ? 'Inativar' : 'Reativar'}
        </Botao>
        <button
          type="button"
          aria-label={`Excluir ${area.nome}`}
          title="Excluir"
          onClick={aoExcluir}
          disabled={ocupada}
          className="flex size-11 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-perigo disabled:cursor-not-allowed disabled:opacity-50 lg:size-[34px]"
        >
          <Trash2 size={17} aria-hidden />
        </button>
        <Botao pequeno onClick={aoEditar} disabled={ocupada} className="max-lg:h-11">
          <Pencil size={14} aria-hidden />
          Editar
        </Botao>
      </div>
    </li>
  );
}
