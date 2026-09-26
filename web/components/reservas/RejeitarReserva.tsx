'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { resumoDaReserva } from '@/components/reservas/acoesDaReserva';
import { Botao, CaixaDeErro, CampoDeTexto } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { reservaService, type Reserva } from '@/services/reservaService';
import { mensagemErroApi } from '@/services/utilitarios';

/** Rejeição de uma reserva pendente: o motivo é obrigatório e aparece para o morador. */
export function RejeitarReserva({
  reserva,
  aoFechar,
  aoRejeitar,
}: {
  reserva: Reserva;
  aoFechar: () => void;
  aoRejeitar: () => void;
}) {
  const [motivo, setMotivo] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      await reservaService.rejeitar(reserva.codigo, motivo.trim());
      toast.success('Reserva rejeitada.');
      aoRejeitar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível rejeitar a reserva.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo="Rejeitar reserva"
      subtitulo={resumoDaReserva(reserva)}
      largura="sm"
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Voltar
          </Botao>
          <Botao type="submit" form="rejeitar-reserva" variante="perigo" carregando={salvando} disabled={!motivo.trim()}>
            Rejeitar reserva
          </Botao>
        </>
      }
    >
      <form id="rejeitar-reserva" onSubmit={salvar} className="flex flex-col gap-4">
        {erro && <CaixaDeErro>{erro}</CaixaDeErro>}
        <CampoDeTexto
          rotulo="Motivo"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          obrigatorio
          ajuda={`${reserva.solicitanteNome ?? 'O morador'} verá este motivo na reserva.`}
          placeholder="Data já ocupada por evento do condomínio, pendência na unidade…"
        />
      </form>
    </Modal>
  );
}
