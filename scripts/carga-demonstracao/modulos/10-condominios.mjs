// Os três condomínios e as unidades de cada um. O que já existe (pelo nome do condomínio e pelo bloco e número da
// unidade) fica como está.

import { emParalelo, q } from '../comum.mjs';

export const descricao = 'condomínios e unidades';

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverCondominios();

  for (const c of mundo.CONDOMINIOS) {
    if (ctx.ids.condominio.has(c.chave)) {
      ctx.existente();
      continue;
    }
    try {
      const criado = await api.post(ctx.admin, '/condominios', { ...c.dados, numeroUnidades: c.unidades.length });
      ctx.ids.condominio.set(c.chave, criado.id);
      ctx.incluido();
    } catch (e) {
      ctx.recusado(e, c.dados.nome);
    }
  }

  await ctx.resolverUnidades();
  const faltam = mundo.CONDOMINIOS.flatMap((c) => c.unidades
    .filter((u) => ctx.ids.condominio.has(c.chave) && !ctx.ids.unidade.has(u.chave))
    .map((u) => ({ c, u })));
  ctx.existente(mundo.CONDOMINIOS.reduce((t, c) => t + c.unidades.length, 0) - faltam.length);

  await emParalelo(faltam, 6, async ({ c, u }) => {
    try {
      const criada = await api.post(ctx.admin, '/unidades', {
        condominioId: ctx.ids.condominio.get(c.chave), numero: u.numero, bloco: u.bloco, andar: u.andar,
        tipo: u.tipo, statusOcupacao: u.statusOcupacao, fracaoIdeal: u.fracaoIdeal, areaPrivada: u.areaPrivada,
        observacao: u.observacao,
      });
      ctx.ids.unidade.set(u.chave, criada.id);
      ctx.incluido();
    } catch (e) {
      ctx.recusado(e, `${c.dados.nome} ${u.bloco ?? ''} ${u.numero}`);
    }
  });

  // O cadastro tem a data do momento; na demonstração, os condomínios entraram no sistema no começo de 2025
  const sql = mundo.CONDOMINIOS.filter((c) => ctx.ids.condominio.has(c.chave)).map((c) => {
    const id = ctx.ids.condominio.get(c.chave);
    return `update gc_condominio set con_dt_cadastro = ${q(c.cadastradoEm + ' 09:30:00')} where con_cod = ${id};
update gc_unidade set uni_dt_cadastro = ${q(c.cadastradoEm + ' 10:15:00')} where con_cod = ${id};`;
  });
  if (sql.length) ctx.banco.consultar(sql.join('\n'));
}
