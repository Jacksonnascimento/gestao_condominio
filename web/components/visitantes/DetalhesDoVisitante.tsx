'use client';

import { useEffect, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { SeloDoVisitante, estaNoCondominio } from '@/components/visitantes/SeloDoVisitante';
import { Botao, CaixaDeErro, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { formatarDataHora, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';
import { visitanteService, type VisitanteDetalhe, type VisitanteResumo } from '@/services/visitanteService';

/**
 * Tudo sobre a visita. Documentos e observações só aparecem para a administração e a portaria (a API não os manda
 * para o morador). Para quem pode alterar, os botões de editar e de registrar a saída.
 */
export function DetalhesDoVisitante({
  visitante,
  aoFechar,
  aoEditar,
  aoRegistrarSaida,
}: {
  visitante: VisitanteResumo;
  aoFechar: () => void;
  aoEditar: (detalhe: VisitanteDetalhe) => void;
  aoRegistrarSaida: (visitante: VisitanteResumo) => void;
}) {
  const [detalhe, setDetalhe] = useState<VisitanteDetalhe | null>(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let ativa = true;
    visitanteService
      .buscar(visitante.id)
      .then((dados) => {
        if (ativa) setDetalhe(dados);
      })
      .catch((e) => {
        if (ativa) setErro(mensagemErroApi(e, 'Não foi possível carregar os dados do visitante.'));
      });
    return () => {
      ativa = false;
    };
  }, [visitante.id]);

  const dados = detalhe ?? visitante;
  const unidade = rotuloUnidade(dados.unidadeNumero, dados.unidadeBloco);
  const podeAlterar = dados.podeAlterar;

  return (
    <Modal
      titulo={dados.nome}
      subtitulo={unidade ? `Visita à unidade ${unidade}` : undefined}
      aoFechar={aoFechar}
      rodape={
        <>
          <Botao variante={podeAlterar ? 'texto' : 'secundario'} onClick={aoFechar}>
            Fechar
          </Botao>
          {podeAlterar && (
            <Botao onClick={() => detalhe && aoEditar(detalhe)} disabled={!detalhe}>
              Editar
            </Botao>
          )}
          {podeAlterar && estaNoCondominio(dados) && (
            <Botao variante="primario" onClick={() => aoRegistrarSaida(dados)}>
              Registrar saída
            </Botao>
          )}
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div>
          <SeloDoVisitante visitante={dados} />
        </div>
        {erro && <CaixaDeErro>{erro}</CaixaDeErro>}
        <dl className="m-0 divide-y divide-borda-suave">
          <LinhaDeDetalhe rotulo="Unidade" valor={unidade} />
          <LinhaDeDetalhe rotulo="Quem autorizou" valor={dados.moradorNome} />
          <LinhaDeDetalhe rotulo="Telefone" valor={dados.telefone} />
          <LinhaDeDetalhe rotulo="CPF" valor={detalhe?.cpf} />
          <LinhaDeDetalhe rotulo="RG" valor={detalhe?.rg} />
          <LinhaDeDetalhe rotulo="Entrada" valor={formatarDataHora(dados.dataEntrada)} />
          <LinhaDeDetalhe rotulo="Saída" valor={formatarDataHora(dados.dataSaida)} />
          <LinhaDeDetalhe rotulo="Observações" valor={detalhe?.observacoes} />
        </dl>
        {!detalhe && !erro && (
          <p className="flex items-center gap-2 text-sm text-apagado" role="status">
            <LoaderCircle size={16} className="animate-spin" aria-hidden />
            Carregando os dados completos…
          </p>
        )}
      </div>
    </Modal>
  );
}
