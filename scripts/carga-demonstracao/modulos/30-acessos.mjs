// Quem entra no sistema: o síndico (um morador), a administradora, o gerente predial, a portaria e os moradores com
// acesso. Todos ficam com a senha de CARGA_SENHA_DEMONSTRACAO; um porteiro que saiu fica com o acesso desativado.
//
// A API só deixa definir a senha na hora para quem ainda não tem cadastro (a portaria e a administração). Para os
// moradores, que já são ocupantes, ela manda o link de definição de senha: a carga lê o link no banco, como faria a
// pessoa ao abrir o e-mail, e define a senha por ele.

import { emParalelo, q } from '../comum.mjs';

export const descricao = 'acessos ao sistema e senhas da demonstração';

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverPessoas();

  const existentes = new Map();
  for (const [chave, id] of ctx.ids.condominio) {
    for (const a of await api.todas(ctx.admin, `/usuarios?condominioId=${id}`)) {
      existentes.set(`${chave}|${a.pessoaEmail}|${a.papel.codigo ?? a.papel}`, a);
    }
  }

  const planejados = mundo.ACESSOS.filter((a) => ctx.ids.condominio.has(a.condominio));
  const faltam = planejados.filter((a) => !existentes.has(`${a.condominio}|${a.pessoa.email}|${a.papel}`));
  ctx.existente(planejados.length - faltam.length);

  // Quem é de fora e ainda não tem cadastro entra com a senha definida agora; uma vez só, no primeiro acesso dele
  const cadastradosAgora = new Set();
  const pedidoDe = (a) => {
    const condominioId = ctx.ids.condominio.get(a.condominio);
    const semCadastro = mundo.PESSOAS_DE_FORA.has(a.pessoa) && !ctx.ids.pessoa.has(a.pessoa.email)
      && !cadastradosAgora.has(a.pessoa.email);
    if (semCadastro) {
      cadastradosAgora.add(a.pessoa.email);
      return {
        condominioId, papel: a.papel, acaoSenha: 'CRIAR_SENHA', cpf: a.pessoa.cpfCnpj, nome: a.pessoa.nome,
        email: a.pessoa.email, telefone: a.pessoa.telefone, senha: ctx.senha,
      };
    }
    if (a.papel === 'MORADOR') return { condominioId, papel: a.papel, pessoaId: ctx.ids.pessoa.get(a.pessoa.email), acaoSenha: 'ENVIAR_LINK' };
    return { condominioId, papel: a.papel, cpf: a.pessoa.cpfCnpj, acaoSenha: 'ENVIAR_LINK' };
  };

  // Em sequência para quem é de fora (a mesma pessoa pode ter acesso a dois condomínios); em paralelo para os demais
  const deFora = faltam.filter((a) => mundo.PESSOAS_DE_FORA.has(a.pessoa));
  const demais = faltam.filter((a) => !mundo.PESSOAS_DE_FORA.has(a.pessoa));
  const ajustes = [];
  const dar = async (a) => {
    try {
      const criado = await api.post(ctx.admin, '/usuarios', pedidoDe(a));
      ctx.ids.pessoa.set(a.pessoa.email, criado.pessoaId);
      existentes.set(`${a.condominio}|${a.pessoa.email}|${a.papel}`, criado);
      ctx.incluido();
    } catch (e) {
      ctx.recusado(e, `${a.papel} ${a.pessoa.nome} (${a.condominio})`);
    }
  };
  for (const a of deFora) await dar(a);
  await emParalelo(demais, 6, dar);

  // Situação e data de início de cada acesso, como na demonstração
  for (const a of planejados) {
    const atual = existentes.get(`${a.condominio}|${a.pessoa.email}|${a.papel}`);
    if (!atual) continue;
    const rota = `/usuarios/${atual.pessoaId}/vinculos/${ctx.ids.condominio.get(a.condominio)}/${a.papel}`;
    if (!a.ativo && atual.ativo !== false) await api.post(ctx.admin, `${rota}/desativar`).catch((e) => ctx.recusado(e, rota));
    ajustes.push(`update gc_usuario_condominio set usc_dt_associacao = ${q(a.desde + ' 14:00:00')}
      where pes_cod = ${atual.pessoaId} and con_cod = ${ctx.ids.condominio.get(a.condominio)} and usc_papel = ${q(a.papel)};`);
  }
  if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));

  // Senhas: quem ainda não entra com a senha da demonstração recebe o link e define a senha por ele
  const comAcessoAtivo = [...new Map(planejados.filter((a) => a.ativo).map((a) => [a.pessoa.email, a.pessoa])).values()];
  const semSenha = [];
  await emParalelo(comAcessoAtivo, 6, async (p) => {
    try {
      await ctx.token(p);
    } catch {
      semSenha.push(p);
    }
  });
  if (!semSenha.length) return;
  ctx.log(`definindo a senha de ${semSenha.length} pessoa(s)`);
  await emParalelo(semSenha, 6, (p) => api.post(ctx.admin, `/usuarios/${ctx.ids.pessoa.get(p.email)}/link-de-senha`)
    .catch((e) => ctx.recusado(e, `link de senha de ${p.nome}`)));
  const links = new Map(ctx.banco.consultar(`select p.pes_email, t.prt_token from gc_password_reset_token t
    join gc_pessoa p on p.pes_cod = t.pes_cod
    where p.pes_email in (${semSenha.map((p) => q(p.email)).join(', ')})`));
  await emParalelo(semSenha, 6, async (p) => {
    try {
      if (!links.has(p.email)) throw new Error('link de senha não encontrado no banco');
      await api.post(null, '/auth/redefinir-senha', { token: links.get(p.email), novaSenha: ctx.senha });
      await ctx.token(p);
    } catch (e) {
      ctx.recusado(e, `senha de ${p.nome}`);
    }
  });
}
