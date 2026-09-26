import Image from 'next/image';

/** Logo e nome do sistema. */
export function Marca({ tamanho = 42, comNome = true }: { tamanho?: number; comNome?: boolean }) {
  return (
    <span className="flex items-center gap-3">
      <Image src="/logo.png" alt={comNome ? '' : 'Condigtal'} width={tamanho} height={tamanho} priority />
      {comNome && <span className="font-titulo text-[28px] leading-none text-tinta">Condigtal</span>}
    </span>
  );
}
