'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Building2, ChevronDown, CircleUser, LogOut, Menu } from 'lucide-react';
import { Marca } from '@/components/Marca';
import { useSessao } from '@/context/SessaoContext';
import { iniciais } from '@/services/utilitarios';

/** Escolha do condomínio. Com um só, mostra o nome sem lista. */
function SeletorDeCondominio() {
  const { condominios, condominio, trocarCondominio } = useSessao();
  if (!condominio) return null;
  if (condominios.length < 2) {
    return (
      <span className="flex h-10 min-w-0 items-center gap-2.5 text-sm font-bold">
        <Building2 size={17} className="shrink-0 text-ouro" aria-hidden />
        <span className="truncate">{condominio.nome}</span>
      </span>
    );
  }
  return (
    <label className="relative flex h-10 min-w-0 items-center gap-2.5 rounded-[10px] border border-borda bg-superficie pr-9 pl-3.5 text-sm font-bold">
      <Building2 size={17} className="shrink-0 text-ouro" aria-hidden />
      <span className="sr-only">Condomínio</span>
      <select
        value={condominio.id}
        onChange={(e) => trocarCondominio(Number(e.target.value))}
        className="absolute inset-0 cursor-pointer opacity-0"
      >
        {condominios.map((c) => (
          <option key={c.id} value={c.id}>
            {c.nome}
          </option>
        ))}
      </select>
      <span className="truncate">{condominio.nome}</span>
      <ChevronDown size={15} className="pointer-events-none absolute right-3 text-apagado" aria-hidden />
    </label>
  );
}

function MenuDaConta() {
  const { usuario, descricaoDoPapel, sair } = useSessao();
  const [aberto, setAberto] = useState(false);
  const caixa = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!aberto) return;
    const fecharFora = (evento: MouseEvent) => {
      if (caixa.current && !caixa.current.contains(evento.target as Node)) setAberto(false);
    };
    const fecharNoEsc = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') setAberto(false);
    };
    document.addEventListener('mousedown', fecharFora);
    document.addEventListener('keydown', fecharNoEsc);
    return () => {
      document.removeEventListener('mousedown', fecharFora);
      document.removeEventListener('keydown', fecharNoEsc);
    };
  }, [aberto]);

  if (!usuario) return null;
  return (
    <div ref={caixa} className="relative">
      <button
        type="button"
        onClick={() => setAberto((a) => !a)}
        aria-expanded={aberto}
        aria-label="Minha conta"
        className="flex size-10 items-center justify-center rounded-full bg-tinta text-[13px] font-bold text-ouro-claro"
      >
        {iniciais(usuario.nome)}
      </button>
      {aberto && (
        <div className="absolute right-0 z-30 mt-2 w-64 rounded-xl border border-borda bg-superficie p-2 shadow-lg">
          <div className="flex flex-col gap-0.5 px-3 py-2">
            <span className="truncate text-sm font-bold">{usuario.nome}</span>
            <span className="truncate text-[13px] text-apagado">{usuario.email}</span>
            {descricaoDoPapel && <span className="truncate text-[13px] text-apagado">{descricaoDoPapel}</span>}
          </div>
          <Link
            href="/perfil"
            onClick={() => setAberto(false)}
            className="flex h-11 w-full items-center gap-2.5 rounded-lg px-3 text-sm font-semibold text-tinta no-underline hover:bg-trilho hover:text-tinta"
          >
            <CircleUser size={17} aria-hidden />
            Meu perfil
          </Link>
          <button
            type="button"
            onClick={sair}
            className="flex h-11 w-full items-center gap-2.5 rounded-lg px-3 text-left text-sm font-semibold text-tinta hover:bg-trilho"
          >
            <LogOut size={17} aria-hidden />
            Sair
          </button>
        </div>
      )}
    </div>
  );
}

/** Barra do topo: menu (no celular), condomínio e conta. */
export function Topo({ aoAbrirMenu }: { aoAbrirMenu: () => void }) {
  return (
    <header className="sticky top-0 z-20 flex h-16 shrink-0 items-center justify-between gap-4 border-b border-borda bg-lateral px-4 sm:px-6 lg:px-9">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          onClick={aoAbrirMenu}
          aria-label="Abrir menu"
          className="flex size-11 shrink-0 items-center justify-center rounded-[10px] text-tinta hover:bg-trilho lg:hidden"
        >
          <Menu size={21} aria-hidden />
        </button>
        <span className="shrink-0 lg:hidden">
          <Marca tamanho={34} comNome={false} />
        </span>
        <SeletorDeCondominio />
      </div>
      <MenuDaConta />
    </header>
  );
}
