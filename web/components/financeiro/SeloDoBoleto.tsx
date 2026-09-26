import { Selo, type TomDoSelo } from '@/components/Interface';
import type { Boleto } from '@/services/financeiroService';
import { textoLegivelDeCodigo } from '@/services/utilitarios';

const SITUACOES: Record<string, { texto: string; tom: TomDoSelo }> = {
  ABERTO: { texto: 'Em aberto', tom: 'aviso' },
  VENCIDO: { texto: 'Vencido', tom: 'perigo' },
  PAGO: { texto: 'Pago', tom: 'neutro' },
};

export function SeloDoBoleto({ boleto }: { boleto: Boleto }) {
  const situacao = SITUACOES[boleto.status];
  return <Selo tom={situacao?.tom ?? 'neutro'}>{situacao?.texto ?? textoLegivelDeCodigo(boleto.status)}</Selo>;
}
