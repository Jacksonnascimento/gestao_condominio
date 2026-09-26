'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Menu } from 'lucide-react';
import { ATALHOS_DO_CELULAR, MENU } from '@/components/navegacao';

const ITENS = MENU.flatMap((grupo) => grupo.itens);

/** Barra inferior do celular: as telas mais usadas no dia a dia e o botão que abre o menu completo. */
export function BarraDoCelular({ aoAbrirMenu }: { aoAbrirMenu: () => void }) {
  const caminho = usePathname();
  const atalhos = ATALHOS_DO_CELULAR.map((href) => ITENS.find((item) => item.href === href)).filter(
    (item) => item !== undefined,
  );

  return (
    <nav
      aria-label="Atalhos"
      className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-5 border-t border-borda bg-lateral px-2 pt-2 pb-[max(env(safe-area-inset-bottom),10px)] lg:hidden"
    >
      {atalhos.map((item) => {
        const Icone = item.icone;
        const ativo = item.href === '/' ? caminho === '/' : caminho.startsWith(item.href);
        const rotulo = item.rotulo === 'Comunicados' ? 'Avisos' : item.rotulo;
        const classe = 'flex h-14 flex-col items-center justify-center gap-1 text-[11px] no-underline';
        if (!item.pronta) {
          return (
            <span key={item.href} className={`${classe} font-semibold text-apagado opacity-60`} title="Em breve">
              <Icone size={22} strokeWidth={1.7} aria-hidden />
              {rotulo}
            </span>
          );
        }
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={ativo ? 'page' : undefined}
            className={`${classe} ${ativo ? 'font-extrabold text-tinta hover:text-tinta' : 'font-semibold text-tinta-2 hover:text-tinta'}`}
          >
            <span
              className={`flex h-[30px] w-[52px] items-center justify-center rounded-full ${ativo ? 'bg-realce text-ouro' : ''}`}
            >
              <Icone size={22} strokeWidth={ativo ? 1.9 : 1.7} aria-hidden />
            </span>
            {rotulo}
          </Link>
        );
      })}
      <button
        type="button"
        onClick={aoAbrirMenu}
        className="flex h-14 flex-col items-center justify-center gap-1 text-[11px] font-semibold text-tinta-2"
      >
        <span className="flex h-[30px] w-[52px] items-center justify-center">
          <Menu size={22} strokeWidth={1.7} aria-hidden />
        </span>
        Mais
      </button>
    </nav>
  );
}
