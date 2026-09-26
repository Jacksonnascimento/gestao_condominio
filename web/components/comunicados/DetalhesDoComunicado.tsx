'use client';

import { useState } from 'react';
import { Download, Paperclip, Pencil, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { SelosDoComunicado, extensaoDoAnexo } from '@/components/comunicados/SelosDoComunicado';
import { Botao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { comunicadoService, type Comunicado } from '@/services/comunicadoService';
import { confirmar } from '@/services/confirmacao';
import { formatarDataHora, mensagemErroApi } from '@/services/utilitarios';

/** Leitura do comunicado inteiro, com o anexo para baixar. Para quem pode, também editar e excluir. */
export function DetalhesDoComunicado({
  comunicado,
  aoFechar,
  aoEditar,
  aoExcluir,
}: {
  comunicado: Comunicado;
  aoFechar: () => void;
  aoEditar: () => void;
  aoExcluir: () => void;
}) {
  const [baixando, setBaixando] = useState(false);
  // Enquanto a exclusão está em andamento, esta janela não fecha
  const [ocupado, setOcupado] = useState(false);

  async function baixar() {
    setBaixando(true);
    try {
      await comunicadoService.baixarAnexo(comunicado);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível baixar o anexo.'));
    } finally {
      setBaixando(false);
    }
  }

  async function excluir() {
    const confirmado = await confirmar({
      titulo: 'Excluir comunicado',
      mensagem: `O comunicado “${comunicado.titulo}” e o seu anexo serão apagados e ninguém mais poderá lê-lo. Essa ação não pode ser desfeita.`,
      textoConfirmar: 'Excluir',
      perigo: true,
    });
    if (!confirmado) return;
    setOcupado(true);
    try {
      await comunicadoService.excluir(comunicado.id);
      toast.success('Comunicado excluído.');
      aoExcluir();
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível excluir o comunicado.'));
      setOcupado(false);
    }
  }

  const publicado = [formatarDataHora(comunicado.dataCadastro), comunicado.autor ? `por ${comunicado.autor}` : null]
    .filter(Boolean)
    .join(' ');
  const extensao = extensaoDoAnexo(comunicado.nomeAnexo);

  return (
    <Modal
      titulo={comunicado.titulo}
      subtitulo={`Publicado em ${publicado}`}
      largura="lg"
      aoFechar={aoFechar}
      ocupado={ocupado}
      rodape={
        <>
          {comunicado.podeGerenciar && (
            <>
              <Botao variante="texto" onClick={excluir} disabled={ocupado} className="mr-auto text-perigo max-sm:h-11">
                <Trash2 size={16} aria-hidden />
                Excluir
              </Botao>
              <Botao onClick={aoEditar} disabled={ocupado} className="max-sm:h-11">
                <Pencil size={16} aria-hidden />
                Editar
              </Botao>
            </>
          )}
          <Botao variante={comunicado.podeGerenciar ? 'texto' : 'secundario'} onClick={aoFechar} disabled={ocupado} className="max-sm:h-11">
            Fechar
          </Botao>
        </>
      }
    >
      <article className="flex flex-col gap-5">
        <SelosDoComunicado comunicado={comunicado} />
        <p className="m-0 text-[15px] leading-relaxed break-words whitespace-pre-line text-tinta">{comunicado.mensagem}</p>

        {comunicado.possuiAnexo && (
          <div className="flex flex-wrap items-center gap-3 rounded-xl border border-borda bg-cabecalho px-4 py-3">
            <Paperclip size={18} className="shrink-0 text-ouro" aria-hidden />
            <span className="flex min-w-0 grow flex-col">
              <span className="text-sm font-bold">Anexo do comunicado</span>
              {extensao && <span className="text-xs text-apagado">Arquivo {extensao}</span>}
            </span>
            <Botao pequeno onClick={baixar} carregando={baixando} className="max-sm:h-11">
              {!baixando && <Download size={15} aria-hidden />}
              Baixar anexo
            </Botao>
          </div>
        )}

        {comunicado.condominios.length > 0 && (
          <p className="m-0 text-sm text-apagado">
            Enviado para: {comunicado.condominios.map((c) => c.nome ?? `Condomínio ${c.codigo}`).join(', ')}
          </p>
        )}
      </article>
    </Modal>
  );
}
