// Reservas das áreas comuns: seis meses de histórico (concluídas, canceladas, rejeitadas) e as próximas semanas
// (aprovadas e aguardando aprovação). Os fins de semana lotam o salão e as churrasqueiras; a quadra enche à noite.
//
// Tudo passa pela API: o morador pede, o síndico aprova ou rejeita, o morador cancela. Como a API só aceita reservas
// dentro da antecedência da área, cada reserva do histórico é pedida numa data livre do futuro e, depois de aprovada,
// rejeitada ou cancelada, levada no banco para a data planejada, com a situação de concluída quando foi aprovada.
// Só carrega num condomínio que ainda não tem nenhuma reserva.

import { diasEntre, emParalelo, HOJE, iso, momento, nomeCompleto, noHorario, q, somarDias, sorteio } from '../comum.mjs';

export const descricao = 'reservas das áreas comuns (histórico e próximas semanas)';

const AGORA = new Date();
const DIAS_DE_HISTORICO = 182;
const DIAS_A_FRENTE = 45;

const MOTIVOS_DE_REJEICAO = [
  'A área estará interditada nesta data para a dedetização trimestral.',
  'A unidade tem taxa condominial em atraso; depois de regularizar, é só pedir de novo.',
  'Pedido para evento com buffet precisa da lista do fornecedor com cinco dias de antecedência.',
  'Já há um evento do condomínio marcado para esta data.',
  'O número de convidados informado passa da capacidade da área.',
];

/** Chance de alguém reservar o turno no dia, pelo tipo de área e pelo dia da semana. */
function procura(area, turno, dia) {
  const semana = dia.getDay(); // 0 domingo, 6 sábado
  const nome = area.nome.toLowerCase();
  const inicio = turno ? Number(turno.horaInicio.slice(0, 2)) : 12;
  if (nome.includes('quadra') || nome.includes('campo')) {
    if (semana === 0 || semana === 6) return inicio < 12 ? 0.35 : 0.15;
    return inicio >= 18 ? 0.25 : inicio >= 16 ? 0.12 : 0.04;
  }
  if (nome.includes('salão')) {
    if (semana === 6) return turno ? (inicio >= 18 ? 0.5 : 0.25) : 0.6;
    if (semana === 0) return turno ? (inicio >= 18 ? 0.15 : 0.3) : 0.35;
    if (semana === 5) return turno ? (inicio >= 18 ? 0.25 : 0.03) : 0.2;
    return 0.03;
  }
  const fator = nome.includes('cobertura') ? 0.7 : 1;
  if (semana === 6 || semana === 0) return fator * (turno ? (inicio < 14 ? 0.6 : 0.35) : 0.4);
  if (semana === 5) return fator * (turno ? (inicio >= 16 ? 0.3 : 0.05) : 0.25);
  return fator * 0.05;
}

const semanaDoAno = (d) => `${d.getFullYear()}-${Math.floor(diasEntre(new Date(d.getFullYear(), 0, 1), d) / 7)}`;

function planejar(ctx, chave, area) {
  const s = sorteio(`reservas|${chave}|${area.nome}|${iso(HOJE)}`);
  const familias = ctx.mundo.familiasComAcesso(chave);
  const esporte = /quadra|campo/i.test(area.nome);
  const usoNaSemana = new Map();
  const turnos = area.turnos.filter((t) => t.ativo !== false);
  const min = area.diasAntecedenciaMin ?? 0;
  const max = area.diasAntecedenciaMax ?? 60;
  const plano = [];

  const escolherFamilia = (dia) => {
    for (let tentativa = 0; tentativa < 6; tentativa++) {
      const f = s.um(familias);
      const chaveSemana = `${f.unidade.chave}|${semanaDoAno(dia)}`;
      if (!esporte || (usoNaSemana.get(chaveSemana) ?? 0) < 2) {
        usoNaSemana.set(chaveSemana, (usoNaSemana.get(chaveSemana) ?? 0) + 1);
        return f;
      }
    }
    return s.um(familias);
  };

  const nova = (dia, turno, passado) => {
    const familia = escolherFamilia(dia);
    const distancia = diasEntre(HOJE, dia);
    let status;
    if (passado) status = s.pesado([['CONCLUIDA', 82], ['CANCELADA_PELO_MORADOR', 11], ['REJEITADA', 7]]);
    else if (distancia <= 2) status = s.pesado([['APROVADA', 85], ['CANCELADA_PELO_MORADOR', 10], ['REJEITADA', 5]]);
    else status = s.pesado([['APROVADA', 50], ['PENDENTE_APROVACAO', 35], ['CANCELADA_PELO_MORADOR', 8], ['REJEITADA', 7]]);
    // Pedido feito entre a antecedência mínima e um mês antes da data, e nunca depois de hoje
    let antes = s.int(Math.max(min, esporte ? 0 : 1), Math.max(Math.min(max, esporte ? 6 : 35), Math.max(min, 1)));
    if (distancia - antes > 0) antes = distancia + s.int(0, 2);
    antes = Math.min(Math.max(antes, min), max);
    let criadaEm = noHorario(somarDias(dia, -antes), s.int(7, 22), s.int(0, 59));
    if (criadaEm > AGORA) criadaEm = new Date(AGORA.getTime() - s.int(20, 300) * 60000);
    const convidados = area.permiteConvidados && s.chance(esporte ? 0.25 : 0.45)
      ? Array.from({ length: s.int(2, Math.min(area.limiteConvidados ?? 15, esporte ? 8 : 15)) }, () => {
        const sexo = s.chance(0.5) ? 'F' : 'M';
        return { nome: nomeCompleto(s, sexo), documento: s.chance(0.3) ? String(s.int(1000000, 9999999)) : undefined };
      })
      : [];
    plano.push({
      chave, area, dia, turno, familia, morador: familia.comAcesso[0], status, passado, criadaEm, convidados,
      motivo: status === 'REJEITADA' ? s.um(MOTIVOS_DE_REJEICAO) : null,
    });
  };

  for (let d = -DIAS_DE_HISTORICO; d <= Math.min(max, DIAS_A_FRENTE); d++) {
    if (d >= 0 && d < min) continue;
    if (d === 0 && min === 0 && !esporte) continue;
    const dia = somarDias(HOJE, d);
    const passado = d < 0;
    // Quanto mais longe, menos gente já reservou
    const fator = passado ? 1 : Math.max(0.25, 1 - d / 60);
    if (!turnos.length) {
      if (s.chance(procura(area, null, dia) * fator)) nova(dia, null, passado);
      continue;
    }
    // Às vezes alguém reserva o dia inteiro, e aí os turnos não ficam livres
    if (!esporte && s.chance(procura(area, null, dia) * fator * 0.12)) { nova(dia, null, passado); continue; }
    for (const t of turnos) {
      if (s.chance(procura(area, t, dia) * fator)) nova(dia, t, passado);
    }
  }
  return plano;
}

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverAreas();
  await ctx.resolverUnidades();

  for (const c of mundo.CONDOMINIOS) {
    const condominioId = ctx.ids.condominio.get(c.chave);
    if (!condominioId) continue;
    const totais = await api.get(ctx.admin, `/reservas/totais?condominioId=${condominioId}`);
    if (totais.TOTAL > 0) {
      ctx.log(`${c.dados.nome}: já tem ${totais.TOTAL} reservas; nada a fazer`);
      ctx.existente(totais.TOTAL);
      continue;
    }
    const sindico = mundo.GESTAO[c.chave].sindico;
    const areas = [...ctx.ids.area.entries()].filter(([k, a]) => k.startsWith(`${c.chave}|`) && a.ativa).map(([, a]) => a);

    for (const area of areas) {
      const plano = planejar(ctx, c.chave, area);
      const futuras = plano.filter((r) => !r.passado);
      const antigas = plano.filter((r) => r.passado);
      const ocupado = new Set(futuras.map((r) => `${iso(r.dia)}|${r.turno?.codigo ?? 'dia'}`));
      const ajustes = [];

      const pedir = async (r, data) => {
        try {
          const tokenMorador = await ctx.token(r.morador);
          const criada = await api.post(tokenMorador, '/reservas', {
            areaId: area.codigo, turnoId: r.turno?.codigo, unidadeId: ctx.ids.unidade.get(r.familia.unidade.chave),
            data: iso(data), termosAceitos: true, convidados: r.convidados,
          });
          if (r.status === 'APROVADA' || r.status === 'CONCLUIDA') await api.post(await ctx.token(sindico), `/reservas/${criada.codigo}/aprovar`);
          else if (r.status === 'REJEITADA') await api.post(await ctx.token(sindico), `/reservas/${criada.codigo}/rejeitar`, { motivo: r.motivo });
          else if (r.status === 'CANCELADA_PELO_MORADOR') await api.post(tokenMorador, `/reservas/${criada.codigo}/cancelar`);
          let atualizada = r.status === 'CONCLUIDA'
            ? noHorario(somarDias(r.dia, 1), 0, 5)
            : r.status === 'PENDENTE_APROVACAO' ? r.criadaEm : noHorario(somarDias(r.criadaEm, 1), 10, 20);
          if (atualizada > AGORA) atualizada = AGORA;
          ajustes.push(`update gc_reserva set res_data = ${q(iso(r.dia))}, res_status = ${q(r.status)},
            res_dt_registro = ${q(momento(r.criadaEm))}, res_dt_atualizacao = ${q(momento(atualizada))} where res_cod = ${criada.codigo};`);
          ctx.incluido();
        } catch (e) {
          ctx.recusado(e, `reserva de ${area.nome} em ${iso(r.dia)} (${c.chave})`);
        }
      };
      const gravar = () => { if (ajustes.length) ctx.banco.consultar(ajustes.splice(0).join('\n')); };

      // As próximas semanas entram direto, na data certa
      await emParalelo(futuras, 4, (r) => pedir(r, r.dia));
      gravar();

      // O histórico entra em lotes, cada um numa data livre dentro da antecedência da área
      const janela = [];
      for (let d = Math.max(area.diasAntecedenciaMin ?? 0, 1); d <= (area.diasAntecedenciaMax ?? 60); d++) janela.push(iso(somarDias(HOJE, d)));
      let lote = [];
      let usadas = new Set();
      const livre = (data, turno) => {
        const k = (t) => `${data}|${t}`;
        if (turno) return ![k(turno.codigo), k('dia')].some((x) => ocupado.has(x) || usadas.has(x));
        return ![...ocupado, ...usadas].some((x) => x.startsWith(`${data}|`));
      };
      const esvaziar = async () => {
        await emParalelo(lote, 4, ({ r, data }) => pedir(r, new Date(`${data}T12:00:00`)));
        gravar();
        lote = [];
        usadas = new Set();
      };
      for (const r of antigas) {
        let data = janela.find((d) => livre(d, r.turno));
        if (!data) {
          await esvaziar();
          data = janela.find((d) => livre(d, r.turno));
          if (!data) { ctx.recusado(new Error('sem data livre para a reserva'), `${area.nome} (${c.chave})`); continue; }
        }
        usadas.add(`${data}|${r.turno?.codigo ?? 'dia'}`);
        lote.push({ r, data });
      }
      await esvaziar();
      ctx.log(`${c.dados.nome} · ${area.nome}: ${antigas.length} no histórico, ${futuras.length} nas próximas semanas`);
    }
  }
}
