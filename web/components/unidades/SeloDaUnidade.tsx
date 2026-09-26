import { Selo, type TomDoSelo } from '@/components/Interface';
import type { Unidade } from '@/services/unidadeService';
import { descricaoDoEnum, emFrase } from '@/services/utilitarios';

const TOM: Record<string, TomDoSelo> = {
  OCUPADA: 'info',
  VAZIA: 'neutro',
  EM_REFORMA: 'aviso',
  MULTIPROPRIEDADE: 'info',
};

/** Situação de ocupação da unidade; a unidade inativa mostra só "Inativa". */
export function SeloDaUnidade({ unidade }: { unidade: Unidade }) {
  if (!unidade.ativa) return <Selo tom="perigo">Inativa</Selo>;
  if (!unidade.statusOcupacao) return null;
  return (
    <Selo tom={TOM[unidade.statusOcupacao] ?? 'neutro'}>
      {emFrase(descricaoDoEnum(unidade.statusOcupacao, unidade.statusOcupacaoDescricao))}
    </Selo>
  );
}

/** "Apartamento · Andar 3 · 72 m²": o resumo da unidade numa linha. */
export function resumoDaUnidade(unidade: Unidade): string {
  const partes = [
    unidade.tipoDescricao ?? descricaoDoEnum(unidade.tipo),
    unidade.andar ? `Andar ${unidade.andar}` : '',
    unidade.areaPrivada != null ? `${Number(unidade.areaPrivada).toLocaleString('pt-BR')} m²` : '',
  ];
  return partes.filter(Boolean).join(' · ');
}
