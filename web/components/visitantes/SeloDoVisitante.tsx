import { Selo } from '@/components/Interface';
import type { VisitanteResumo } from '@/services/visitanteService';
import { valorDoEnum } from '@/services/utilitarios';

/** "No Local" na API; na tela, "No condomínio", que diz à portaria onde a pessoa está. */
export function SeloDoVisitante({ visitante }: { visitante: VisitanteResumo }) {
  return estaNoCondominio(visitante) ? <Selo tom="aviso">No condomínio</Selo> : <Selo tom="neutro">Saiu</Selo>;
}

export function estaNoCondominio(visitante: VisitanteResumo): boolean {
  return valorDoEnum(visitante.status) === 'NO_LOCAL';
}
