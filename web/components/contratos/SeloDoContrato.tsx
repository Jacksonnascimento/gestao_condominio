import { differenceInCalendarDays, parseISO } from 'date-fns';
import { Selo, type TomDoSelo } from '@/components/Interface';
import type { Contrato, SituacaoContrato } from '@/services/contratoService';
import { formatarData, valorDoEnum } from '@/services/utilitarios';

const SELO: Record<SituacaoContrato, { tom: TomDoSelo; texto: string }> = {
  ATIVO: { tom: 'info', texto: 'Ativo' },
  A_VENCER: { tom: 'aviso', texto: 'Vencendo' },
  FINALIZADO: { tom: 'neutro', texto: 'Vencido' },
  RESCINDIDO: { tom: 'perigo', texto: 'Rescindido' },
};

export function situacaoDoContrato(contrato: Contrato): SituacaoContrato {
  return valorDoEnum(contrato.status) as SituacaoContrato;
}

/**
 * Na API, "A Vencer" e "Finalizado"; na tela, "Vencendo" e "Vencido", que dizem à gestão o que fazer com o
 * contrato.
 */
export function SeloDoContrato({ contrato }: { contrato: Contrato }) {
  const selo = SELO[situacaoDoContrato(contrato)] ?? { tom: 'neutro' as const, texto: contrato.statusDescricao ?? '' };
  return <Selo tom={selo.tom}>{selo.texto}</Selo>;
}

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** 1200.5 vira "R$ 1.200,50". */
export function formatarValor(valor: number | null | undefined): string {
  return valor == null ? '' : MOEDA.format(valor);
}

/** "01/01/2026 a 31/12/2026". */
export function textoDaVigencia(contrato: Contrato): string {
  return `${formatarData(contrato.dataInicio)} a ${formatarData(contrato.dataFim)}`;
}

/** Para os contratos vencendo: "vence hoje", "vence amanhã", "vence em 12 dias". */
export function textoDoVencimento(contrato: Contrato): string {
  if (situacaoDoContrato(contrato) !== 'A_VENCER') return '';
  const dias = differenceInCalendarDays(parseISO(contrato.dataFim), new Date());
  if (dias <= 0) return 'vence hoje';
  if (dias === 1) return 'vence amanhã';
  return `vence em ${dias} dias`;
}
