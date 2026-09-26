'use client';

import { useState } from 'react';
import { BarraDoCelular } from '@/components/BarraDoCelular';
import { Sidebar } from '@/components/Sidebar';
import { Topo } from '@/components/Topo';
import { SessaoProvider, useSessao } from '@/context/SessaoContext';

function Conteudo({ children }: { children: React.ReactNode }) {
  const { carregando, condominio, usuario } = useSessao();
  if (carregando) {
    return (
      <div className="flex flex-1 items-center justify-center p-10 text-sm text-apagado" role="status">
        Carregando…
      </div>
    );
  }
  if (usuario && !condominio && !usuario.administradorGeral) {
    return (
      <div className="flex flex-1 items-center justify-center p-10">
        <p className="max-w-md text-center text-sm text-tinta-2">
          Seu acesso ainda não está ligado a nenhum condomínio. Peça ao síndico ou à administração para liberar.
        </p>
      </div>
    );
  }
  return <>{children}</>;
}

/** Menu lateral, topo e barra do celular em volta de todas as telas de quem está logado. */
export function EstruturaDoSistema({ children }: { children: React.ReactNode }) {
  const [menuAberto, setMenuAberto] = useState(false);
  return (
    <SessaoProvider>
      <div className="flex min-h-screen">
        <Sidebar aberto={menuAberto} aoFechar={() => setMenuAberto(false)} />
        <div className="flex min-w-0 flex-1 flex-col">
          <Topo aoAbrirMenu={() => setMenuAberto(true)} />
          <main className="flex flex-1 flex-col px-4 pt-6 pb-28 sm:px-6 lg:px-9 lg:pt-7 lg:pb-9">
            <Conteudo>{children}</Conteudo>
          </main>
        </div>
      </div>
      <BarraDoCelular aoAbrirMenu={() => setMenuAberto(true)} />
    </SessaoProvider>
  );
}
