'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, Campo } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { encomendaService, type Encomenda } from '@/services/encomendaService';
import { agoraParaCampo, hojeParaCampo, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

/** Baixa da encomenda: quem retirou, e quando. */
export function EntregarEncomenda({
  encomenda,
  aoFechar,
  aoEntregar,
}: {
  encomenda: Encomenda;
  aoFechar: () => void;
  aoEntregar: () => void;
}) {
  const [nomeRetirada, setNomeRetirada] = useState(encomenda.destinatario);
  const [dataRetirada, setDataRetirada] = useState(hojeParaCampo());
  const [horaRetirada, setHoraRetirada] = useState(agoraParaCampo());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      await encomendaService.registrarRetirada(encomenda.id, { nomeRetirada: nomeRetirada.trim(), dataRetirada, horaRetirada });
      toast.success('Entrega registrada.');
      aoEntregar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível registrar a entrega.'));
      setSalvando(false);
    }
  }

  const unidade = rotuloUnidade(encomenda.unidadeNumero, encomenda.unidadeBloco);

  return (
    <Modal
      titulo="Entregar encomenda"
      subtitulo={`${encomenda.tipoDescricao ?? encomenda.tipo} para ${encomenda.destinatario}${unidade ? ` · ${unidade}` : ''}`}
      largura="sm"
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="entregar-encomenda" variante="primario" carregando={salvando}>
            Confirmar entrega
          </Botao>
        </>
      }
    >
      <form id="entregar-encomenda" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo sm:col-span-2" role="alert">
            {erro}
          </p>
        )}
        <Campo
          rotulo="Quem retirou"
          value={nomeRetirada}
          onChange={(e) => setNomeRetirada(e.target.value)}
          obrigatorio
          maxLength={100}
          className="sm:col-span-2"
        />
        <Campo rotulo="Data" type="date" value={dataRetirada} onChange={(e) => setDataRetirada(e.target.value)} obrigatorio max={hojeParaCampo()} />
        <Campo rotulo="Hora" type="time" value={horaRetirada} onChange={(e) => setHoraRetirada(e.target.value)} obrigatorio />
      </form>
    </Modal>
  );
}
