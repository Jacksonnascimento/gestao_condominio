'use client';

import { useEffect, useRef, useState } from 'react';
import { Camera, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { Botao } from '@/components/Interface';
import type { UsuarioLogado } from '@/services/autenticacaoService';
import { confirmar } from '@/services/confirmacao';
import { perfilService } from '@/services/perfilService';
import { iniciais, mensagemErroApi } from '@/services/utilitarios';

/** Maior lado da foto enviada, em pixels: ela aparece pequena, e fica guardada no cadastro da pessoa. */
const LADO_DA_FOTO = 512;

/**
 * A imagem escolhida, reduzida para no máximo {@link LADO_DA_FOTO} pixels no maior lado e convertida em JPEG. Assim
 * uma foto de celular de vários megabytes vira poucas dezenas de kilobytes, dentro do limite da API (1 MB), e fotos
 * HEIC ou PNG saem no formato que a API aceita.
 */
async function reduzirImagem(arquivo: File): Promise<Blob> {
  const imagem = await createImageBitmap(arquivo);
  const escala = Math.min(1, LADO_DA_FOTO / Math.max(imagem.width, imagem.height));
  const largura = Math.max(1, Math.round(imagem.width * escala));
  const altura = Math.max(1, Math.round(imagem.height * escala));
  const tela = document.createElement('canvas');
  tela.width = largura;
  tela.height = altura;
  const contexto = tela.getContext('2d');
  if (!contexto) throw new Error('Sem canvas');
  // Fundo branco: a transparência de um PNG ficaria preta no JPEG
  contexto.fillStyle = '#fff';
  contexto.fillRect(0, 0, largura, altura);
  contexto.drawImage(imagem, 0, 0, largura, altura);
  imagem.close();
  return new Promise((resolver, rejeitar) =>
    tela.toBlob((blob) => (blob ? resolver(blob) : rejeitar(new Error('Sem imagem'))), 'image/jpeg', 0.86),
  );
}

/** Foto de quem está logado; sem foto (ou se ela não carregar), as iniciais do nome. */
export function FotoDoPerfil({ nome, possuiFoto, versao = 0 }: { nome: string; possuiFoto: boolean; versao?: number }) {
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
  }, [possuiFoto, versao]);

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

/** A foto com os botões de trocar e de tirar. Devolve o perfil atualizado pela API. */
export function EditorDaFoto({ perfil, aoMudar }: { perfil: UsuarioLogado; aoMudar: (novo: UsuarioLogado) => void }) {
  const entrada = useRef<HTMLInputElement>(null);
  // Sobe a cada troca, para a foto ser lida de novo mesmo quando já havia uma
  const [versao, setVersao] = useState(0);
  const [ocupado, setOcupado] = useState(false);

  async function escolher(evento: React.ChangeEvent<HTMLInputElement>) {
    const arquivo = evento.target.files?.[0];
    evento.target.value = '';
    if (!arquivo) return;
    setOcupado(true);
    let reduzida: Blob;
    try {
      reduzida = await reduzirImagem(arquivo);
    } catch {
      toast.error('Não foi possível ler esta imagem. Escolha uma foto em JPEG, PNG ou WebP.');
      setOcupado(false);
      return;
    }
    try {
      const novo = await perfilService.trocarFoto(reduzida);
      setVersao((v) => v + 1);
      toast.success('Foto atualizada.');
      aoMudar(novo);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível trocar a foto.'));
    } finally {
      setOcupado(false);
    }
  }

  async function tirar() {
    const confirmado = await confirmar({
      titulo: 'Tirar a foto',
      mensagem: 'A sua foto sai do perfil, e no lugar dela aparecem as iniciais do seu nome. Você pode pôr outra quando quiser.',
      textoConfirmar: 'Tirar a foto',
      perigo: true,
    });
    if (!confirmado) return;
    setOcupado(true);
    try {
      const novo = await perfilService.tirarFoto();
      toast.success('Foto retirada.');
      aoMudar(novo);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível tirar a foto.'));
    } finally {
      setOcupado(false);
    }
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <FotoDoPerfil nome={perfil.nome} possuiFoto={perfil.possuiFoto} versao={versao} />
      <div className="flex items-center gap-0.5">
        <Botao pequeno variante="texto" onClick={() => entrada.current?.click()} carregando={ocupado} className="max-sm:h-11">
          {!ocupado && <Camera size={15} aria-hidden />}
          {perfil.possuiFoto ? 'Trocar' : 'Pôr foto'}
        </Botao>
        {perfil.possuiFoto && (
          <button
            type="button"
            aria-label="Tirar a foto"
            title="Tirar a foto"
            onClick={tirar}
            disabled={ocupado}
            className="flex size-11 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-perigo-fundo hover:text-perigo disabled:cursor-not-allowed disabled:opacity-50 sm:size-8"
          >
            <Trash2 size={15} aria-hidden />
          </button>
        )}
      </div>
      <input
        ref={entrada}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/heic,image/heif"
        aria-label="Escolher a foto do perfil"
        onChange={escolher}
        className="sr-only"
        tabIndex={-1}
      />
    </div>
  );
}
