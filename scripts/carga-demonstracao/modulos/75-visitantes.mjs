// Visitantes dos últimos 60 dias, registrados pelo porteiro do turno: parentes e amigos, técnicos, entregadores de
// móveis, e os prestadores de toda semana (diaristas, personal, cuidadoras). Quem entrou hoje e ainda não saiu fica
// "no condomínio". A API grava a entrada e a saída com a hora do momento; a carga as leva no banco para a hora
// planejada. Só carrega num condomínio que ainda não tem nenhum visitante.

import { celular, cpf, emParalelo, HOJE, iso, momento, nomeCompleto, noHorario, q, somarDias, somarMinutos, sorteio } from '../comum.mjs';
import { porteiroDoTurno } from './70-encomendas.mjs';

export const descricao = 'visitantes da portaria (últimos 60 dias)';

const DIAS = 60;
const POR_DIA = { palmeiras: 7, mirante: 3, villaggio: 6 };
const FREQUENTES = { palmeiras: 10, mirante: 4, villaggio: 9 };
const FATOR_DO_DIA = [1.3, 0.9, 0.9, 0.9, 1, 1.1, 1.4]; // domingo a sábado
const SEMANA = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado'];

const EVENTUAIS = [
  // [peso, observação, hora mínima, hora máxima, minutos mínimos, minutos máximos]
  [46, null, 9, 21, 60, 300],
  [12, 'Técnico da operadora de internet', 8, 17, 30, 120],
  [8, 'Entrega e montagem de móveis', 8, 16, 40, 180],
  [7, 'Eletricista chamado pelo morador', 8, 17, 40, 150],
  [6, 'Encanador chamado pelo morador', 8, 17, 40, 150],
  [6, 'Corretor de imóveis com cliente', 9, 18, 20, 60],
  [5, 'Técnico de ar-condicionado', 8, 17, 60, 180],
  [5, 'Entregador de supermercado (subiu com as compras)', 9, 20, 10, 25],
  [5, 'Professor particular', 14, 19, 60, 90],
];

const FUNCOES_FREQUENTES = [
  // [função, dias da semana, hora de entrada, minutos de permanência]
  ['Diarista', 1, 8, 480], ['Diarista', 2, 8, 480], ['Diarista', 4, 8, 480], ['Diarista', 5, 8, 450],
  ['Personal trainer', 2, 6, 60], ['Cuidadora de idosos', 1, 7, 600], ['Babá', 3, 7, 600],
  ['Fisioterapeuta', 4, 15, 60], ['Jardineiro', 6, 7, 240], ['Passeador de cães', 3, 17, 50],
];

function planejar(chave, familias) {
  const s = sorteio(`visitantes|${chave}|${iso(HOJE)}`);
  const agora = new Date();
  const lista = [];
  const visitante = (sexo) => ({
    nome: nomeCompleto(s, sexo), cpf: s.chance(0.55) ? cpf(s) : null, telefone: s.chance(0.6) ? celular(s) : null,
    rg: s.chance(0.15) ? `${s.int(1000000, 6999999)} SSP/GO` : null,
  });
  const incluir = (familia, pessoa, entrada, minutos, observacoes) => {
    if (entrada > agora) return;
    const saida = somarMinutos(entrada, minutos);
    lista.push({ familia, pessoa, entrada, saida: saida > agora ? null : saida, observacoes, autorizou: s.um(familia.adultos) });
  };

  // Prestadores de toda semana, sempre no mesmo dia e horário, para a mesma casa
  const frequentes = Array.from({ length: FREQUENTES[chave] }, () => {
    const [funcao, diaDaSemana, horaEntrada, minutos] = s.um(FUNCOES_FREQUENTES.filter(([f]) => chave === 'villaggio' || f !== 'Jardineiro'));
    const sexo = /Diarista|Cuidadora|Babá/.test(funcao) ? 'F' : s.chance(0.5) ? 'F' : 'M';
    return {
      familia: s.um(familias), pessoa: { ...visitante(sexo), cpf: cpf(s) }, diaDaSemana, horaEntrada, minutos,
      observacoes: `${funcao} — toda ${SEMANA[diaDaSemana]}${diaDaSemana === 0 || diaDaSemana === 6 ? '' : '-feira'}`,
    };
  });

  const conhecidos = new Map(); // unidade → parentes e amigos que já vieram
  for (let d = -DIAS; d <= 0; d++) {
    const dia = somarDias(HOJE, d);
    for (const f of frequentes) {
      if (dia.getDay() !== f.diaDaSemana || s.chance(0.08)) continue; // de vez em quando falta
      incluir(f.familia, f.pessoa, noHorario(dia, f.horaEntrada, s.int(0, 25)), f.minutos + s.int(-30, 30), f.observacoes);
    }
    const quantos = Math.round(POR_DIA[chave] * FATOR_DO_DIA[dia.getDay()] * (0.7 + s.r() * 0.6));
    for (let i = 0; i < quantos; i++) {
      const [, observacoes, hMin, hMax, mMin, mMax] = s.pesado(EVENTUAIS.map((e) => [e, e[0]]));
      const familia = s.um(familias);
      // Parente ou amigo que volta outras vezes aparece com o mesmo nome e documento
      const daCasa = conhecidos.get(familia.unidade.chave) ?? [];
      let pessoa;
      if (!observacoes && daCasa.length && s.chance(0.45)) pessoa = s.um(daCasa);
      else {
        pessoa = visitante(s.chance(0.5) ? 'F' : 'M');
        if (!observacoes) conhecidos.set(familia.unidade.chave, [...daCasa, pessoa]);
      }
      incluir(familia, pessoa, noHorario(dia, s.int(hMin, hMax), s.int(0, 59)), s.int(mMin, mMax), observacoes);
    }
  }
  return lista;
}

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverUnidades();
  await ctx.resolverPessoas();

  for (const c of mundo.CONDOMINIOS) {
    const condominioId = ctx.ids.condominio.get(c.chave);
    if (!condominioId) continue;
    const totais = await api.get(ctx.admin, `/visitantes/totais?condominioId=${condominioId}`);
    if (totais.TOTAL > 0) {
      ctx.log(`${c.dados.nome}: já tem ${totais.TOTAL} visitantes; nada a fazer`);
      ctx.existente(totais.TOTAL);
      continue;
    }
    const porteiros = mundo.GESTAO[c.chave].porteiros;
    const plano = planejar(c.chave, mundo.familiasDo(c.chave));
    const ajustes = [];
    await emParalelo(plano, 6, async (v) => {
      try {
        const porteiro = await ctx.token(porteiroDoTurno(c.chave, porteiros, v.entrada));
        const criado = await api.post(porteiro, '/visitantes', {
          nome: v.pessoa.nome, cpf: v.pessoa.cpf, rg: v.pessoa.rg, telefone: v.pessoa.telefone,
          unidadeId: ctx.ids.unidade.get(v.familia.unidade.chave), moradorId: ctx.ids.pessoa.get(v.autorizou.email),
          observacoes: v.observacoes,
        });
        if (v.saida) await api.post(await ctx.token(porteiroDoTurno(c.chave, porteiros, v.saida)), `/visitantes/${criado.id}/saida`);
        ajustes.push(`update gc_visitante set vis_dt_entrada = ${q(momento(v.entrada))}, vis_dt_saida = ${v.saida ? q(momento(v.saida)) : 'NULL'},
          vis_dt_cadastro = ${q(momento(v.entrada))}, vis_dt_atualizacao = ${q(momento(v.saida ?? v.entrada))} where vis_cod = ${criado.id};`);
        ctx.incluido();
      } catch (erro) {
        ctx.recusado(erro, `visitante de ${iso(v.entrada)} (${c.chave})`);
      }
    });
    if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
    ctx.log(`${c.dados.nome}: ${plano.length} visitas, ${plano.filter((v) => !v.saida).length} ainda no condomínio`);
  }
}
