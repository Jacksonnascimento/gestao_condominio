'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { SeloDaEncomenda } from '@/components/encomendas/SeloDaEncomenda';
import { Botao, CaixaDeErro, CampoDeSelecao, CampoDeTexto, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { encomendaService, type Encomenda } from '@/services/encomendaService';
import type { Opcao } from '@/services/tipos';
import { formatarDataHora, mensagemErroApi, rotuloUnidade, valorDoEnum } from '@/services/utilitarios';

/**
 * Tudo sobre a encomenda. Para a gestão e a portaria, também a mudança de situação (devolvida, extraviada ou
 * de volta a aguardando); a entrega tem o seu próprio botão na lista.
 */
export function DetalhesDaEncomenda({
  encomenda,
  podeGerenciar,
  situacoes,
  aoFechar,
  aoAlterar,
}: {
  encomenda: Encomenda;
  podeGerenciar: boolean;
  situacoes: Opcao[];
  aoFechar: () => void;
  aoAlterar: () => void;
}) {
  const atual = valorDoEnum(encomenda.status);
  const outras = situacoes.filter((s) => s.valor !== atual);
  const [novaSituacao, setNovaSituacao] = useState('');
  const [observacoes, setObservacoes] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      await encomendaService.mudarSituacao(encomenda.id, novaSituacao, observacoes.trim() || undefined);
      toast.success('Situação atualizada.');
      aoAlterar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível mudar a situação.'));
      setSalvando(false);
    }
  }

  const rotuloDaSituacao = (s: Opcao) => (s.valor === 'PENDENTE' ? 'Aguardando retirada' : s.descricao);

  return (
    <Modal
      titulo={encomenda.tipoDescricao ?? encomenda.tipo}
      subtitulo={`Para ${encomenda.destinatario}`}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        podeGerenciar && outras.length > 0 ? (
          <>
            <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
              Fechar
            </Botao>
            <Botao type="submit" form="mudar-situacao" variante="primario" carregando={salvando} disabled={!novaSituacao}>
              Mudar situação
            </Botao>
          </>
        ) : (
          <Botao variante="secundario" onClick={aoFechar}>
            Fechar
          </Botao>
        )
      }
    >
      <div className="flex flex-col gap-5">
        <div>
          <SeloDaEncomenda encomenda={encomenda} />
        </div>
        <dl className="m-0 divide-y divide-borda-suave">
          <LinhaDeDetalhe rotulo="Unidade" valor={rotuloUnidade(encomenda.unidadeNumero, encomenda.unidadeBloco)} />
          <LinhaDeDetalhe rotulo="Descrição" valor={encomenda.descricao} />
          <LinhaDeDetalhe rotulo="Recebida em" valor={formatarDataHora(encomenda.dataRecebimento)} />
          <LinhaDeDetalhe rotulo="Recebida por" valor={encomenda.nomeRecebidoPor} />
          <LinhaDeDetalhe rotulo="Observações" valor={encomenda.observacoes} />
          <LinhaDeDetalhe rotulo="Retirada em" valor={formatarDataHora(encomenda.dataRetirada)} />
          <LinhaDeDetalhe rotulo="Retirada por" valor={encomenda.nomeRetirada} />
          <LinhaDeDetalhe rotulo="Última observação" valor={encomenda.observacaoAtualizacao} />
        </dl>
        {podeGerenciar && outras.length > 0 && (
          <form id="mudar-situacao" onSubmit={salvar} className="flex flex-col gap-4 rounded-xl bg-cabecalho p-4">
            {erro && <CaixaDeErro>{erro}</CaixaDeErro>}
            <CampoDeSelecao rotulo="Mudar para" value={novaSituacao} onChange={(e) => setNovaSituacao(e.target.value)} obrigatorio>
              <option value="">Escolha a nova situação</option>
              {outras.map((s) => (
                <option key={s.valor} value={s.valor}>
                  {rotuloDaSituacao(s)}
                </option>
              ))}
            </CampoDeSelecao>
            <CampoDeTexto rotulo="Observação" value={observacoes} onChange={(e) => setObservacoes(e.target.value)} placeholder="Motivo da mudança" />
          </form>
        )}
      </div>
    </Modal>
  );
}
