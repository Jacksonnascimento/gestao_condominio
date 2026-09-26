import { Marca } from '@/components/Marca';

/** Moldura das telas de fora do sistema: login, esqueci a senha e definir senha. */
export function TelaDeAcesso({ titulo, subtitulo, children }: { titulo: string; subtitulo?: string; children: React.ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="flex w-full max-w-[420px] flex-col gap-8">
        <div className="flex justify-center">
          <Marca tamanho={52} />
        </div>
        <div className="flex flex-col gap-6 rounded-2xl border border-borda bg-superficie px-6 py-7 sm:px-8">
          <div className="flex flex-col gap-1.5">
            <h1 className="font-titulo text-[38px] leading-none font-normal">{titulo}</h1>
            {subtitulo && <p className="text-sm leading-relaxed text-apagado">{subtitulo}</p>}
          </div>
          {children}
        </div>
      </div>
    </main>
  );
}
