import { Selo, type TomDoSelo } from '@/components/Interface';
import type { SituacaoOcorrencia } from '@/services/ocorrenciaService';
import { textoLegivelDeCodigo } from '@/services/utilitarios';

const TOM: Record<SituacaoOcorrencia, TomDoSelo> = {
  ABERTA: 'aviso',
  EM_ANALISE: 'info',
  RESOLVIDA: 'neutro',
};

/** Como cada situação aparece na tela ("Em Análise" na API vira "Em análise"). */
export const ROTULO_DA_SITUACAO: Record<SituacaoOcorrencia, string> = {
  ABERTA: 'Aberta',
  EM_ANALISE: 'Em análise',
  RESOLVIDA: 'Resolvida',
};

export function SeloDaOcorrencia({ situacao }: { situacao: SituacaoOcorrencia }) {
  return <Selo tom={TOM[situacao] ?? 'neutro'}>{ROTULO_DA_SITUACAO[situacao] ?? textoLegivelDeCodigo(situacao)}</Selo>;
}
