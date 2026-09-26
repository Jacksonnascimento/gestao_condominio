'use client';

import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { SeloDoVinculo } from '@/components/ocupantes/SeloDoVinculo';
import { Botao, LinhaDeDetalhe } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { SeloDaUnidade } from '@/components/unidades/SeloDaUnidade';
import { useSessao } from '@/context/SessaoContext';
import { ocupanteService, type OpcoesOcupante, type Ocupante } from '@/services/ocupanteService';
import type { Pagina } from '@/services/tipos';
import type { Unidade } from '@/services/unidadeService';
import {
  descricaoDoEnum,
  emFrase,
  formatarData,
  formatarDataHora,
  formatarNumero,
  mensagemErroApi,
  rotuloUnidade,
} from '@/services/utilitarios';

const LIMITE_DE_OCUPANTES = 50;

/**
 * Tudo sobre a unidade e quem ocupa. Para síndico e administração, também editar, inativar e reativar; para a
 * gestão, adicionar ocupante.
 */
export function DetalhesDaUnidade({
  unidade,
  podeGerenciar,
  aoFechar,
  aoEditar,
  aoInativar,
  aoReativar,
  aoAdicionarOcupante,
}: {
  unidade: Unidade;
  /** Síndico ou administração do condomínio: edita, inativa e reativa. */
  podeGerenciar: boolean;
  aoFechar: () => void;
  aoEditar: () => void;
  aoInativar: () => void;
  aoReativar: () => void;
  aoAdicionarOcupante: (opcoes: OpcoesOcupante) => void;
}) {
  const { permissoes } = useSessao();
  const [ocupantes, setOcupantes] = useState<Pagina<Ocupante> | null>(null);
  const [opcoesDeOcupante, setOpcoesDeOcupante] = useState<OpcoesOcupante | null>(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let ativa = true;
    Promise.all([
      ocupanteService.listar({ unidadeId: unidade.id, tamanho: LIMITE_DE_OCUPANTES }),
      ocupanteService.opcoes(),
    ])
      .then(([lista, opcoes]) => {
        if (!ativa) return;
        setOcupantes(lista);
        setOpcoesDeOcupante(opcoes);
      })
      .catch((e) => {
        if (ativa) setErro(mensagemErroApi(e, 'Não foi possível carregar os ocupantes da unidade.'));
      });
    return () => {
      ativa = false;
    };
  }, [unidade.id]);

  const gerenciaOcupantes = opcoesDeOcupante?.podeGerenciar === true && permissoes.gestao && unidade.ativa;
  const rotulo = rotuloUnidade(unidade.numero, unidade.bloco);
  const itens = ocupantes?.itens ?? [];

  return (
    <Modal
      titulo={`Unidade ${rotulo}`}
      subtitulo={unidade.condominioNome ?? undefined}
      largura="lg"
      aoFechar={aoFechar}
      rodape={
        podeGerenciar ? (
          <>
            {unidade.ativa ? (
              <Botao variante="texto" onClick={aoInativar} className="sm:mr-auto">
                Inativar unidade
              </Botao>
            ) : (
              <Botao variante="secundario" onClick={aoReativar} className="sm:mr-auto">
                Reativar unidade
              </Botao>
            )}
            <Botao variante="secundario" onClick={aoFechar}>
              Fechar
            </Botao>
            {unidade.ativa && (
              <Botao variante="primario" onClick={aoEditar}>
                Editar
              </Botao>
            )}
          </>
        ) : (
          <Botao variante="secundario" onClick={aoFechar}>
            Fechar
          </Botao>
        )
      }
    >
      <div className="flex flex-col gap-6">
        <div>
          <SeloDaUnidade unidade={unidade} />
        </div>
        <dl className="m-0 divide-y divide-borda-suave">
          <LinhaDeDetalhe rotulo="Tipo" valor={emFrase(unidade.tipoDescricao ?? descricaoDoEnum(unidade.tipo))} />
          <LinhaDeDetalhe rotulo="Bloco" valor={unidade.bloco} />
          <LinhaDeDetalhe rotulo="Andar" valor={unidade.andar} />
          <LinhaDeDetalhe rotulo="Área privada" valor={unidade.areaPrivada != null ? `${formatarNumero(unidade.areaPrivada)} m²` : ''} />
          <LinhaDeDetalhe rotulo="Fração ideal" valor={unidade.fracaoIdeal != null ? `${formatarNumero(unidade.fracaoIdeal)}%` : ''} />
          <LinhaDeDetalhe rotulo="Observações" valor={unidade.observacao} />
          <LinhaDeDetalhe rotulo="Atualizada em" valor={formatarDataHora(unidade.dataAtualizacao)} />
        </dl>

        <section aria-labelledby="ocupantes-da-unidade" className="flex flex-col gap-2">
          <div className="flex items-center justify-between gap-3">
            <h3 id="ocupantes-da-unidade" className="text-base font-extrabold">
              Ocupantes
              {ocupantes && <span className="ml-2 text-sm font-semibold text-apagado">{ocupantes.totalItens}</span>}
            </h3>
            {gerenciaOcupantes && opcoesDeOcupante && (
              <Botao pequeno onClick={() => aoAdicionarOcupante(opcoesDeOcupante)}>
                <Plus size={15} aria-hidden />
                Adicionar ocupante
              </Botao>
            )}
          </div>

          {erro && <p className="text-sm text-perigo">{erro}</p>}
          {!erro && !ocupantes && <p className="text-sm text-apagado">Carregando…</p>}
          {ocupantes && itens.length === 0 && <p className="text-sm text-apagado">Ninguém vinculado a esta unidade.</p>}

          {itens.length > 0 && (
            <ul className="m-0 list-none divide-y divide-borda-suave rounded-xl border border-borda-suave p-0">
              {itens.map((ocupante) => (
                <li key={ocupante.id} className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 px-4 py-3 text-sm">
                  <span className="flex min-w-0 flex-col">
                    <span className="font-bold">{ocupante.nome}</span>
                    <span className="text-apagado">
                      {[ocupante.email, ocupante.telefone].filter(Boolean).join(' · ')}
                      {ocupante.inicioOcupacao && `${ocupante.email || ocupante.telefone ? ' · ' : ''}desde ${formatarData(ocupante.inicioOcupacao)}`}
                    </span>
                  </span>
                  <SeloDoVinculo ocupante={ocupante} />
                </li>
              ))}
            </ul>
          )}
          {ocupantes && ocupantes.totalItens > itens.length && (
            <p className="text-xs text-apagado">
              Mostrando {itens.length} de {ocupantes.totalItens}. A lista completa está na tela de ocupantes.
            </p>
          )}
          {!unidade.ativa && podeGerenciar && <p className="text-xs text-apagado">Unidade inativa: reative-a para vincular ocupantes.</p>}
        </section>
      </div>
    </Modal>
  );
}
