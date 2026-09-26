import { Selo } from '@/components/Interface';
import { emFrase } from '@/components/unidades/PecasDeCadastro';
import type { Ocupante } from '@/services/ocupanteService';
import { descricaoDoEnum } from '@/services/utilitarios';

/** Vínculo da pessoa com a unidade: proprietário em destaque, os demais em tom neutro. */
export function SeloDoVinculo({ ocupante }: { ocupante: Ocupante }) {
  if (!ocupante.vinculo) return null;
  const proprietario = ocupante.vinculo === 'PROPRIETARIO' || ocupante.vinculo === 'MULTIPROPRIETARIO';
  return <Selo tom={proprietario ? 'info' : 'neutro'}>{emFrase(descricaoDoEnum(ocupante.vinculo, ocupante.vinculoDescricao))}</Selo>;
}

/**
 * Nome do tipo de período de uso do multiproprietário. A API descreve FLUANTE como "Fluante"; na tela, o certo é
 * "Flutuante".
 */
export function descricaoDoTipoDePeriodo(valor?: string | null, descricao?: string | null): string {
  if (!valor) return '';
  if (valor === 'FLUANTE') return 'Flutuante';
  return emFrase(descricao || descricaoDoEnum(valor));
}
