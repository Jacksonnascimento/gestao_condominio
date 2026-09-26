// Encomendas dos últimos 60 dias, registradas pelo porteiro do turno: Correios, transportadoras, delivery. Quase
// todas já retiradas (o delivery em minutos, as caixas em um ou dois dias), algumas ainda aguardando, poucas
// devolvidas ou extraviadas. Só carrega num condomínio que ainda não tem nenhuma encomenda.

import { emParalelo, HOJE, hora, iso, momento, noHorario, q, somarDias, somarMinutos, sorteio } from '../comum.mjs';

export const descricao = 'encomendas da portaria (últimos 60 dias)';

const DIAS = 60;
const POR_DIA = { palmeiras: 9, mirante: 3, villaggio: 5 };
const FATOR_DO_DIA = [0.25, 1.2, 1, 1, 1, 1, 0.6]; // domingo a sábado

const DESCRICOES = {
  CORREIOS: ['Carta registrada', 'Envelope dos Correios', 'Caixa pequena — Sedex', 'Pacote — PAC', 'Revista por assinatura', 'Cartão de banco'],
  TRANSPORTADORA: ['Caixa — Mercado Livre', 'Caixa — Amazon', 'Pacote — Shopee', 'Caixa grande — Magazine Luiza', 'Pacote — Shein',
    'Caixa — Natura', 'Caixa de vinhos', 'Caixa grande — eletrodoméstico', 'Pacote — Americanas', 'Caixa — farmácia online'],
  DELIVERY: ['Farmácia', 'Mercado — 3 sacolas', 'Restaurante', 'Padaria', 'Floricultura', 'Água mineral — 2 galões', 'Açaí'],
  OUTROS: ['Chave deixada por prestador de serviço', 'Documento da imobiliária', 'Controle novo do portão', 'Sacola deixada por um vizinho'],
};

/** Porteiro do turno, pela hora (e, no Palmeiras, a folguista nos fins de semana). */
export function porteiroDoTurno(chave, porteiros, quando) {
  const h = quando.getHours();
  const fds = quando.getDay() === 0 || quando.getDay() === 6;
  if (chave === 'palmeiras') return fds && h >= 7 && h < 19 ? porteiros[3] : porteiros[h >= 6 && h < 14 ? 0 : h >= 14 && h < 22 ? 1 : 2];
  if (chave === 'mirante') return porteiros[h >= 7 && h < 19 ? 0 : 1];
  return porteiros[h >= 6 && h < 14 ? 0 : h >= 14 && h < 22 ? 1 : 2];
}

function planejar(chave, familias) {
  const s = sorteio(`encomendas|${chave}|${iso(HOJE)}`);
  const agora = new Date();
  const lista = [];
  for (let d = -DIAS; d <= 0; d++) {
    const dia = somarDias(HOJE, d);
    const quantas = Math.round(POR_DIA[chave] * FATOR_DO_DIA[dia.getDay()] * (0.7 + s.r() * 0.6));
    for (let i = 0; i < quantas; i++) {
      const tipo = s.pesado([['CORREIOS', 26], ['TRANSPORTADORA', 42], ['DELIVERY', 22], ['OUTROS', 10]]);
      const recebida = tipo === 'DELIVERY'
        ? noHorario(dia, s.chance(0.5) ? s.int(11, 13) : s.int(18, 21), s.int(0, 59))
        : noHorario(dia, s.int(8, 19), s.int(0, 59));
      if (recebida > agora) continue;
      const familia = s.um(familias);
      const destinatario = s.chance(0.85) ? s.um(familia.adultos) : s.um(familia.moradores);
      let retirada = tipo === 'DELIVERY'
        ? somarMinutos(recebida, s.int(4, 35))
        : noHorario(somarDias(dia, s.pesado([[0, 40], [1, 30], [2, 15], [s.int(3, 6), 12], [s.int(10, 16), 3]])), s.int(7, 22), s.int(0, 59));
      if (retirada <= recebida) retirada = somarMinutos(recebida, s.int(30, 180));
      let status = retirada <= agora ? 'RETIRADA' : 'PENDENTE';
      if (tipo !== 'DELIVERY' && d < -12) {
        status = s.pesado([['RETIRADA', 984], ['DEVOLVIDA', 12], ['EXTRAVIADA', 4]]);
      }
      lista.push({
        familia, tipo, recebida, retirada, status,
        destinatario: destinatario.nome,
        descricao: s.um(DESCRICOES[tipo]),
        quemRetirou: s.chance(0.08) ? `${s.um(['Maria', 'Cida', 'Rose', 'Lúcia'])} (diarista, autorizada)` : s.um(familia.adultos).nome,
        observacoes: tipo === 'TRANSPORTADORA' && s.chance(0.08) ? 'Caixa com a embalagem amassada; morador avisado.' : null,
        mudancaEm: noHorario(somarDias(dia, s.int(7, 10)), s.int(9, 17), s.int(0, 59)),
      });
    }
  }
  return lista;
}

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverUnidades();

  for (const c of mundo.CONDOMINIOS) {
    const condominioId = ctx.ids.condominio.get(c.chave);
    if (!condominioId) continue;
    const totais = await api.get(ctx.admin, `/encomendas/totais?condominioId=${condominioId}`);
    if (totais.TOTAL > 0) {
      ctx.log(`${c.dados.nome}: já tem ${totais.TOTAL} encomendas; nada a fazer`);
      ctx.existente(totais.TOTAL);
      continue;
    }
    const porteiros = mundo.GESTAO[c.chave].porteiros;
    const plano = planejar(c.chave, mundo.familiasDo(c.chave));
    const ajustes = [];
    await emParalelo(plano, 6, async (e) => {
      try {
        const quemRecebeu = porteiroDoTurno(c.chave, porteiros, e.recebida);
        const criada = await api.post(await ctx.token(quemRecebeu), '/encomendas', {
          condominioId, unidadeId: ctx.ids.unidade.get(e.familia.unidade.chave), destinatario: e.destinatario,
          tipo: e.tipo, descricao: e.descricao, dataRecebimento: iso(e.recebida), horaRecebimento: hora(e.recebida),
          nomeRecebidoPor: quemRecebeu.nome, observacoes: e.observacoes,
        });
        if (e.status === 'RETIRADA') {
          await api.post(await ctx.token(porteiroDoTurno(c.chave, porteiros, e.retirada)), `/encomendas/${criada.id}/retirada`, {
            dataRetirada: iso(e.retirada), horaRetirada: hora(e.retirada), nomeRetirada: e.quemRetirou,
          });
          ajustes.push(`update gc_encomenda set enc_dt_atualizacao_status = ${q(momento(e.retirada))} where enc_cod = ${criada.id};`);
        } else if (e.status !== 'PENDENTE') {
          await api.put(await ctx.token(porteiroDoTurno(c.chave, porteiros, e.mudancaEm)), `/encomendas/${criada.id}/status`, {
            novoStatus: e.status,
            observacoes: e.status === 'DEVOLVIDA'
              ? 'Não retirada no prazo de 7 dias; devolvida ao remetente.'
              : 'Não localizada na sala de encomendas; a transportadora foi acionada.',
          });
          ajustes.push(`update gc_encomenda set enc_dt_atualizacao_status = ${q(momento(e.mudancaEm))} where enc_cod = ${criada.id};`);
        }
        ctx.incluido();
      } catch (erro) {
        ctx.recusado(erro, `encomenda de ${iso(e.recebida)} (${c.chave})`);
      }
    });
    if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
    ctx.log(`${c.dados.nome}: ${plano.length} encomendas`);
  }
}
