'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { useSessao } from '@/context/SessaoContext';
import { condominioService, type UnidadeResumo } from '@/services/condominioService';
import { encomendaService } from '@/services/encomendaService';
import type { Opcao } from '@/services/tipos';
import { agoraParaCampo, hojeParaCampo, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

/** Registro de uma encomenda que chegou na portaria, para a unidade escolhida. */
export function RegistrarEncomenda({
  tipos,
  aoFechar,
  aoRegistrar,
}: {
  tipos: Opcao[];
  aoFechar: () => void;
  aoRegistrar: () => void;
}) {
  const { condominio, usuario } = useSessao();
  const [unidades, setUnidades] = useState<UnidadeResumo[] | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [formulario, setFormulario] = useState({
    unidadeId: '',
    destinatario: '',
    tipo: tipos[0]?.valor ?? '',
    descricao: '',
    dataRecebimento: hojeParaCampo(),
    horaRecebimento: agoraParaCampo(),
    nomeRecebidoPor: usuario?.nome ?? '',
    observacoes: '',
  });

  useEffect(() => {
    if (!condominio) return;
    condominioService
      .unidades(condominio.id)
      .then((lista) => setUnidades([...lista].sort((a, b) => rotuloUnidade(a.numero, a.bloco).localeCompare(rotuloUnidade(b.numero, b.bloco), 'pt-BR', { numeric: true }))))
      .catch((e) => {
        setUnidades([]);
        setErro(mensagemErroApi(e, 'Não foi possível carregar as unidades.'));
      });
  }, [condominio]);

  const mudar = (campo: keyof typeof formulario) => (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setFormulario((atual) => ({ ...atual, [campo]: evento.target.value }));

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!condominio) return;
    setErro('');
    setSalvando(true);
    try {
      await encomendaService.registrar({
        condominioId: condominio.id,
        unidadeId: Number(formulario.unidadeId),
        destinatario: formulario.destinatario.trim(),
        tipo: formulario.tipo,
        descricao: formulario.descricao.trim() || undefined,
        dataRecebimento: formulario.dataRecebimento,
        horaRecebimento: formulario.horaRecebimento,
        nomeRecebidoPor: formulario.nomeRecebidoPor.trim(),
        observacoes: formulario.observacoes.trim() || undefined,
      });
      toast.success('Encomenda registrada.');
      aoRegistrar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível registrar a encomenda.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo="Registrar encomenda"
      subtitulo={condominio?.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="registrar-encomenda" variante="primario" carregando={salvando}>
            Registrar
          </Botao>
        </>
      }
    >
      <form id="registrar-encomenda" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && <CaixaDeErro className="sm:col-span-2">{erro}</CaixaDeErro>}
        <CampoDeSelecao rotulo="Unidade" value={formulario.unidadeId} onChange={mudar('unidadeId')} obrigatorio disabled={!unidades}>
          <option value="">{unidades ? 'Escolha a unidade' : 'Carregando…'}</option>
          {unidades?.map((u) => (
            <option key={u.id} value={u.id}>
              {rotuloUnidade(u.numero, u.bloco)}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo rotulo="Destinatário" value={formulario.destinatario} onChange={mudar('destinatario')} obrigatorio maxLength={100} />
        <CampoDeSelecao rotulo="Tipo" value={formulario.tipo} onChange={mudar('tipo')} obrigatorio>
          {tipos.map((t) => (
            <option key={t.valor} value={t.valor}>
              {t.descricao}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo rotulo="Descrição" value={formulario.descricao} onChange={mudar('descricao')} placeholder="Caixa pequena, envelope…" maxLength={255} />
        <Campo rotulo="Recebida em" type="date" value={formulario.dataRecebimento} onChange={mudar('dataRecebimento')} obrigatorio max={hojeParaCampo()} />
        <Campo rotulo="Hora" type="time" value={formulario.horaRecebimento} onChange={mudar('horaRecebimento')} obrigatorio />
        <Campo rotulo="Recebida por" value={formulario.nomeRecebidoPor} onChange={mudar('nomeRecebidoPor')} obrigatorio maxLength={100} className="sm:col-span-2" />
        <CampoDeTexto rotulo="Observações" value={formulario.observacoes} onChange={mudar('observacoes')} className="sm:col-span-2" />
      </form>
    </Modal>
  );
}
