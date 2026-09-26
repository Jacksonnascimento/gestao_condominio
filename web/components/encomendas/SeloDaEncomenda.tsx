import { Selo, type TomDoSelo } from '@/components/Interface';
import type { Encomenda } from '@/services/encomendaService';
import { descricaoDoEnum, valorDoEnum } from '@/services/utilitarios';

const TOM: Record<string, TomDoSelo> = {
  PENDENTE: 'aviso',
  RETIRADA: 'neutro',
  DEVOLVIDA: 'info',
  EXTRAVIADA: 'perigo',
};

/** "Pendente" na API; na tela, "Aguardando", que diz ao morador o que está acontecendo. */
export function SeloDaEncomenda({ encomenda }: { encomenda: Encomenda }) {
  const situacao = valorDoEnum(encomenda.status);
  const texto = situacao === 'PENDENTE' ? 'Aguardando' : descricaoDoEnum(encomenda.status, encomenda.statusDescricao);
  return <Selo tom={TOM[situacao] ?? 'neutro'}>{texto}</Selo>;
}

export function estaAguardando(encomenda: Encomenda): boolean {
  return valorDoEnum(encomenda.status) === 'PENDENTE';
}
