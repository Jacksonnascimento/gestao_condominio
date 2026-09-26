// O mundo da demonstração: três condomínios em Goiânia e Aparecida de Goiânia, com as unidades, as famílias que moram
// nelas, os proprietários que alugam, o síndico, a administradora e a portaria. Sai sempre igual da mesma semente;
// só as datas do movimento (reservas, portaria, ocorrências) andam com o dia em que a carga roda.

import {
  celular, cnpj, cpf, fabricaDeEmails, nomeCompleto, NOMES_INFANTIS_F, NOMES_INFANTIS_M, SOBRENOMES, sorteio,
} from './comum.mjs';

const s = sorteio('mundo-v1');
const email = fabricaDeEmails();
const cpfsUsados = new Set();

function novoCpf() {
  let c;
  do { c = cpf(s); } while (cpfsUsados.has(c));
  cpfsUsados.add(c);
  return c;
}

function pessoa(nome, sexo, extras = {}) {
  return { nome, sexo, cpfCnpj: novoCpf(), tipoPessoa: 'F', email: email(nome), telefone: celular(s), ...extras };
}

function pessoaQualquer() {
  const sexo = s.chance(0.5) ? 'F' : 'M';
  return pessoa(nomeCompleto(s, sexo), sexo);
}

function empresa(nome) {
  const doc = cnpj(s);
  cpfsUsados.add(doc);
  const base = nome.split(' ')[0].toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  return { nome, sexo: null, cpfCnpj: doc, tipoPessoa: 'J', email: email(`${base} contato`), telefone: `(62) 3${s.int(200, 999)}-${String(s.int(0, 9999)).padStart(4, '0')}` };
}

// ---------------------------------------------------------------- condomínios e unidades

function apartamentos(blocos, andares, finais, area) {
  const lista = [];
  for (const bloco of blocos) {
    for (let andar = 1; andar <= andares; andar++) {
      for (const final of finais) {
        lista.push({
          numero: `${andar}0${final}`, bloco, andar: String(andar), tipo: 'APARTAMENTO',
          areaPrivada: area(final), observacao: null,
        });
      }
    }
  }
  return lista;
}

export const CONDOMINIOS = [
  {
    chave: 'palmeiras',
    dados: {
      nome: 'Residencial Parque das Palmeiras', logradouro: 'Avenida E', numero: '820', complemento: null,
      bairro: 'Jardim Goiás', cidade: 'Goiânia', estado: 'GO', cep: '74805-080', pais: 'Brasil',
      referencia: 'A duas quadras do Parque Flamboyant', tipologia: 'RESIDENCIAL', diaVencimentoTaxa: 10,
    },
    cadastradoEm: '2025-01-20',
    unidades: apartamentos(['Torre 1', 'Torre 2'], 12, [1, 2, 3, 4], (f) => (f === 1 || f === 4 ? 98.4 : 76.2)),
  },
  {
    chave: 'mirante',
    dados: {
      nome: 'Edifício Mirante do Bosque', logradouro: 'Rua T-37', numero: '1450', complemento: null,
      bairro: 'Setor Bueno', cidade: 'Goiânia', estado: 'GO', cep: '74230-020', pais: 'Brasil',
      referencia: 'Esquina com a Avenida T-10, de frente para o Bosque dos Buritis', tipologia: 'MISTO',
      diaVencimentoTaxa: 5,
    },
    cadastradoEm: '2025-02-10',
    unidades: [
      ...apartamentos([null], 16, [1, 2], () => 142.5),
      ...['01', '02', '03'].map((n) => ({
        numero: `Loja ${n}`, bloco: null, andar: 'Térreo', tipo: 'COMERCIAL', areaPrivada: 64.0,
        observacao: 'Loja com frente para a Rua T-37',
      })),
    ],
  },
  {
    chave: 'villaggio',
    dados: {
      nome: 'Condomínio Villaggio Toscana', logradouro: 'Avenida Rio Verde', numero: '3100', complemento: null,
      bairro: 'Jardim Presidente', cidade: 'Aparecida de Goiânia', estado: 'GO', cep: '74946-420', pais: 'Brasil',
      referencia: 'Portaria principal pela Avenida Rio Verde', tipologia: 'RESIDENCIAL', diaVencimentoTaxa: 15,
    },
    cadastradoEm: '2025-03-05',
    unidades: ['Quadra A', 'Quadra B', 'Quadra C'].flatMap((bloco) =>
      Array.from({ length: 18 }, (_, i) => {
        const sobrado = s.chance(0.55);
        return {
          numero: String(i + 1).padStart(2, '0'), bloco, andar: null, tipo: 'OUTROS',
          areaPrivada: sobrado ? s.int(190, 240) : s.int(140, 175),
          observacao: sobrado ? 'Sobrado com 3 suítes' : 'Casa térrea com 3 quartos',
        };
      })),
  },
];

export const chaveDaUnidade = (condominio, u) => `${condominio}|${u.bloco ?? ''}|${u.numero}`;

// Fração ideal pela área, e a situação de ocupação de cada unidade
for (const c of CONDOMINIOS) {
  const areaTotal = c.unidades.reduce((t, u) => t + u.areaPrivada, 0);
  for (const u of c.unidades) {
    u.chave = chaveDaUnidade(c.chave, u);
    u.fracaoIdeal = Math.round((u.areaPrivada / areaTotal) * 1e6) / 1e6;
    u.statusOcupacao = u.tipo === 'COMERCIAL'
      ? (u.numero === 'Loja 03' ? 'VAZIA' : 'OCUPADA')
      : s.pesado([['OCUPADA', 86], ['VAZIA', 10], ['EM_REFORMA', 4]]);
  }
}

// ---------------------------------------------------------------- famílias e ocupantes

/** Data 'YYYY-MM-DD' sorteada entre dois anos. */
const dataEntre = (anoIni, anoFim) => `${s.int(anoIni, anoFim)}-${String(s.int(1, 12)).padStart(2, '0')}-${String(s.int(1, 28)).padStart(2, '0')}`;

export const PESSOAS = [];
export const OCUPANTES = []; // { unidade, pessoa, vinculo, inicio, fim }
export const FAMILIAS = []; // uma por unidade habitada: { unidade, condominio, responsavel, moradores[] }

const registrar = (p) => { PESSOAS.push(p); return p; };

// Alguns investidores têm várias unidades alugadas no mesmo condomínio
const investidores = {
  palmeiras: [registrar(pessoa('Otávio Guimarães Caiado', 'M')), registrar(pessoa('Kátia Magalhães Brandão', 'F'))],
  mirante: [registrar(pessoa('Wagner Siqueira Fonseca', 'M'))],
  villaggio: [],
};

function familia(sobrenome, { conjuge = 0.65, filhos = [0, 2] } = {}) {
  const sexo = s.chance(0.5) ? 'F' : 'M';
  const titular = registrar(pessoa(nomeCompleto(s, sexo, sobrenome), sexo));
  const membros = [{ pessoa: titular, papel: 'titular' }];
  if (s.chance(conjuge)) {
    const sexoConjuge = sexo === 'F' ? 'M' : 'F';
    membros.push({ pessoa: registrar(pessoa(nomeCompleto(s, sexoConjuge, sobrenome), sexoConjuge)), papel: 'conjuge' });
  }
  for (let i = s.int(filhos[0], filhos[1]); i > 0; i--) {
    const sexoFilho = s.chance(0.5) ? 'F' : 'M';
    const primeiro = s.um(sexoFilho === 'F' ? NOMES_INFANTIS_F : NOMES_INFANTIS_M);
    membros.push({ pessoa: registrar(pessoa(`${primeiro} ${sobrenome}`, sexoFilho)), papel: 'dependente' });
  }
  return membros;
}

const vinculoDoMembro = (papel, base) => (papel === 'titular' ? base : papel === 'conjuge' ? 'CONJUGE' : 'DEPENDENTE');

for (const c of CONDOMINIOS) {
  for (const u of c.unidades) {
    if (u.tipo === 'COMERCIAL') {
      const dono = registrar(pessoa(nomeCompleto(s, 'M'), 'M'));
      OCUPANTES.push({ unidade: u, pessoa: dono, vinculo: 'PROPRIETARIO', inicio: dataEntre(2016, 2019), fim: null });
      if (u.statusOcupacao === 'OCUPADA') {
        const loja = registrar(empresa(u.numero === 'Loja 01' ? 'Farmácia Bosque dos Buritis Ltda' : 'Café Mirante Comércio de Alimentos Ltda'));
        OCUPANTES.push({ unidade: u, pessoa: loja, vinculo: 'LOCATARIO', inicio: dataEntre(2022, 2024), fim: '2027-06-30' });
      }
      continue;
    }
    if (u.statusOcupacao !== 'OCUPADA') {
      // Unidade vazia ou em reforma: só o proprietário, quando há
      if (u.statusOcupacao === 'EM_REFORMA' || s.chance(0.6)) {
        const dono = registrar(pessoaQualquer());
        OCUPANTES.push({ unidade: u, pessoa: dono, vinculo: 'PROPRIETARIO', inicio: dataEntre(2018, 2025), fim: null });
      }
      continue;
    }
    const sobrenome = s.um(SOBRENOMES);
    const alugada = s.chance(0.3);
    let base = 'PROPRIETARIO';
    let inicio = dataEntre(2014, 2025);
    if (alugada) {
      const dono = investidores[c.chave].length && s.chance(0.35)
        ? s.um(investidores[c.chave])
        : registrar(pessoaQualquer());
      OCUPANTES.push({ unidade: u, pessoa: dono, vinculo: 'PROPRIETARIO', inicio: dataEntre(2012, 2021), fim: null });
      // De vez em quando, um inquilino anterior, que já saiu
      if (s.chance(0.2)) {
        const antigo = registrar(pessoa(nomeCompleto(s, 'M'), 'M'));
        OCUPANTES.push({ unidade: u, pessoa: antigo, vinculo: 'LOCATARIO', inicio: '2022-02-01', fim: '2024-01-31' });
      }
      base = 'LOCATARIO';
      inicio = dataEntre(2024, 2026);
      if (inicio > '2026-08-31') inicio = '2026-08-01';
    }
    const membros = familia(sobrenome, alugada ? { conjuge: 0.5, filhos: [0, 2] } : {});
    const fim = base === 'LOCATARIO' ? `${Number(inicio.slice(0, 4)) + 2}${inicio.slice(4)}` : null;
    for (const m of membros) {
      OCUPANTES.push({ unidade: u, pessoa: m.pessoa, vinculo: vinculoDoMembro(m.papel, base), inicio, fim });
    }
    const adultos = membros.filter((m) => m.papel !== 'dependente').map((m) => m.pessoa);
    FAMILIAS.push({
      condominio: c.chave, unidade: u, sobrenome, inicio, alugada,
      responsavel: membros[0].pessoa, adultos, moradores: membros.map((m) => m.pessoa),
      // O titular entra no sistema na maioria das casas; o cônjuge, em algumas
      comAcesso: [
        ...(s.chance(0.78) ? [membros[0].pessoa] : []),
        ...(adultos[1] && s.chance(0.3) ? [adultos[1]] : []),
      ],
    });
  }
}

// ---------------------------------------------------------------- gestão e portaria

/**
 * Acessos ao sistema: `pessoa`, `condominio`, `papel`, desde quando e se está ativo. O síndico é um morador; a
 * administração e a portaria são pessoas de fora, que não ocupam unidade.
 */
export const ACESSOS = [];

const administradora = {
  luciana: registrar(pessoa('Luciana Arantes Prado', 'F')),
  rodrigo: registrar(pessoa('Rodrigo Peixoto Campos', 'M')),
};
const FUNCIONARIOS = {
  palmeiras: {
    gerente: registrar(pessoa('Márcio Rezende Borges', 'M')),
    porteiros: [
      registrar(pessoa('José Carlos Nunes', 'M')), registrar(pessoa('Edson Ramos Teixeira', 'M')),
      registrar(pessoa('Sônia Aparecida Dias', 'F')), registrar(pessoa('Wellington Cardoso Assis', 'M')),
    ],
    exPorteiro: registrar(pessoa('Cláudio Mendes Freitas', 'M')),
  },
  mirante: {
    gerente: null,
    porteiros: [registrar(pessoa('Antônio Marques Xavier', 'M')), registrar(pessoa('Rosângela Queiroz Lima', 'F'))],
    exPorteiro: null,
  },
  villaggio: {
    gerente: registrar(pessoa('Simone Santana Rocha', 'F')),
    porteiros: [
      registrar(pessoa('Hugo Ferreira Machado', 'M')), registrar(pessoa('Leandro Gomes Andrade', 'M')),
      registrar(pessoa('Elaine Barbosa Moreira', 'F')),
    ],
    exPorteiro: null,
  },
};

/** Síndico: um proprietário morador, com acesso, de uma unidade escolhida. */
function escolherSindico(condominio) {
  const candidatas = FAMILIAS.filter((f) => f.condominio === condominio && !f.alugada && f.inicio < '2023-01-01');
  const f = candidatas[Math.floor(candidatas.length / 3)];
  if (!f.comAcesso.includes(f.responsavel)) f.comAcesso.unshift(f.responsavel);
  return f.responsavel;
}

export const GESTAO = {};
for (const c of CONDOMINIOS) {
  const sindico = escolherSindico(c.chave);
  const f = FUNCIONARIOS[c.chave];
  const admin = c.chave === 'villaggio' ? administradora.rodrigo : administradora.luciana;
  GESTAO[c.chave] = { sindico, admin, gerente: f.gerente, porteiros: f.porteiros };
  ACESSOS.push({ pessoa: sindico, condominio: c.chave, papel: 'SINDICO', desde: c.chave === 'mirante' ? '2025-04-26' : '2025-03-29', ativo: true });
  ACESSOS.push({ pessoa: admin, condominio: c.chave, papel: 'ADMIN', desde: c.cadastradoEm, ativo: true });
  if (f.gerente) ACESSOS.push({ pessoa: f.gerente, condominio: c.chave, papel: 'FUNCIONARIO_ADM', desde: c.cadastradoEm, ativo: true });
  f.porteiros.forEach((p, i) => ACESSOS.push({ pessoa: p, condominio: c.chave, papel: 'PORTEIRO', desde: i === f.porteiros.length - 1 && c.chave === 'palmeiras' ? '2026-02-02' : c.cadastradoEm, ativo: true }));
  if (f.exPorteiro) ACESSOS.push({ pessoa: f.exPorteiro, condominio: c.chave, papel: 'PORTEIRO', desde: c.cadastradoEm, ativo: false });
  for (const fam of FAMILIAS.filter((x) => x.condominio === c.chave)) {
    for (const p of fam.comAcesso) {
      const entrada = fam.inicio > c.cadastradoEm ? fam.inicio : c.cadastradoEm;
      ACESSOS.push({ pessoa: p, condominio: c.chave, papel: 'MORADOR', desde: entrada, ativo: true });
    }
  }
}

/** Pessoas que não ocupam unidade: o cadastro delas é feito junto com o acesso. */
export const PESSOAS_DE_FORA = new Set([
  ...Object.values(administradora),
  ...Object.values(FUNCIONARIOS).flatMap((f) => [f.gerente, ...f.porteiros, f.exPorteiro].filter(Boolean)),
]);

export const condominio = (chave) => CONDOMINIOS.find((c) => c.chave === chave);
export const familiasDo = (chave) => FAMILIAS.filter((f) => f.condominio === chave);
/** Famílias em que alguém entra no sistema: são elas que pedem reservas e registram ocorrências. */
export const familiasComAcesso = (chave) => familiasDo(chave).filter((f) => f.comAcesso.length > 0);
