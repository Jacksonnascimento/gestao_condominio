'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { CampoDeArquivo } from '@/components/comunicados/CampoDeArquivo';
import { useTravaDaJanela } from '@/components/comunicados/useTravaDaJanela';
import { Botao, Campo, CampoDeSelecao, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import {
  comunicadoService,
  type Comunicado,
  type OpcoesComunicado,
  type PublicoDoComunicado,
} from '@/services/comunicadoService';
import { mensagemErroApi } from '@/services/utilitarios';

/**
 * Publicação de um comunicado novo ou edição de um existente, com anexo opcional. O administrador geral escolhe
 * para quais condomínios o comunicado vai; os demais publicam no próprio condomínio.
 */
export function FormularioDeComunicado({
  comunicado,
  opcoes,
  aoFechar,
  aoSalvar,
}: {
  /** Ausente, publica um comunicado novo. */
  comunicado?: Comunicado;
  opcoes: OpcoesComunicado;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const { condominio } = useSessao();
  const escolheCondominios = opcoes.condominios.length > 0;
  const [titulo, setTitulo] = useState(comunicado?.titulo ?? '');
  const [mensagem, setMensagem] = useState(comunicado?.mensagem ?? '');
  const [publico, setPublico] = useState<PublicoDoComunicado>(comunicado?.publicoDestino ?? 'TODOS');
  const [urgente, setUrgente] = useState(comunicado?.urgente ?? false);
  const [condominioIds, setCondominioIds] = useState<number[]>(() => {
    if (comunicado?.condominios.length) return comunicado.condominios.map((c) => c.codigo);
    return condominio && opcoes.condominios.some((c) => c.codigo === condominio.id) ? [condominio.id] : [];
  });
  const [anexo, setAnexo] = useState<File | null>(null);
  const { travada: salvando, travar: setSalvando, fechar } = useTravaDaJanela(aoFechar);
  const [erro, setErro] = useState('');

  const marcarCondominio = (codigo: number, marcado: boolean) =>
    setCondominioIds((atuais) => (marcado ? [...atuais, codigo] : atuais.filter((c) => c !== codigo)));

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    if (escolheCondominios && condominioIds.length === 0) {
      setErro('Escolha ao menos um condomínio para receber o comunicado.');
      return;
    }
    setErro('');
    setSalvando(true);
    const dados = {
      titulo: titulo.trim(),
      mensagem: mensagem.trim(),
      publicoDestino: publico,
      urgente,
      condominioIds: escolheCondominios ? condominioIds : undefined,
    };
    try {
      if (comunicado) {
        await comunicadoService.editar(comunicado.id, dados, anexo);
        toast.success('Comunicado atualizado.');
      } else {
        await comunicadoService.publicar(dados, anexo);
        toast.success('Comunicado publicado.');
      }
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, comunicado ? 'Não foi possível salvar o comunicado.' : 'Não foi possível publicar o comunicado.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo={comunicado ? 'Editar comunicado' : 'Publicar comunicado'}
      subtitulo={escolheCondominios ? undefined : condominio?.nome}
      largura="lg"
      aoFechar={fechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="formulario-comunicado" variante="primario" carregando={salvando}>
            {comunicado ? 'Salvar' : 'Publicar'}
          </Botao>
        </>
      }
    >
      <form id="formulario-comunicado" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo sm:col-span-2" role="alert">
            {erro}
          </p>
        )}
        <Campo
          rotulo="Título"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          obrigatorio
          maxLength={255}
          className="sm:col-span-2"
        />
        <CampoDeTexto
          rotulo="Mensagem"
          value={mensagem}
          onChange={(e) => setMensagem(e.target.value)}
          obrigatorio
          rows={8}
          className="sm:col-span-2"
        />
        <CampoDeSelecao
          rotulo="Para quem"
          value={publico}
          onChange={(e) => setPublico(e.target.value as PublicoDoComunicado)}
          obrigatorio
          ajuda="Quem vai ver o comunicado. Síndico e administração veem todos."
        >
          {opcoes.publicos.map((p) => (
            <option key={p.valor} value={p.valor}>
              {p.descricao}
            </option>
          ))}
        </CampoDeSelecao>
        <div className="flex flex-col gap-1.5">
          <span className="text-[13px] font-semibold text-tinta-2">Destaque</span>
          <label className="flex min-h-11 cursor-pointer items-center gap-3 rounded-[10px] border border-borda bg-superficie px-3 text-sm">
            <input
              type="checkbox"
              checked={urgente}
              onChange={(e) => setUrgente(e.target.checked)}
              className="size-[18px] cursor-pointer accent-[var(--color-perigo)]"
            />
            Marcar como urgente
          </label>
          <span className="text-xs text-apagado">O comunicado aparece com o selo “Urgente” em destaque.</span>
        </div>

        {escolheCondominios && (
          <fieldset className="m-0 flex flex-col gap-1.5 border-0 p-0 sm:col-span-2">
            <legend className="mb-1.5 text-[13px] font-semibold text-tinta-2">
              Condomínios que recebem <span className="text-ouro">*</span>
            </legend>
            <div className="grid gap-2 sm:grid-cols-2">
              {opcoes.condominios.map((c) => (
                <label
                  key={c.codigo}
                  className="flex min-h-11 cursor-pointer items-center gap-3 rounded-[10px] border border-borda bg-superficie px-3 text-sm"
                >
                  <input
                    type="checkbox"
                    checked={condominioIds.includes(c.codigo)}
                    onChange={(e) => marcarCondominio(c.codigo, e.target.checked)}
                    className="size-[18px] cursor-pointer accent-[var(--color-tinta)]"
                  />
                  {c.nome ?? `Condomínio ${c.codigo}`}
                </label>
              ))}
            </div>
          </fieldset>
        )}

        <CampoDeArquivo
          rotulo={comunicado?.possuiAnexo ? 'Trocar o anexo' : 'Anexo'}
          arquivo={anexo}
          aoMudar={setAnexo}
          disabled={salvando}
          ajuda={
            comunicado?.possuiAnexo
              ? 'Este comunicado já tem um anexo. Escolha outro arquivo só se quiser substituí-lo.'
              : 'Opcional: um PDF, uma imagem ou outro documento para quem for ler.'
          }
          className="sm:col-span-2"
        />
      </form>
    </Modal>
  );
}
