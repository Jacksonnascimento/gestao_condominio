'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import { ocorrenciaService, type OpcoesOcorrencia, type TipoOcorrencia } from '@/services/ocorrenciaService';
import { mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

/** Registro de uma ocorrência nova: o que aconteceu, de que tipo e com qual unidade. */
export function RegistrarOcorrencia({
  opcoes,
  aoFechar,
  aoRegistrar,
}: {
  opcoes: OpcoesOcorrencia;
  aoFechar: () => void;
  aoRegistrar: () => void;
}) {
  const { condominio } = useSessao();
  const unidades = [...opcoes.unidades].sort((a, b) =>
    rotuloUnidade(a.numero, a.bloco).localeCompare(rotuloUnidade(b.numero, b.bloco), 'pt-BR', { numeric: true }),
  );
  const [unidadeId, setUnidadeId] = useState(unidades.length === 1 ? String(unidades[0].codigo) : '');
  const [tipo, setTipo] = useState('');
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      await ocorrenciaService.registrar({
        unidadeId: Number(unidadeId),
        tipo: tipo as TipoOcorrencia,
        titulo: titulo.trim(),
        descricao: descricao.trim(),
        condominioId: condominio?.id,
      });
      toast.success('Ocorrência registrada. A administração vai acompanhar.');
      aoRegistrar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível registrar a ocorrência.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo="Registrar ocorrência"
      subtitulo={condominio?.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="registrar-ocorrencia" variante="primario" carregando={salvando}>
            Registrar
          </Botao>
        </>
      }
    >
      <form id="registrar-ocorrencia" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && <CaixaDeErro className="sm:col-span-2">{erro}</CaixaDeErro>}
        <CampoDeSelecao rotulo="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)} obrigatorio>
          <option value="">Escolha o tipo</option>
          {opcoes.tipos.map((t) => (
            <option key={t.valor} value={t.valor}>
              {t.descricao}
            </option>
          ))}
        </CampoDeSelecao>
        <CampoDeSelecao
          rotulo="Unidade"
          value={unidadeId}
          onChange={(e) => setUnidadeId(e.target.value)}
          obrigatorio
          ajuda="A unidade de onde vem ou a que se refere a ocorrência."
        >
          <option value="">{unidades.length ? 'Escolha a unidade' : 'Nenhuma unidade disponível'}</option>
          {unidades.map((u) => (
            <option key={u.codigo} value={u.codigo}>
              {rotuloUnidade(u.numero, u.bloco)}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo
          rotulo="Título"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          obrigatorio
          maxLength={150}
          placeholder="Resumo em poucas palavras"
          className="sm:col-span-2"
        />
        <CampoDeTexto
          rotulo="O que aconteceu"
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          obrigatorio
          rows={6}
          placeholder="Conte o que aconteceu, quando e onde."
          className="sm:col-span-2"
        />
      </form>
    </Modal>
  );
}
