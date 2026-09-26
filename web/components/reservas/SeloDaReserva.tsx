import { Selo, type TomDoSelo } from '@/components/Interface';
import type { Reserva } from '@/services/reservaService';
import { descricaoDoEnum, faixaDeHorario, valorDoEnum } from '@/services/utilitarios';

const TOM: Record<string, TomDoSelo> = {
  PENDENTE_APROVACAO: 'aviso',
  APROVADA: 'info',
  REJEITADA: 'perigo',
  CANCELADA_PELO_MORADOR: 'neutro',
  CONCLUIDA: 'neutro',
};

/** "Pendente" na API; na tela, "Aguardando aprovação", que diz ao morador o que falta. */
export function SeloDaReserva({ reserva }: { reserva: Reserva }) {
  const situacao = valorDoEnum(reserva.status);
  const texto = situacao === 'PENDENTE_APROVACAO' ? 'Aguardando aprovação' : descricaoDoEnum(reserva.status, reserva.statusDescricao);
  return <Selo tom={TOM[situacao] ?? 'neutro'}>{texto}</Selo>;
}

/** "Noite · 18:00 às 23:00", ou "Dia inteiro" quando a reserva não tem turno. */
export function turnoDaReserva(reserva: Reserva): string {
  if (!reserva.turnoCodigo) return 'Dia inteiro';
  const faixa = faixaDeHorario(reserva.turnoHoraInicio, reserva.turnoHoraFim);
  return faixa ? `${reserva.turnoNome} · ${faixa}` : (reserva.turnoNome ?? '');
}
