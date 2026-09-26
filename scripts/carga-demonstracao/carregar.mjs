// Carregador da carga de demonstração.
//
//   node carregar.mjs <cliente> [modulo ...]
//
// Sem módulos, roda todos, na ordem do prefixo do arquivo (modulos/10-..., 20-...). Cada módulo exporta
//   export const descricao = '...';
//   export async function carregar(ctx) { ... }
// e usa o contexto montado aqui: a API, o banco, o mundo da demonstração, os códigos já resolvidos e o token de quem
// age em cada passo.

import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { cliente as lerCliente, criarApi, criarBanco, ENV, ErroDaApi, PASTA } from './comum.mjs';
import * as mundo from './mundo.mjs';

const args = process.argv.slice(2);
const [identificador, ...filtro] = args;
if (!identificador) {
  console.error('Uso: node carregar.mjs <cliente> [modulo ...]   (o cliente é um dos identificadores de CLIENTES no .env)');
  process.exit(1);
}

const dadosDoCliente = lerCliente(identificador);
const api = criarApi(dadosDoCliente);
const banco = criarBanco(dadosDoCliente);
const senha = ENV.CARGA_SENHA_DEMONSTRACAO;
if (!senha || senha.length < 6) {
  console.error('Defina CARGA_SENHA_DEMONSTRACAO no .env (pelo menos 6 caracteres): é a senha de todos os acessos da demonstração.');
  process.exit(1);
}

// ---------------------------------------------------------------- acesso do administrador geral

async function tokenDoAdministrador() {
  const arquivo = path.join(PASTA, `token-${identificador}.txt`);
  if (fs.existsSync(arquivo)) return fs.readFileSync(arquivo, 'utf8').trim();
  const email = ENV.CARGA_ADMIN_EMAIL || ENV.ADMINISTRADOR_INICIAL_EMAIL;
  const senhaAdmin = ENV.CARGA_ADMIN_SENHA || ENV.ADMINISTRADOR_INICIAL_SENHA;
  if (!email || !senhaAdmin) {
    throw new Error(`Sem acesso de administrador geral: grave o token em token-${identificador}.txt ou defina `
      + 'CARGA_ADMIN_EMAIL e CARGA_ADMIN_SENHA (ou ADMINISTRADOR_INICIAL_*) no .env.');
  }
  return (await api.post(null, '/auth/login', { email, senha: senhaAdmin })).token;
}

// ---------------------------------------------------------------- contexto

const tokens = new Map();
const ctx = {
  cliente: dadosDoCliente,
  api,
  banco,
  mundo,
  senha,
  admin: null,
  ids: {
    condominio: new Map(), // chave do condomínio → código
    unidade: new Map(), // chave da unidade → código
    pessoa: new Map(), // e-mail → código da pessoa
    area: new Map(), // `${condominio}|${nome}` → área (resposta da API)
  },

  /** Token de uma pessoa da demonstração, que entra com a senha da demonstração. */
  async token(pessoa) {
    if (!tokens.has(pessoa.email)) {
      tokens.set(pessoa.email, api.post(null, '/auth/login', { email: pessoa.email, senha }).then((r) => r.token));
    }
    try {
      return await tokens.get(pessoa.email);
    } catch (e) {
      tokens.delete(pessoa.email);
      throw e;
    }
  },

  /** Código de cada condomínio da demonstração que já existe na API. */
  async resolverCondominios() {
    const existentes = await api.get(ctx.admin, '/condominios?incluirInativos=true');
    for (const c of mundo.CONDOMINIOS) {
      const achado = existentes.find((e) => e.nome === c.dados.nome);
      if (achado) ctx.ids.condominio.set(c.chave, achado.id);
    }
    return ctx.ids.condominio;
  },

  async resolverUnidades() {
    await ctx.resolverCondominios();
    for (const c of mundo.CONDOMINIOS) {
      const id = ctx.ids.condominio.get(c.chave);
      if (!id) continue;
      for (const u of await api.todas(ctx.admin, `/unidades?condominioId=${id}&incluirInativas=true`)) {
        ctx.ids.unidade.set(mundo.chaveDaUnidade(c.chave, u), u.id);
      }
    }
    return ctx.ids.unidade;
  },

  /** Código de cada pessoa da demonstração já cadastrada, pelos ocupantes e pelos acessos dos condomínios. */
  async resolverPessoas() {
    await ctx.resolverCondominios();
    for (const id of ctx.ids.condominio.values()) {
      for (const o of await api.todas(ctx.admin, `/ocupantes?condominioId=${id}`)) ctx.ids.pessoa.set(o.email, o.pessoaId);
      for (const a of await api.todas(ctx.admin, `/usuarios?condominioId=${id}`)) ctx.ids.pessoa.set(a.pessoaEmail, a.pessoaId);
    }
    return ctx.ids.pessoa;
  },

  async resolverAreas() {
    await ctx.resolverCondominios();
    for (const c of mundo.CONDOMINIOS) {
      const id = ctx.ids.condominio.get(c.chave);
      if (!id) continue;
      for (const a of await api.todas(ctx.admin, `/areas-comuns?condominioId=${id}`)) ctx.ids.area.set(`${c.chave}|${a.nome}`, a);
    }
    return ctx.ids.area;
  },

  /** Contadores por módulo: incluídos, já existentes, pulados e recusados. */
  placar: null,
  incluido: (n = 1) => { ctx.placar.incluidos += n; },
  existente: (n = 1) => { ctx.placar.existentes += n; },
  recusado(erro, onde) {
    ctx.placar.recusados++;
    if (ctx.placar.recusados <= 8) console.log(`    <<< ${onde}: ${erro instanceof ErroDaApi ? erro.message : erro.stack ?? erro}`);
  },
  log: (texto) => console.log(`    ${texto}`),
};

// ---------------------------------------------------------------- módulos

const arquivos = fs.readdirSync(path.join(PASTA, 'modulos')).filter((f) => f.endsWith('.mjs')).sort();
const modulos = [];
for (const f of arquivos) {
  const nome = f.replace('.mjs', '').replace(/^\d+-/, '');
  if (filtro.length && !filtro.includes(nome)) continue;
  modulos.push({ nome, ...(await import(pathToFileURL(path.join(PASTA, 'modulos', f)).href)) });
}
if (filtro.length && modulos.length !== filtro.length) {
  console.error(`Módulo desconhecido. Módulos: ${arquivos.map((f) => f.replace('.mjs', '').replace(/^\d+-/, '')).join(', ')}`);
  process.exit(1);
}

const inicio = Date.now();
console.log(`Carga de demonstração em "${identificador}" (banco ${dadosDoCliente.banco}, API ${dadosDoCliente.api}, endereço ${dadosDoCliente.host})`);
ctx.admin = await tokenDoAdministrador();
let comProblema = false;
for (const m of modulos) {
  ctx.placar = { incluidos: 0, existentes: 0, recusados: 0 };
  const t0 = Date.now();
  console.log(`\n== ${m.nome}: ${m.descricao}`);
  try {
    await m.carregar(ctx);
  } catch (e) {
    ctx.recusado(e, 'módulo interrompido');
    comProblema = true;
  }
  const { incluidos, existentes, recusados } = ctx.placar;
  if (recusados) comProblema = true;
  console.log(`   +${incluidos} incluídos  =${existentes} já existiam  x${recusados} recusados  (${((Date.now() - t0) / 1000).toFixed(1)} s)${recusados ? '  <<<' : ''}`);
}
console.log(`\nFim: ${api.chamadas} chamadas à API em ${((Date.now() - inicio) / 1000).toFixed(0)} s.${comProblema ? ' Houve recusas, marcadas com <<<.' : ''}`);
process.exit(comProblema ? 1 : 0);
