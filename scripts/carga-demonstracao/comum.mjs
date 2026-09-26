// Base comum da carga de demonstração: configuração, sorteio reproduzível, datas, documentos válidos, nomes, e o
// acesso à API e ao banco. Tudo é fictício, mas com cara de dado real. O mesmo sorteio gera sempre os mesmos
// registros, para que rodar a carga de novo complete o que falta em vez de duplicar.

import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const PASTA = path.dirname(fileURLToPath(import.meta.url));
const RAIZ = path.resolve(PASTA, '..', '..');

// ---------------------------------------------------------------- configuração

/** Variáveis do .env da raiz do repositório, com as do ambiente por cima. */
function lerEnv() {
  const valores = {};
  const arquivo = path.join(RAIZ, '.env');
  if (fs.existsSync(arquivo)) {
    for (const linha of fs.readFileSync(arquivo, 'utf8').split(/\r?\n/)) {
      const m = /^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/.exec(linha);
      if (m) valores[m[1]] = m[2];
    }
  }
  return { ...valores, ...process.env };
}

export const ENV = lerEnv();

/**
 * Clientes da variável CLIENTES (identificador:banco[:dominio...]). A carga fala com a API pelo endereço de
 * CARGA_API (padrão http://localhost:8080) e diz o cliente no cabeçalho X-Forwarded-Host, como faz o sistema web.
 */
export function cliente(identificador) {
  const lista = (ENV.CLIENTES || '').split(',').map((c) => c.trim()).filter(Boolean);
  const achado = lista.map((c) => c.split(':')).find(([id]) => id === identificador);
  if (!achado) {
    throw new Error(`Cliente "${identificador}" não está em CLIENTES. Clientes: ${lista.map((c) => c.split(':')[0]).join(', ') || 'nenhum'}.`);
  }
  return {
    identificador,
    banco: achado[1],
    api: (ENV.CARGA_API || 'http://localhost:8080').replace(/\/$/, ''),
    host: ENV.CARGA_HOST ? ENV.CARGA_HOST.replace('{cliente}', identificador) : `${identificador}.localhost`,
  };
}

// ---------------------------------------------------------------- sorteio

function hash(texto) {
  let h = 1779033703 ^ texto.length;
  for (let i = 0; i < texto.length; i++) {
    h = Math.imul(h ^ texto.charCodeAt(i), 3432918353);
    h = (h << 13) | (h >>> 19);
  }
  return h >>> 0;
}

/** Sorteio reproduzível a partir de uma semente em texto. */
export function sorteio(semente) {
  let a = hash(String(semente));
  const r = () => {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  return {
    r,
    /** inteiro entre min e max, inclusive */
    int: (min, max) => min + Math.floor(r() * (max - min + 1)),
    valor: (min, max) => Math.round((min + r() * (max - min)) * 100) / 100,
    um: (lista) => lista[Math.floor(r() * lista.length)],
    alguns: (lista, n) => {
      const copia = [...lista];
      const saida = [];
      while (saida.length < n && copia.length) saida.push(copia.splice(Math.floor(r() * copia.length), 1)[0]);
      return saida;
    },
    chance: (p) => r() < p,
    /** sorteio ponderado: [[item, peso], ...] */
    pesado: (pares) => {
      const total = pares.reduce((s, [, p]) => s + p, 0);
      let x = r() * total;
      for (const [item, p] of pares) { if ((x -= p) < 0) return item; }
      return pares[pares.length - 1][0];
    },
  };
}

// ---------------------------------------------------------------- datas

/** Hoje, à meia-noite. As datas da carga são relativas a ele: o histórico termina hoje e o futuro começa amanhã. */
export const HOJE = (() => { const d = new Date(); d.setHours(0, 0, 0, 0); return d; })();

const p2 = (n) => String(n).padStart(2, '0');
/** 'YYYY-MM-DD' */
export const iso = (d) => `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`;
/** 'HH:MM' */
export const hora = (d) => `${p2(d.getHours())}:${p2(d.getMinutes())}`;
/** 'YYYY-MM-DD HH:MM:SS', para o banco */
export const momento = (d) => `${iso(d)} ${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
export const somarDias = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
export const somarMinutos = (d, n) => new Date(d.getTime() + n * 60000);
export const noHorario = (d, h, m = 0) => { const x = new Date(d); x.setHours(h, m, Math.floor((m * 7) % 60), 0); return x; };
export const diasEntre = (a, b) => Math.round((b - a) / 86400000);
export const fimDeSemana = (d) => d.getDay() === 0 || d.getDay() === 6;
export const dataBR = (d) => `${p2(d.getDate())}/${p2(d.getMonth() + 1)}/${d.getFullYear()}`;
export const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

// ---------------------------------------------------------------- documentos e contatos

function digitoVerificador(numeros, pesoInicial) {
  let soma = 0;
  numeros.forEach((n, i) => { soma += n * (pesoInicial - i); });
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

/** CPF válido, só dígitos. */
export function cpf(s) {
  let base;
  do { base = Array.from({ length: 9 }, () => s.int(0, 9)); } while (new Set(base).size === 1);
  const d1 = digitoVerificador(base, 10);
  const d2 = digitoVerificador([...base, d1], 11);
  return [...base, d1, d2].join('');
}

/** CNPJ válido, só dígitos. */
export function cnpj(s) {
  const base = [...Array.from({ length: 8 }, () => s.int(0, 9)), 0, 0, 0, 1];
  const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesos2 = [6, ...pesos1];
  const dv = (nums, pesos) => { const r = nums.reduce((t, n, i) => t + n * pesos[i], 0) % 11; return r < 2 ? 0 : 11 - r; };
  const d1 = dv(base, pesos1);
  const d2 = dv([...base, d1], pesos2);
  return [...base, d1, d2].join('');
}

/** Celular de Goiânia, no formato da tela. */
export const celular = (s) => `(62) 9${s.int(8100, 9999)}-${String(s.int(0, 9999)).padStart(4, '0')}`;

/**
 * E-mails no domínio reservado .test, que não existe e nunca entrega mensagem: a carga pode rodar num ambiente com o
 * envio de e-mail ligado sem escrever para ninguém de verdade.
 */
export const DOMINIO_DOS_EMAILS = 'email.test';

const semAcento = (t) => t.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/** Cria e-mails únicos a partir do nome: "Ana Paula Moreira" vira ana.moreira@..., depois ana.moreira2@... */
export function fabricaDeEmails() {
  const usados = new Set();
  return (nome, sufixo = '') => {
    const partes = semAcento(nome).replace(/[^a-z ]/g, '').split(' ').filter((p) => p.length > 2 || p === nome);
    const base = `${partes[0]}.${partes[partes.length - 1]}${sufixo}`;
    let email = `${base}@${DOMINIO_DOS_EMAILS}`;
    for (let n = 2; usados.has(email); n++) email = `${base}${n}@${DOMINIO_DOS_EMAILS}`;
    usados.add(email);
    return email;
  };
}

// ---------------------------------------------------------------- nomes

export const NOMES_FEMININOS = ['Ana Paula', 'Beatriz', 'Camila', 'Carolina', 'Cláudia', 'Daniela', 'Débora', 'Eduarda',
  'Elaine', 'Fernanda', 'Gabriela', 'Helena', 'Isabela', 'Juliana', 'Larissa', 'Letícia', 'Luciana', 'Mariana',
  'Marília', 'Natália', 'Patrícia', 'Priscila', 'Raquel', 'Renata', 'Sabrina', 'Simone', 'Tatiane', 'Vanessa',
  'Viviane', 'Aline', 'Bruna', 'Cristiane', 'Adriana', 'Rosângela', 'Luana', 'Sônia', 'Kátia', 'Lorena', 'Thaís',
  'Márcia'];
export const NOMES_MASCULINOS = ['André', 'Bruno', 'Carlos Eduardo', 'Daniel', 'Diego', 'Eduardo', 'Fábio', 'Felipe',
  'Fernando', 'Gabriel', 'Guilherme', 'Gustavo', 'Henrique', 'Igor', 'João Pedro', 'José Carlos', 'Leandro',
  'Leonardo', 'Lucas', 'Luiz Fernando', 'Marcelo', 'Márcio', 'Mateus', 'Paulo Henrique', 'Rafael', 'Renato',
  'Ricardo', 'Rodrigo', 'Sérgio', 'Thiago', 'Vinícius', 'Wellington', 'Alexandre', 'Antônio', 'Cláudio', 'Edson',
  'Hugo', 'Otávio', 'Rogério', 'Wagner'];
export const NOMES_INFANTIS_F = ['Alice', 'Laura', 'Valentina', 'Manuela', 'Sophia', 'Lívia', 'Heloísa', 'Cecília',
  'Maria Clara', 'Lara', 'Isadora', 'Yasmin'];
export const NOMES_INFANTIS_M = ['Miguel', 'Arthur', 'Heitor', 'Davi', 'Bernardo', 'Théo', 'Samuel', 'Enzo', 'Lorenzo',
  'Benício', 'Pedro Henrique', 'Rafael'];
export const SOBRENOMES = ['Silva', 'Santos', 'Oliveira', 'Souza', 'Rodrigues', 'Ferreira', 'Alves', 'Pereira', 'Lima',
  'Gomes', 'Costa', 'Ribeiro', 'Martins', 'Carvalho', 'Almeida', 'Lopes', 'Soares', 'Fernandes', 'Vieira', 'Barbosa',
  'Rocha', 'Dias', 'Nascimento', 'Andrade', 'Moreira', 'Nunes', 'Marques', 'Machado', 'Mendes', 'Freitas', 'Cardoso',
  'Ramos', 'Gonçalves', 'Santana', 'Teixeira', 'Borges', 'Queiroz', 'Rezende', 'Campos', 'Siqueira', 'Brandão',
  'Magalhães', 'Guimarães', 'Arantes', 'Caiado', 'Peixoto', 'Fonseca', 'Assis', 'Prado', 'Xavier'];

export function nomeCompleto(s, sexo, sobrenomeDeFamilia) {
  const primeiro = s.um(sexo === 'F' ? NOMES_FEMININOS : NOMES_MASCULINOS);
  const meio = s.chance(0.6) ? `${s.um(SOBRENOMES)} ` : '';
  return `${primeiro} ${meio}${sobrenomeDeFamilia ?? s.um(SOBRENOMES)}`;
}

// ---------------------------------------------------------------- execução em paralelo

/** Roda `fn` em cada item, no máximo `n` de cada vez, e devolve os resultados na ordem dos itens. */
export async function emParalelo(itens, n, fn) {
  const resultados = new Array(itens.length);
  let proximo = 0;
  const trabalhador = async () => {
    while (proximo < itens.length) {
      const i = proximo++;
      resultados[i] = await fn(itens[i], i);
    }
  };
  await Promise.all(Array.from({ length: Math.min(n, itens.length) }, trabalhador));
  return resultados;
}

// ---------------------------------------------------------------- API

export class ErroDaApi extends Error {
  constructor(metodo, caminho, status, mensagem) {
    super(`${metodo} ${caminho} → ${status}: ${mensagem}`);
    this.status = status;
    this.mensagemDaApi = mensagem;
  }
}

/** Acesso à API de um cliente. Cada chamada leva o token de quem age (administrador, síndico, porteiro, morador). */
export function criarApi(dadosDoCliente) {
  const base = `${dadosDoCliente.api}/api/v1`;
  let chamadas = 0;

  async function chamar(token, metodo, caminho, corpo, { formulario } = {}) {
    const cabecalhos = { 'X-Forwarded-Host': dadosDoCliente.host };
    if (token) cabecalhos.Authorization = `Bearer ${token}`;
    let body;
    if (formulario) body = formulario;
    else if (corpo !== undefined) {
      cabecalhos['Content-Type'] = 'application/json';
      body = JSON.stringify(corpo);
    }
    for (let tentativa = 1; ; tentativa++) {
      chamadas++;
      let resposta;
      try {
        resposta = await fetch(base + caminho, { method: metodo, headers: cabecalhos, body });
      } catch (e) {
        if (tentativa < 3) { await new Promise((r) => setTimeout(r, 500 * tentativa)); continue; }
        throw new ErroDaApi(metodo, caminho, 0, `sem conexão com a API (${e.cause?.code ?? e.message})`);
      }
      const texto = await resposta.text();
      let dados = null;
      try { dados = texto ? JSON.parse(texto) : null; } catch { dados = texto; }
      if (!resposta.ok) {
        throw new ErroDaApi(metodo, caminho, resposta.status, dados?.message ?? String(texto).slice(0, 200));
      }
      return dados;
    }
  }

  return {
    get: (token, caminho) => chamar(token, 'GET', caminho),
    post: (token, caminho, corpo) => chamar(token, 'POST', caminho, corpo),
    put: (token, caminho, corpo) => chamar(token, 'PUT', caminho, corpo),
    del: (token, caminho) => chamar(token, 'DELETE', caminho),
    /** Envio multipart: `partes` é [[nome, Blob, nomeDoArquivo?], ...]. */
    enviar: (token, metodo, caminho, partes) => {
      const formulario = new FormData();
      for (const [nome, valor, arquivo] of partes) {
        if (arquivo) formulario.append(nome, valor, arquivo);
        else formulario.append(nome, valor);
      }
      return chamar(token, metodo, caminho, undefined, { formulario });
    },
    /** Todas as páginas de uma listagem da API (formato { itens, totalPaginas }). */
    async todas(token, caminho) {
      const itens = [];
      const juntar = caminho.includes('?') ? '&' : '?';
      for (let pagina = 0; ; pagina++) {
        const p = await chamar(token, 'GET', `${caminho}${juntar}pagina=${pagina}&tamanho=100`);
        itens.push(...p.itens);
        if (pagina + 1 >= p.totalPaginas) return itens;
      }
    },
    get chamadas() { return chamadas; },
  };
}

export const json = (objeto) => new Blob([JSON.stringify(objeto)], { type: 'application/json' });
export const pdfBlob = (bytes) => new Blob([bytes], { type: 'application/pdf' });

// ---------------------------------------------------------------- banco

/**
 * Roda SQL no banco do cliente, para o que a API grava com a data do momento (entrada de visitante, registro de
 * ocorrência, data do comunicado...) e para as reservas do histórico. Por padrão usa o psql do contêiner do
 * PostgreSQL de desenvolvimento; CARGA_PSQL troca o comando inteiro (o SQL vai pela entrada padrão).
 */
export function criarBanco(dadosDoCliente) {
  const comando = ENV.CARGA_PSQL
    ? ENV.CARGA_PSQL.replace('{banco}', dadosDoCliente.banco)
    : `docker exec -i ${ENV.CARGA_CONTEINER_BANCO || 'portal_transparencia_db'} psql -U ${ENV.DB_USER || 'admin'} -d ${dadosDoCliente.banco}`;
  return {
    /** Executa e devolve as linhas, com as colunas separadas por tabulação. */
    consultar(sql) {
      const r = spawnSync(`${comando} -v ON_ERROR_STOP=1 -q -At -F "\t"`, {
        input: sql, shell: true, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024,
      });
      if (r.status !== 0) throw new Error(`Falha no banco: ${(r.stderr || r.stdout || '').trim().slice(0, 500)}`);
      return r.stdout.split(/\r?\n/).filter(Boolean).map((l) => l.split('\t'));
    },
  };
}

/** Texto como literal SQL. */
export const q = (v) => (v == null ? 'NULL' : `'${String(v).replace(/'/g, "''")}'`);
