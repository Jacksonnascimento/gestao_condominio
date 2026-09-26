'use client';

import { useEffect, useState } from 'react';
import { CheckCircle2, Download, FileText, LoaderCircle, MessageSquare, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { CampoDeArquivo } from '@/components/CampoDeArquivo';
import { SeloDaOcorrencia } from '@/components/ocorrencias/SeloDaOcorrencia';
import { Botao, CaixaDeErro, CampoDeTexto, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { formatarTamanho } from '@/services/arquivos';
import { confirmar } from '@/services/confirmacao';
import {
  ocorrenciaService,
  type AnexoDaOcorrencia,
  type Ocorrencia,
  type OcorrenciaResumida,
} from '@/services/ocorrenciaService';
import type { Opcao } from '@/services/tipos';
import { formatarDataHora, mensagemErroApi, rotuloUnidade, textoLegivelDeCodigo } from '@/services/utilitarios';

function Secao({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3">
      <h3 className="m-0 text-xs font-bold tracking-[0.04em] text-apagado uppercase">{titulo}</h3>
      {children}
    </section>
  );
}

/** Um ponto da linha do tempo: quem, quando e o quê. */
function Marco({ titulo, quando, children }: { titulo: string; quando: string; children?: React.ReactNode }) {
  return (
    <li className="relative flex flex-col gap-1 pb-5 pl-5 last:pb-0">
      <span className="absolute top-[5px] -left-[7px] size-3 rounded-full border-2 border-ouro bg-superficie" aria-hidden />
      <span className="text-sm">
        <span className="font-bold">{titulo}</span> <span className="text-apagado">· {quando}</span>
      </span>
      {children}
    </li>
  );
}

type Modo = 'acompanhar' | 'resolver';

/**
 * Tudo sobre a ocorrência: a descrição, e, quando resolvida, o parecer final. Para quem trata a ocorrência (a API
 * diz em `podeGerenciar`), também a linha do tempo de comentários, os anexos e os formulários para comentar, anexar
 * e resolver. O primeiro comentário ou anexo coloca a ocorrência em análise; o parecer final a resolve.
 */
export function DetalhesDaOcorrencia({
  resumo,
  tipos,
  aoFechar,
  aoAlterar,
}: {
  resumo: OcorrenciaResumida;
  tipos: Opcao[];
  aoFechar: () => void;
  /** Algo mudou na ocorrência (situação, comentários, anexos): a lista por trás deve ser lida de novo. */
  aoAlterar: () => void;
}) {
  const [ocorrencia, setOcorrencia] = useState<Ocorrencia | null>(null);
  const [erroAoCarregar, setErroAoCarregar] = useState('');
  // Sobe a cada comentário, anexo ou resolução, para a ocorrência ser lida de novo
  const [versao, setVersao] = useState(0);
  const [modo, setModo] = useState<Modo>('acompanhar');
  const [comentario, setComentario] = useState('');
  const [arquivo, setArquivo] = useState<File | null>(null);
  // Chave do campo de arquivo: trocar a chave limpa o campo depois de um envio
  const [chaveDoArquivo, setChaveDoArquivo] = useState(0);
  const [parecer, setParecer] = useState('');
  const [enviando, setEnviando] = useState<'comentario' | 'anexo' | 'parecer' | null>(null);
  const [baixando, setBaixando] = useState<number | null>(null);
  const [excluindo, setExcluindo] = useState<number | null>(null);
  const [erro, setErro] = useState('');
  // Enquanto um envio ou uma exclusão está em andamento, a janela não fecha
  const ocupado = enviando !== null || excluindo !== null;

  const id = resumo.id;

  useEffect(() => {
    let ativa = true;
    ocorrenciaService
      .buscar(id)
      .then((dados) => {
        if (ativa) setOcorrencia(dados);
      })
      .catch((e) => {
        if (ativa) setErroAoCarregar(mensagemErroApi(e, 'Não foi possível carregar a ocorrência.'));
      });
    return () => {
      ativa = false;
    };
  }, [id, versao]);

  const depoisDeMudar = () => {
    setVersao((v) => v + 1);
    aoAlterar();
  };

  async function enviar(qual: 'comentario' | 'anexo' | 'parecer', acao: () => Promise<unknown>, sucesso: string, falha: string) {
    setErro('');
    setEnviando(qual);
    try {
      await acao();
      toast.success(sucesso);
      depoisDeMudar();
      return true;
    } catch (e) {
      setErro(mensagemErroApi(e, falha));
      return false;
    } finally {
      setEnviando(null);
    }
  }

  async function comentar(evento: React.FormEvent) {
    evento.preventDefault();
    const texto = comentario.trim();
    if (!texto) return;
    const ok = await enviar('comentario', () => ocorrenciaService.comentar(id, texto), 'Comentário registrado.', 'Não foi possível registrar o comentário.');
    if (ok) setComentario('');
  }

  async function anexar() {
    if (!arquivo) return;
    const escolhido = arquivo;
    const ok = await enviar('anexo', () => ocorrenciaService.anexar(id, escolhido), 'Arquivo anexado.', 'Não foi possível anexar o arquivo.');
    if (ok) {
      setArquivo(null);
      setChaveDoArquivo((c) => c + 1);
    }
  }

  async function resolver(evento: React.FormEvent) {
    evento.preventDefault();
    const texto = parecer.trim();
    if (!texto) return;
    const ok = await enviar('parecer', () => ocorrenciaService.resolver(id, texto), 'Ocorrência resolvida.', 'Não foi possível resolver a ocorrência.');
    if (ok) {
      setParecer('');
      setModo('acompanhar');
    }
  }

  async function baixar(anexo: AnexoDaOcorrencia) {
    setBaixando(anexo.id);
    try {
      await ocorrenciaService.baixarAnexo(id, anexo);
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível baixar o arquivo.'));
    } finally {
      setBaixando(null);
    }
  }

  async function excluirAnexo(anexo: AnexoDaOcorrencia) {
    const confirmado = await confirmar({
      titulo: 'Excluir anexo',
      mensagem: `O arquivo “${anexo.nomeOriginal}” será apagado desta ocorrência. Essa ação não pode ser desfeita.`,
      textoConfirmar: 'Excluir',
      perigo: true,
    });
    if (!confirmado) return;
    setExcluindo(anexo.id);
    try {
      await ocorrenciaService.excluirAnexo(id, anexo.id);
      toast.success('Anexo excluído.');
      depoisDeMudar();
    } catch (e) {
      toast.error(mensagemErroApi(e, 'Não foi possível excluir o anexo.'));
    } finally {
      setExcluindo(null);
    }
  }

  const situacao = ocorrencia?.status ?? resumo.status;
  const resolvida = situacao === 'RESOLVIDA';
  const gerencia = ocorrencia?.podeGerenciar === true;
  const tipo = ocorrencia?.tipo ?? resumo.tipo;
  const descricaoDoTipo = tipos.find((t) => t.valor === tipo)?.descricao ?? textoLegivelDeCodigo(tipo);
  const unidade = rotuloUnidade(ocorrencia?.unidadeNumero ?? resumo.unidadeNumero, ocorrencia?.unidadeBloco ?? resumo.unidadeBloco);
  const comentarios = [...(ocorrencia?.comentarios ?? [])].sort((a, b) => a.dataComentario.localeCompare(b.dataComentario));

  const rodape =
    gerencia && !resolvida && modo === 'resolver' ? (
      <>
        <Botao variante="texto" onClick={() => setModo('acompanhar')} disabled={ocupado} className="max-sm:h-11">
          Voltar
        </Botao>
        <Botao type="submit" form="resolver-ocorrencia" variante="primario" carregando={enviando === 'parecer'} disabled={!parecer.trim()} className="max-sm:h-11">
          Marcar como resolvida
        </Botao>
      </>
    ) : (
      <>
        {gerencia && !resolvida && (
          <Botao
            onClick={() => {
              setErro('');
              setModo('resolver');
            }}
            disabled={ocupado}
            className="mr-auto max-sm:h-11"
          >
            <CheckCircle2 size={16} aria-hidden />
            Resolver ocorrência
          </Botao>
        )}
        <Botao variante={gerencia && !resolvida ? 'texto' : 'secundario'} onClick={aoFechar} disabled={ocupado} className="max-sm:h-11">
          Fechar
        </Botao>
      </>
    );

  return (
    <Modal
      titulo={ocorrencia?.titulo ?? resumo.titulo}
      subtitulo={[descricaoDoTipo, unidade, ocorrencia?.condominioNome ?? resumo.condominioNome].filter(Boolean).join(' · ')}
      largura="lg"
      aoFechar={aoFechar}
      ocupado={ocupado}
      rodape={rodape}
    >
      {!ocorrencia ? (
        erroAoCarregar ? (
          <CaixaDeErro>{erroAoCarregar}</CaixaDeErro>
        ) : (
          <p className="flex items-center gap-2 py-6 text-sm text-apagado" role="status">
            <LoaderCircle size={16} className="animate-spin" aria-hidden />
            Carregando a ocorrência…
          </p>
        )
      ) : (
        <div className="flex flex-col gap-6">
          <div>
            <SeloDaOcorrencia situacao={situacao} />
          </div>

          <p className="m-0 text-[15px] leading-relaxed break-words whitespace-pre-line">{ocorrencia.descricao}</p>

          <dl className="m-0 divide-y divide-borda-suave border-y border-borda-suave">
            <LinhaDeDetalhe rotulo="Registrada em" valor={formatarDataHora(ocorrencia.dataRegistro)} />
            <LinhaDeDetalhe rotulo="Registrada por" valor={ocorrencia.nomePessoaRegistro} />
            <LinhaDeDetalhe rotulo="Unidade" valor={unidade} />
            <LinhaDeDetalhe rotulo="Tipo" valor={descricaoDoTipo} />
          </dl>

          {resolvida && ocorrencia.parecerFinal && (
            <div className="flex flex-col gap-1.5 rounded-xl border border-borda bg-cabecalho px-4 py-3.5">
              <span className="text-xs font-bold tracking-[0.04em] text-apagado uppercase">Parecer final</span>
              <p className="m-0 text-sm leading-relaxed break-words whitespace-pre-line">{ocorrencia.parecerFinal}</p>
              <span className="text-xs text-apagado">
                {['Resolvida', ocorrencia.nomePessoaFinalizou ? `por ${ocorrencia.nomePessoaFinalizou}` : null, ocorrencia.dataFinalizacao ? `em ${formatarDataHora(ocorrencia.dataFinalizacao)}` : null]
                  .filter(Boolean)
                  .join(' ')}
              </span>
            </div>
          )}

          {!gerencia && !resolvida && (
            <p className="m-0 rounded-xl bg-info-fundo px-4 py-3 text-sm text-info">
              {situacao === 'ABERTA'
                ? 'A administração do condomínio vai analisar esta ocorrência. O parecer aparece aqui quando ela for resolvida.'
                : 'A administração do condomínio está analisando esta ocorrência. O parecer aparece aqui quando ela for resolvida.'}
            </p>
          )}

          {gerencia && (
            <>
              <Secao titulo="Andamento">
                <ol className="m-0 ml-[5px] list-none border-l-2 border-borda-suave p-0">
                  <Marco titulo="Ocorrência registrada" quando={formatarDataHora(ocorrencia.dataRegistro)} />
                  {comentarios.map((c) => (
                    <Marco key={c.id} titulo={c.nomeUsuario ?? 'Comentário'} quando={formatarDataHora(c.dataComentario)}>
                      <p className="m-0 text-sm leading-relaxed break-words whitespace-pre-line text-tinta-2">{c.comentario}</p>
                    </Marco>
                  ))}
                  {resolvida && ocorrencia.dataFinalizacao && (
                    <Marco titulo="Resolvida" quando={formatarDataHora(ocorrencia.dataFinalizacao)} />
                  )}
                </ol>
                {comentarios.length === 0 && <p className="m-0 text-sm text-apagado">Nenhum comentário ainda.</p>}
              </Secao>

              <Secao titulo="Anexos">
                {ocorrencia.anexos.length === 0 ? (
                  <p className="m-0 text-sm text-apagado">Nenhum arquivo anexado.</p>
                ) : (
                  <ul className="m-0 flex list-none flex-col gap-2 p-0">
                    {ocorrencia.anexos.map((anexo) => (
                      <li key={anexo.id} className="flex items-center gap-3 rounded-xl border border-borda px-3 py-2">
                        <FileText size={18} className="shrink-0 text-ouro" aria-hidden />
                        <span className="flex min-w-0 grow flex-col">
                          <span className="truncate text-sm font-semibold">{anexo.nomeOriginal}</span>
                          <span className="text-xs text-apagado">
                            {[formatarTamanho(anexo.tamanhoArquivo), anexo.nomeUsuario, formatarDataHora(anexo.dataAnexo)].filter(Boolean).join(' · ')}
                          </span>
                        </span>
                        <button
                          type="button"
                          aria-label={`Baixar ${anexo.nomeOriginal}`}
                          onClick={() => baixar(anexo)}
                          disabled={baixando === anexo.id}
                          className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-lg text-tinta-2 hover:bg-trilho hover:text-tinta disabled:cursor-wait sm:size-[34px]"
                        >
                          {baixando === anexo.id ? <LoaderCircle size={16} className="animate-spin" aria-hidden /> : <Download size={16} aria-hidden />}
                        </button>
                        {!resolvida && (
                          <button
                            type="button"
                            aria-label={`Excluir ${anexo.nomeOriginal}`}
                            onClick={() => excluirAnexo(anexo)}
                            disabled={ocupado}
                            className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-lg text-apagado hover:bg-perigo-fundo hover:text-perigo sm:size-[34px]"
                          >
                            <Trash2 size={16} aria-hidden />
                          </button>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </Secao>

              {!resolvida && (
                <div className="flex flex-col gap-4 rounded-xl bg-cabecalho p-4">
                  {erro && <CaixaDeErro>{erro}</CaixaDeErro>}
                  {modo === 'resolver' ? (
                    <form id="resolver-ocorrencia" onSubmit={resolver} className="flex flex-col gap-3">
                      <CampoDeTexto
                        rotulo="Parecer final"
                        value={parecer}
                        onChange={(e) => setParecer(e.target.value)}
                        obrigatorio
                        rows={5}
                        autoFocus
                        placeholder="O que foi feito e como a ocorrência terminou."
                        ajuda="O parecer fica visível para quem registrou a ocorrência. Depois de resolvida, ela não recebe mais comentários nem anexos."
                      />
                    </form>
                  ) : (
                    <>
                      <form onSubmit={comentar} className="flex flex-col gap-3">
                        <CampoDeTexto
                          rotulo="Novo comentário"
                          value={comentario}
                          onChange={(e) => setComentario(e.target.value)}
                          placeholder="Registre o que foi feito ou combinado."
                          ajuda={situacao === 'ABERTA' ? 'O primeiro comentário coloca a ocorrência em análise.' : undefined}
                        />
                        <div className="flex justify-end">
                          <Botao type="submit" carregando={enviando === 'comentario'} disabled={!comentario.trim() || ocupado} className="max-sm:h-11">
                            {enviando !== 'comentario' && <MessageSquare size={16} aria-hidden />}
                            {situacao === 'ABERTA' ? 'Comentar e colocar em análise' : 'Comentar'}
                          </Botao>
                        </div>
                      </form>
                      <div className="flex flex-col gap-3 border-t border-borda pt-4">
                        <CampoDeArquivo
                          key={chaveDoArquivo}
                          rotulo="Anexar arquivo"
                          arquivo={arquivo}
                          aoMudar={setArquivo}
                          disabled={ocupado}
                          ajuda="Fotos, laudos ou outros documentos da ocorrência."
                        />
                        <div className="flex justify-end">
                          <Botao onClick={anexar} carregando={enviando === 'anexo'} disabled={!arquivo || ocupado} className="max-sm:h-11">
                            Enviar arquivo
                          </Botao>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      )}
    </Modal>
  );
}
