import { Selo } from '@/components/Interface';
import type { Comunicado } from '@/services/comunicadoService';

/** "Urgente" em destaque e o público do comunicado ("Para todos", "Para proprietários"...). */
export function SelosDoComunicado({ comunicado }: { comunicado: Comunicado }) {
  return (
    <span className="flex flex-wrap items-center gap-1.5">
      {comunicado.urgente && <Selo tom="perigo">Urgente</Selo>}
      <Selo tom="neutro">{rotuloDoPublico(comunicado)}</Selo>
    </span>
  );
}

export function rotuloDoPublico(comunicado: Pick<Comunicado, 'publicoDestino' | 'publicoDestinoDescricao'>): string {
  if (comunicado.publicoDestino === 'TODOS') return 'Para todos';
  const descricao = comunicado.publicoDestinoDescricao ?? comunicado.publicoDestino;
  return `Para ${descricao.toLowerCase()}`;
}

/** "PDF", "JPG"... a partir do nome do arquivo guardado. */
export function extensaoDoAnexo(nome?: string | null): string {
  const partes = (nome ?? '').split('.');
  return partes.length > 1 ? partes[partes.length - 1].toUpperCase() : '';
}
