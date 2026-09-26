'use client';

import { useEffect, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { SeloDoVisitante, estaNoCondominio } from '@/components/visitantes/SeloDoVisitante';
import { Botao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { formatarDataHora, mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';
import { visitanteService, type VisitanteDetalhe, type VisitanteResumo } from '@/services/visitanteService';

function Linha({ rotulo, valor }: { rotulo: string; valor?: string | null }) {
  if (!valor) return null;
  return (
    <div className="grid grid-cols-[130px_minmax(0,1fr)] gap-3 py-2.5 text-sm">
      <dt className="text-apagado">{rotulo}</dt>
      <dd className="m-0 break-words whitespace-pre-line">{valor}</dd>
    </div>
  );
}

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
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo" role="alert">
            {erro}
          </p>
        )}
        <dl className="m-0 divide-y divide-borda-suave">
          <Linha rotulo="Unidade" valor={unidade} />
          <Linha rotulo="Quem autorizou" valor={dados.moradorNome} />
          <Linha rotulo="Telefone" valor={dados.telefone} />
          <Linha rotulo="CPF" valor={detalhe?.cpf} />
          <Linha rotulo="RG" valor={detalhe?.rg} />
          <Linha rotulo="Entrada" valor={formatarDataHora(dados.dataEntrada)} />
          <Linha rotulo="Saída" valor={formatarDataHora(dados.dataSaida)} />
          <Linha rotulo="Observações" valor={detalhe?.observacoes} />
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
