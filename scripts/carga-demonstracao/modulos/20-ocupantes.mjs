// Quem ocupa cada unidade: proprietários (moradores ou não), inquilinos, cônjuges e dependentes, alguns inquilinos
// antigos que já saíram e as duas lojas alugadas por empresas. O que já existe (pela unidade e pelo e-mail) fica.

import { emParalelo, q } from '../comum.mjs';

export const descricao = 'ocupantes das unidades';

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverUnidades();

  const existentes = new Set();
  for (const id of ctx.ids.condominio.values()) {
    for (const o of await api.todas(ctx.admin, `/ocupantes?condominioId=${id}`)) {
      existentes.add(`${o.unidadeId}|${o.email}`);
      ctx.ids.pessoa.set(o.email, o.pessoaId);
    }
  }

  const faltam = [];
  for (const o of mundo.OCUPANTES) {
    const unidadeId = ctx.ids.unidade.get(o.unidade.chave);
    if (!unidadeId) continue;
    if (existentes.has(`${unidadeId}|${o.pessoa.email}`)) ctx.existente();
    else faltam.push({ ...o, unidadeId });
  }

  // Uma pessoa com várias unidades (os investidores) é cadastrada na primeira e só ganha o vínculo nas outras:
  // em paralelo, as duas primeiras chamadas tentariam criar a mesma pessoa
  const primeiras = [];
  const depois = [];
  const vistas = new Set();
  for (const o of faltam) {
    (vistas.has(o.pessoa.email) ? depois : primeiras).push(o);
    vistas.add(o.pessoa.email);
  }

  const ajustes = [];
  const cadastrar = async (o) => {
    try {
      const criado = await api.post(ctx.admin, '/ocupantes', {
        cpfCnpj: o.pessoa.cpfCnpj, tipoPessoa: o.pessoa.tipoPessoa, nome: o.pessoa.nome, email: o.pessoa.email,
        telefone: o.pessoa.telefone, unidadeId: o.unidadeId, vinculo: o.vinculo, inicioOcupacao: o.inicio,
        fimOcupacao: o.fim,
      });
      ctx.ids.pessoa.set(o.pessoa.email, criado.pessoaId);
      // O cadastro na demonstração é de quando o condomínio entrou no sistema, ou de quando a pessoa chegou
      const condominio = mundo.condominio(o.unidade.chave.split('|')[0]);
      const data = o.inicio > condominio.cadastradoEm ? o.inicio : condominio.cadastradoEm;
      ajustes.push(`update gc_ocupante set ocu_dt_cadastro = ${q(data + ' 11:00:00')} where ocu_cod = ${criado.id};`);
      ajustes.push(`update gc_pessoa set pes_dt_cadastro = ${q(data + ' 11:00:00')} where pes_cod = ${criado.pessoaId} and pes_dt_cadastro > ${q(data + ' 11:00:00')};`);
      ctx.incluido();
    } catch (e) {
      ctx.recusado(e, `${o.pessoa.nome} (${o.unidade.chave})`);
    }
  };
  await emParalelo(primeiras, 6, cadastrar);
  await emParalelo(depois, 6, cadastrar);

  if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
}
