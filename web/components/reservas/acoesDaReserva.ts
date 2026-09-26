import toast from 'react-hot-toast';
import { turnoDaReserva } from '@/components/reservas/SeloDaReserva';
import { confirmar } from '@/services/confirmacao';
import { reservaService, type Reserva } from '@/services/reservaService';
import { formatarData, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

/** "Salão de festas em 10/10/2026 (Noite · 18:00 às 23:00)" */
export function resumoDaReserva(reserva: Reserva): string {
  return `${reserva.areaNome} em ${formatarData(reserva.data)} (${turnoDaReserva(reserva)})`;
}

/** Pergunta e aprova. Resolve true se a reserva foi aprovada. */
export async function aprovarReserva(reserva: Reserva): Promise<boolean> {
  const unidade = rotuloUnidade(reserva.unidadeNumero, reserva.unidadeBloco);
  const certeza = await confirmar({
    titulo: 'Aprovar reserva',
    mensagem: `Aprovar ${resumoDaReserva(reserva)}${unidade ? `, pedida pela unidade ${unidade}` : ''}? O morador verá a reserva como aprovada.`,
    textoConfirmar: 'Aprovar',
  });
  if (!certeza) return false;
  try {
    await reservaService.aprovar(reserva.codigo);
    toast.success('Reserva aprovada.');
    return true;
  } catch (e) {
    toast.error(mensagemErroApi(e, 'Não foi possível aprovar a reserva.'));
    return false;
  }
}

/** Pergunta e cancela. Resolve true se a reserva foi cancelada. */
export async function cancelarReserva(reserva: Reserva): Promise<boolean> {
  const certeza = await confirmar({
    titulo: 'Cancelar reserva',
    mensagem: `Cancelar ${resumoDaReserva(reserva)}? A data fica livre para outras unidades, e não dá para desfazer.`,
    textoConfirmar: 'Cancelar reserva',
    perigo: true,
  });
  if (!certeza) return false;
  try {
    await reservaService.cancelar(reserva.codigo);
    toast.success('Reserva cancelada.');
    return true;
  } catch (e) {
    toast.error(mensagemErroApi(e, 'Não foi possível cancelar a reserva.'));
    return false;
  }
}
