'use client';

import { Botao } from '@/components/Interface';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';

export default function ErroNaTela({ reset }: { error: Error; reset: () => void }) {
  return (
    <TelaDeAcesso titulo="Algo deu errado" subtitulo="A tela não carregou como deveria. Tente de novo; se continuar, avise a administração.">
      <Botao variante="primario" onClick={reset} className="w-full">
        Tentar de novo
      </Botao>
    </TelaDeAcesso>
  );
}
