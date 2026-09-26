'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { X } from 'lucide-react';
import { Marca } from '@/components/Marca';
import { MENU, type ItemDoMenu } from '@/components/navegacao';
import { useSessao } from '@/context/SessaoContext';
import { iniciais } from '@/services/utilitarios';

function estaAtivo(href: string, caminho: string) {
  return href === '/' ? caminho === '/' : caminho === href || caminho.startsWith(`${href}/`);
}

function ItemDoMenuLateral({ item, ativo, aoNavegar }: { item: ItemDoMenu; ativo: boolean; aoNavegar?: () => void }) {
  const Icone = item.icone;
  if (!item.pronta) {
    return (
      <span
        className="flex h-[38px] items-center gap-3 rounded-[10px] px-3 text-sm font-medium text-apagado"
        title="Esta tela ainda está sendo migrada para o sistema novo."
      >
        <Icone size={19} strokeWidth={1.7} aria-hidden />
        <span className="grow">{item.rotulo}</span>
        <span className="rounded-full bg-trilho px-2 py-px text-[11px] font-semibold text-tinta-2">em breve</span>
      </span>
    );
  }
  return (
    <Link
      href={item.href}
      onClick={aoNavegar}
      aria-current={ativo ? 'page' : undefined}
      className={`flex h-[38px] items-center gap-3 rounded-[10px] px-3 text-sm no-underline transition-colors ${
        ativo ? 'bg-realce font-bold text-tinta hover:text-tinta' : 'font-medium text-tinta-2 hover:bg-trilho hover:text-tinta'
      }`}
    >
      <Icone size={19} strokeWidth={ativo ? 1.9 : 1.7} className={ativo ? 'text-ouro' : undefined} aria-hidden />
      {item.rotulo}
    </Link>
  );
}

/**
 * Menu lateral, agrupado por assunto. No computador fica fixo à esquerda; no celular abre por cima da tela,
 * pelo botão "Mais" da barra inferior ou pelo ícone de menu do topo.
 */
export function Sidebar({ aberto, aoFechar }: { aberto: boolean; aoFechar: () => void }) {
  const caminho = usePathname();
  const { usuario, permissoes, descricaoDoPapel } = useSessao();

  const conteudo = (
    <div className="flex h-full flex-col gap-[22px] px-4 py-[22px]">
      <div className="flex items-center justify-between px-1.5">
        <Marca />
        <button
          type="button"
          onClick={aoFechar}
          aria-label="Fechar menu"
          className="flex size-11 items-center justify-center rounded-[10px] text-tinta-2 hover:bg-trilho lg:hidden"
        >
          <X size={20} aria-hidden />
        </button>
      </div>
      <nav aria-label="Menu principal" className="flex flex-col gap-3.5 overflow-y-auto">
        {MENU.map((grupo, indice) => {
          const itens = grupo.itens.filter((item) => !item.visivel || item.visivel(permissoes));
          if (itens.length === 0) return null;
          return (
            <div key={grupo.titulo ?? indice} className="flex flex-col gap-px">
              {grupo.titulo && (
                <span className="px-3 pb-[5px] text-[11px] font-bold tracking-[0.08em] text-apagado uppercase">
                  {grupo.titulo}
                </span>
              )}
              {itens.map((item) => (
                <ItemDoMenuLateral key={item.href} item={item} ativo={estaAtivo(item.href, caminho)} aoNavegar={aoFechar} />
              ))}
            </div>
          );
        })}
      </nav>
      {usuario && (
        <div className="mt-auto flex items-center gap-3 rounded-xl border border-borda bg-superficie px-3 py-2.5">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-tinta text-[13px] font-bold text-ouro-claro">
            {iniciais(usuario.nome)}
          </span>
          <span className="flex min-w-0 flex-col gap-px">
            <span className="truncate text-sm font-semibold">{usuario.nome}</span>
            <span className="truncate text-xs text-apagado">{descricaoDoPapel}</span>
          </span>
        </div>
      )}
    </div>
  );

  return (
    <>
      <aside className="sticky top-0 hidden h-screen w-64 shrink-0 border-r border-borda bg-lateral lg:block">{conteudo}</aside>
      {aberto && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
          <button type="button" aria-label="Fechar menu" onClick={aoFechar} className="absolute inset-0 bg-veu/40" />
          <aside className="relative h-full w-[288px] max-w-[85vw] border-r border-borda bg-lateral shadow-xl">{conteudo}</aside>
        </div>
      )}
    </>
  );
}
