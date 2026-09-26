// Áreas comuns que os moradores reservam, com turnos, taxa, antecedência, convidados e termos de uso. Duas ficam
// inativas (em reforma ou manutenção). O que já existe (pelo nome, no condomínio) fica.

import { q } from '../comum.mjs';

export const descricao = 'áreas comuns e turnos';

const termos = (itens) => itens.map((t, i) => `${i + 1}. ${t}`).join('\n');

const REGRAS_GERAIS = [
  'A reserva só vale depois de aprovada pela administração, e é pessoal e intransferível.',
  'O morador responde por danos causados por ele e pelos seus convidados.',
  'Som em volume moderado, respeitando o horário de silêncio das 22h às 7h.',
  'O espaço deve ser devolvido limpo e organizado até o fim do horário reservado.',
];

const TURNOS_DE_EVENTO = [
  { nome: 'Tarde', horaInicio: '12:00', horaFim: '17:00' },
  { nome: 'Noite', horaInicio: '18:00', horaFim: '23:00' },
];
const TURNOS_DE_CHURRASQUEIRA = [
  { nome: 'Almoço', horaInicio: '10:00', horaFim: '16:00' },
  { nome: 'Noite', horaInicio: '17:00', horaFim: '23:00' },
];
const horarios = (faixas) => faixas.map(([ini, fim]) => ({ nome: `${ini} às ${fim}`, horaInicio: ini, horaFim: fim }));

export const AREAS = {
  palmeiras: [
    {
      nome: 'Salão de festas', capacidadeMaxima: 100, permiteConvidados: true, limiteConvidados: 100, taxaValor: 180,
      diasAntecedenciaMin: 3, diasAntecedenciaMax: 90, turnos: TURNOS_DE_EVENTO,
      descricao: 'Salão climatizado no térreo da Torre 1, com cozinha de apoio, freezer, mesas para 100 pessoas e banheiros.',
      termosUso: termos([...REGRAS_GERAIS, 'A lista de convidados fica com a portaria, que libera a entrada pelo nome.', 'A taxa de uso vem no boleto do mês seguinte.']),
    },
    {
      nome: 'Churrasqueira 1', capacidadeMaxima: 30, permiteConvidados: true, limiteConvidados: 30, taxaValor: 80,
      diasAntecedenciaMin: 2, diasAntecedenciaMax: 60, turnos: TURNOS_DE_CHURRASQUEIRA,
      descricao: 'Churrasqueira coberta ao lado da piscina da Torre 1, com pia, geladeira e mesas para 30 pessoas.',
      termosUso: termos([...REGRAS_GERAIS, 'Carvão, gelo e utensílios ficam por conta do morador.']),
    },
    {
      nome: 'Churrasqueira 2', capacidadeMaxima: 30, permiteConvidados: true, limiteConvidados: 30, taxaValor: 80,
      diasAntecedenciaMin: 2, diasAntecedenciaMax: 60, turnos: TURNOS_DE_CHURRASQUEIRA,
      descricao: 'Churrasqueira coberta ao lado do playground da Torre 2, com pia, geladeira e mesas para 30 pessoas.',
      termosUso: termos([...REGRAS_GERAIS, 'Carvão, gelo e utensílios ficam por conta do morador.']),
    },
    {
      nome: 'Espaço gourmet', capacidadeMaxima: 25, permiteConvidados: true, limiteConvidados: 25, taxaValor: 120,
      diasAntecedenciaMin: 3, diasAntecedenciaMax: 60, turnos: [],
      descricao: 'Cozinha equipada com cooktop, forno, adega e bancada para 25 pessoas, no mezanino da Torre 2. Reservado pelo dia inteiro.',
      termosUso: termos([...REGRAS_GERAIS, 'A louça e os equipamentos são conferidos pelo zelador na entrega das chaves.']),
    },
    {
      nome: 'Quadra poliesportiva', capacidadeMaxima: 16, permiteConvidados: true, limiteConvidados: 10, taxaValor: null,
      diasAntecedenciaMin: 0, diasAntecedenciaMax: 14,
      turnos: horarios([['08:00', '10:00'], ['10:00', '12:00'], ['14:00', '16:00'], ['16:00', '18:00'], ['18:00', '20:00'], ['20:00', '22:00']]),
      descricao: 'Quadra coberta de futsal, vôlei e basquete, com iluminação para jogos à noite.',
      termosUso: termos(['Uso com calçado apropriado; chuteira de trava não é permitida.', 'Cada unidade reserva no máximo dois horários por semana.', 'Menores de 12 anos só com um responsável.']),
    },
    {
      nome: 'Brinquedoteca', capacidadeMaxima: 12, permiteConvidados: false, limiteConvidados: null, taxaValor: null,
      diasAntecedenciaMin: 1, diasAntecedenciaMax: 30, turnos: [], ativa: false,
      descricao: 'Sala de brinquedos para crianças de até 10 anos. Em reforma, com reabertura prevista para novembro.',
      termosUso: termos(['Crianças sempre acompanhadas de um adulto responsável.']),
    },
  ],
  mirante: [
    {
      nome: 'Salão de festas', capacidadeMaxima: 60, permiteConvidados: true, limiteConvidados: 60, taxaValor: 150,
      diasAntecedenciaMin: 3, diasAntecedenciaMax: 90, turnos: TURNOS_DE_EVENTO,
      descricao: 'Salão no mezanino, com copa, som ambiente e vista para o Bosque dos Buritis. Até 60 pessoas.',
      termosUso: termos([...REGRAS_GERAIS, 'A lista de convidados fica com a portaria, que libera a entrada pelo nome.']),
    },
    {
      nome: 'Espaço gourmet da cobertura', capacidadeMaxima: 20, permiteConvidados: true, limiteConvidados: 20, taxaValor: 100,
      diasAntecedenciaMin: 2, diasAntecedenciaMax: 60,
      turnos: [{ nome: 'Almoço', horaInicio: '11:00', horaFim: '16:00' }, { nome: 'Noite', horaInicio: '18:00', horaFim: '23:00' }],
      descricao: 'Cozinha gourmet com churrasqueira a gás e deck na cobertura. Até 20 pessoas.',
      termosUso: termos([...REGRAS_GERAIS, 'Proibido o uso de carvão: a churrasqueira da cobertura é a gás.']),
    },
    {
      nome: 'Sauna', capacidadeMaxima: 6, permiteConvidados: false, limiteConvidados: null, taxaValor: null,
      diasAntecedenciaMin: 0, diasAntecedenciaMax: 7, turnos: [], ativa: false,
      descricao: 'Sauna a vapor. Fechada para a troca do gerador de vapor.',
      termosUso: termos(['Uso por maiores de 16 anos.']),
    },
  ],
  villaggio: [
    {
      nome: 'Salão de festas', capacidadeMaxima: 120, permiteConvidados: true, limiteConvidados: 120, taxaValor: 250,
      diasAntecedenciaMin: 5, diasAntecedenciaMax: 120, turnos: [],
      descricao: 'Salão ao lado da portaria, com cozinha industrial, palco, banheiros e estacionamento para convidados. Reservado pelo dia inteiro.',
      termosUso: termos([...REGRAS_GERAIS, 'Os carros dos convidados ficam no estacionamento externo; a entrada no condomínio é só a pé.', 'Contratação de buffet ou DJ precisa ser informada à portaria com dois dias de antecedência.']),
    },
    {
      nome: 'Quiosque 1', capacidadeMaxima: 25, permiteConvidados: true, limiteConvidados: 25, taxaValor: 50,
      diasAntecedenciaMin: 1, diasAntecedenciaMax: 60, turnos: TURNOS_DE_CHURRASQUEIRA,
      descricao: 'Quiosque com churrasqueira e forno de pizza, perto do lago, na Quadra A.',
      termosUso: termos([...REGRAS_GERAIS, 'Não é permitido usar o lago para banho ou pesca.']),
    },
    {
      nome: 'Quiosque 2', capacidadeMaxima: 25, permiteConvidados: true, limiteConvidados: 25, taxaValor: 50,
      diasAntecedenciaMin: 1, diasAntecedenciaMax: 60, turnos: TURNOS_DE_CHURRASQUEIRA,
      descricao: 'Quiosque com churrasqueira ao lado do campo, na Quadra C.',
      termosUso: termos(REGRAS_GERAIS),
    },
    {
      nome: 'Campo de futebol society', capacidadeMaxima: 20, permiteConvidados: true, limiteConvidados: 14, taxaValor: null,
      diasAntecedenciaMin: 0, diasAntecedenciaMax: 15,
      turnos: horarios([['07:00', '09:00'], ['16:00', '18:00'], ['18:00', '20:00'], ['20:00', '22:00']]),
      descricao: 'Campo de grama sintética com iluminação e vestiários.',
      termosUso: termos(['Só com chuteira society ou tênis.', 'Iluminação desligada às 22h em ponto.']),
    },
  ],
};

export async function carregar(ctx) {
  await ctx.resolverAreas();
  const ajustes = [];
  for (const [chave, areas] of Object.entries(AREAS)) {
    const condominioId = ctx.ids.condominio.get(chave);
    if (!condominioId) continue;
    const cadastradoEm = ctx.mundo.condominio(chave).cadastradoEm;
    for (const a of areas) {
      if (ctx.ids.area.has(`${chave}|${a.nome}`)) { ctx.existente(); continue; }
      try {
        const criada = await ctx.api.post(ctx.admin, '/areas-comuns', {
          condominioId, nome: a.nome, descricao: a.descricao, termosUso: a.termosUso, capacidadeMaxima: a.capacidadeMaxima,
          permiteConvidados: a.permiteConvidados, limiteConvidados: a.limiteConvidados, taxaValor: a.taxaValor,
          diasAntecedenciaMin: a.diasAntecedenciaMin, diasAntecedenciaMax: a.diasAntecedenciaMax, ativa: a.ativa ?? true,
          turnos: a.turnos.map((t) => ({ ...t, ativo: true })),
        });
        ctx.ids.area.set(`${chave}|${a.nome}`, criada);
        ajustes.push(`update gc_area_comum set are_dt_cadastro = ${q(cadastradoEm + ' 16:00:00')} where are_cod = ${criada.codigo};`);
        ctx.incluido();
      } catch (e) {
        ctx.recusado(e, `${a.nome} (${chave})`);
      }
    }
  }
  if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
}
