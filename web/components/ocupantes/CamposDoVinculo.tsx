'use client';

import { descricaoDoTipoDePeriodo } from '@/components/ocupantes/SeloDoVinculo';
import { Campo, CampoDeSelecao } from '@/components/Interface';
import { emFrase } from '@/components/unidades/PecasDeCadastro';
import type { OpcoesOcupante } from '@/services/ocupanteService';

export interface DadosDoVinculo {
  vinculo: string;
  inicioOcupacao: string;
  fimOcupacao: string;
  periodoUso: string;
  tipoPeriodo: string;
}

/** Vínculo, período de ocupação e, para multiproprietário, o período de uso. Usado no cadastro e na edição. */
export function CamposDoVinculo({
  opcoes,
  dados,
  aoMudar,
}: {
  opcoes: OpcoesOcupante;
  dados: DadosDoVinculo;
  aoMudar: (dados: DadosDoVinculo) => void;
}) {
  const mudar = (campo: keyof DadosDoVinculo) => (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    aoMudar({ ...dados, [campo]: evento.target.value });
  const multiproprietario = dados.vinculo === 'MULTIPROPRIETARIO';

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <CampoDeSelecao rotulo="Vínculo" value={dados.vinculo} onChange={mudar('vinculo')} obrigatorio className="sm:col-span-2">
        {opcoes.vinculos.map((v) => (
          <option key={v.valor} value={v.valor}>
            {emFrase(v.descricao)}
          </option>
        ))}
      </CampoDeSelecao>
      <Campo rotulo="Início da ocupação" type="date" value={dados.inicioOcupacao} onChange={mudar('inicioOcupacao')} obrigatorio />
      <Campo
        rotulo="Fim da ocupação"
        type="date"
        value={dados.fimOcupacao}
        onChange={mudar('fimOcupacao')}
        min={dados.inicioOcupacao || undefined}
        ajuda="Deixe em branco se não há data para sair."
      />
      {multiproprietario && (
        <>
          <Campo
            rotulo="Período de uso"
            value={dados.periodoUso}
            onChange={mudar('periodoUso')}
            maxLength={100}
            placeholder="Semanas 1 a 4 de janeiro"
          />
          <CampoDeSelecao rotulo="Tipo do período" value={dados.tipoPeriodo} onChange={mudar('tipoPeriodo')}>
            <option value="">Não informado</option>
            {opcoes.tiposPeriodo.map((t) => (
              <option key={t.valor} value={t.valor}>
                {descricaoDoTipoDePeriodo(t.valor, t.descricao)}
              </option>
            ))}
          </CampoDeSelecao>
        </>
      )}
    </div>
  );
}
