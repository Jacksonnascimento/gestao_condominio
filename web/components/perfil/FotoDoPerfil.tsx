'use client';

import { useEffect, useState } from 'react';
import { perfilService } from '@/services/perfilService';
import { iniciais } from '@/services/utilitarios';

/** Foto de quem está logado; sem foto (ou se ela não carregar), as iniciais do nome. */
export function FotoDoPerfil({ nome, possuiFoto }: { nome: string; possuiFoto: boolean }) {
  const [endereco, setEndereco] = useState<string | null>(null);

  useEffect(() => {
    if (!possuiFoto) return;
    let ativa = true;
    let criado: string | null = null;
    perfilService
      .foto()
      .then((imagem) => {
        if (!ativa) return;
        criado = URL.createObjectURL(imagem);
        setEndereco(criado);
      })
      .catch(() => {
        // Sem a foto, ficam as iniciais
      });
    return () => {
      ativa = false;
      if (criado) URL.revokeObjectURL(criado);
    };
  }, [possuiFoto]);

  if (possuiFoto && endereco) {
    // Imagem vinda da API como arquivo: o next/image não se aplica a endereços blob:
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={endereco} alt={`Foto de ${nome}`} className="size-20 shrink-0 rounded-full border border-borda object-cover" />;
  }
  return (
    <span
      aria-hidden
      className="flex size-20 shrink-0 items-center justify-center rounded-full bg-tinta font-titulo text-[30px] text-ouro-claro"
    >
      {iniciais(nome)}
    </span>
  );
}
