'use client';

import { useId, useRef, useState } from 'react';
import { Paperclip, X } from 'lucide-react';
import { TAMANHO_MAXIMO_DE_ARQUIVO, formatarTamanho } from '@/services/arquivos';

/**
 * Escolha de um arquivo para anexar, no visual dos outros campos. Recusa na hora arquivos acima do limite da API
 * (10 MB) e mostra o nome e o tamanho do escolhido, com um botão para desfazer a escolha.
 */
export function CampoDeArquivo({
  rotulo,
  arquivo,
  aoMudar,
  ajuda,
  disabled,
  className = '',
}: {
  rotulo: string;
  arquivo: File | null;
  aoMudar: (arquivo: File | null) => void;
  ajuda?: string;
  disabled?: boolean;
  className?: string;
}) {
  const id = useId();
  const entrada = useRef<HTMLInputElement>(null);
  const [erro, setErro] = useState('');

  function escolher(evento: React.ChangeEvent<HTMLInputElement>) {
    const escolhido = evento.target.files?.[0] ?? null;
    if (escolhido && escolhido.size > TAMANHO_MAXIMO_DE_ARQUIVO) {
      setErro(`O arquivo tem ${formatarTamanho(escolhido.size)}; o limite é de 10 MB.`);
      evento.target.value = '';
      aoMudar(null);
      return;
    }
    if (escolhido && escolhido.size === 0) {
      setErro('O arquivo escolhido está vazio.');
      evento.target.value = '';
      aoMudar(null);
      return;
    }
    setErro('');
    aoMudar(escolhido);
  }

  function limpar() {
    if (entrada.current) entrada.current.value = '';
    setErro('');
    aoMudar(null);
  }

  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <span id={`${id}-rotulo`} className="text-[13px] font-semibold text-tinta-2">
        {rotulo}
      </span>
      <div className="flex min-h-11 items-center gap-2 rounded-[10px] border border-dashed border-contorno bg-superficie px-3 py-1.5 has-[input:focus-visible]:border-ouro has-[input:focus-visible]:outline-2 has-[input:focus-visible]:outline-ouro">
        <Paperclip size={16} className="shrink-0 text-apagado" aria-hidden />
        {arquivo ? (
          <>
            <span className="min-w-0 grow truncate text-sm">
              {arquivo.name} <span className="text-apagado">· {formatarTamanho(arquivo.size)}</span>
            </span>
            <button
              type="button"
              onClick={limpar}
              disabled={disabled}
              aria-label={`Tirar o arquivo ${arquivo.name}`}
              className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-trilho hover:text-tinta sm:size-8"
            >
              <X size={16} aria-hidden />
            </button>
          </>
        ) : (
          <label htmlFor={id} className="grow cursor-pointer py-1.5 text-sm text-apagado">
            <span className="font-semibold text-ouro underline-offset-2 hover:underline">Escolher arquivo</span> (até 10 MB)
          </label>
        )}
        <input
          ref={entrada}
          id={id}
          type="file"
          aria-labelledby={`${id}-rotulo`}
          onChange={escolher}
          disabled={disabled}
          className="sr-only"
        />
      </div>
      {erro ? (
        <span className="text-xs text-perigo" role="alert">
          {erro}
        </span>
      ) : (
        ajuda && <span className="text-xs text-apagado">{ajuda}</span>
      )}
    </div>
  );
}
