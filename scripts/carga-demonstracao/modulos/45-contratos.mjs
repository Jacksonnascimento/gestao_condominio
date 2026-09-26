// Contratos com os prestadores de cada condomínio: portaria, limpeza, elevadores, jardinagem, piscina, seguro...
// Alguns vencem nos próximos dias (aba "Vencendo"), outros já venceram ou foram rescindidos. A situação é calculada
// pela API a partir das datas. O que já existe (empresa, serviço e início) fica.

import { HOJE, iso, q, somarDias } from '../comum.mjs';

export const descricao = 'contratos de prestadores';

const daquiA = (dias) => iso(somarDias(HOJE, dias));

const CONTRATOS = {
  palmeiras: [
    ['Vigilare Serviços de Portaria Ltda', 'Portaria 24 horas e controle de acesso', 38900, 'Sandro Vieira', '2025-02-01', '2027-01-31'],
    ['Brilho Serviços de Limpeza Eireli', 'Limpeza e conservação das áreas comuns', 19800, 'Cristiane Lopes', '2025-02-01', '2026-01-31', 'Substituído pelo contrato renovado em fevereiro/2026.'],
    ['Brilho Serviços de Limpeza Eireli', 'Limpeza e conservação das áreas comuns', 21450, 'Cristiane Lopes', '2026-02-01', '2027-01-31', 'Renovação com reajuste de 8,3% e mais uma auxiliar no turno da tarde.'],
    ['Ascensor Centro-Oeste Manutenção de Elevadores Ltda', 'Manutenção preventiva e corretiva dos 4 elevadores', 3280, 'Eng. Paulo Mendes', '2024-11-01', daquiA(20), 'Cotação de renovação pedida a três empresas.'],
    ['Jardim & Cia Paisagismo', 'Jardinagem e poda', 2150, 'Marcos Rocha', '2025-03-01', '2026-02-28', 'Rescindido em junho/2025 por atraso no cronograma de podas.', 'RESCINDIDO'],
    ['Verde Vivo Paisagismo Ltda', 'Jardinagem, poda e adubação', 2390, 'Renata Siqueira', '2025-07-01', '2027-06-30'],
    ['AquaLimpa Piscinas', 'Tratamento e limpeza das piscinas adulto e infantil', 1480, 'Diego Freitas', '2025-04-01', daquiA(12)],
    ['Olho Vivo Segurança Eletrônica', 'Monitoramento e manutenção do CFTV (64 câmeras)', 1890, 'Igor Almeida', '2025-05-15', '2027-05-14'],
    ['Protege Corretora de Seguros', 'Seguro predial com cobertura de incêndio, raio e responsabilidade civil', 2740, 'Adriana Costa', '2025-10-01', daquiA(4), 'Parcela mensal do prêmio anual.'],
    ['Dedetiza Goiás Controle de Pragas', 'Dedetização e desratização trimestral', 950, 'Wagner Lima', '2025-01-10', '2026-01-09'],
  ],
  mirante: [
    ['Vigilare Serviços de Portaria Ltda', 'Portaria 24 horas', 19800, 'Sandro Vieira', '2025-03-01', '2027-02-28'],
    ['Conserva Mais Serviços Gerais', 'Limpeza e conservação', 9200, 'Luana Prado', '2025-03-01', '2026-08-31'],
    ['Conserva Mais Serviços Gerais', 'Limpeza e conservação', 9600, 'Luana Prado', '2026-09-01', '2027-08-31', 'Renovação por 12 meses.'],
    ['Ascensor Centro-Oeste Manutenção de Elevadores Ltda', 'Manutenção dos 2 elevadores', 1640, 'Eng. Paulo Mendes', '2025-03-01', '2027-02-28'],
    ['Energia Total Geradores', 'Manutenção preventiva do gerador de emergência', 690, 'Fábio Nunes', '2025-06-01', daquiA(25)],
    ['Protege Corretora de Seguros', 'Seguro predial', 1320, 'Adriana Costa', '2025-11-01', '2026-10-31'],
  ],
  villaggio: [
    ['Guardiã Segurança Patrimonial Ltda', 'Portaria, controle de acesso e ronda motorizada 24 horas', 46500, 'Cel. Hélio Santana', '2025-04-01', '2027-03-31'],
    ['Brilho Serviços de Limpeza Eireli', 'Limpeza das áreas comuns e do clube', 12300, 'Cristiane Lopes', '2025-04-01', '2027-03-31'],
    ['Verde Vivo Paisagismo Ltda', 'Paisagismo, poda e manutenção do lago', 7900, 'Renata Siqueira', '2025-04-01', daquiA(18), 'Proposta de renovação em análise pelo conselho.'],
    ['Recicla Cerrado Cooperativa', 'Coleta seletiva de recicláveis', 1150, 'Maria das Dores', '2025-06-01', '2026-05-31'],
    ['Portão Forte Automação', 'Manutenção dos portões e cancelas', 980, 'Rogério Dias', '2025-04-15', '2026-04-14', 'Rescindido em janeiro/2026; a manutenção passou para a equipe da Guardiã.', 'RESCINDIDO'],
    ['Protege Corretora de Seguros', 'Seguro das áreas comuns e do clube', 3100, 'Adriana Costa', '2025-09-01', '2026-08-31'],
    ['Protege Corretora de Seguros', 'Seguro das áreas comuns e do clube', 3290, 'Adriana Costa', '2026-09-01', '2027-08-31', 'Renovação com inclusão da cobertura de vendaval.'],
  ],
};

export async function carregar(ctx) {
  await ctx.resolverCondominios();
  const ajustes = [];
  for (const [chave, contratos] of Object.entries(CONTRATOS)) {
    const condominioId = ctx.ids.condominio.get(chave);
    if (!condominioId) continue;
    const existentes = new Set();
    for (const aba of ['ATIVOS', 'A_VENCER', 'HISTORICO']) {
      for (const c of await ctx.api.todas(ctx.admin, `/contratos?aba=${aba}&condominioId=${condominioId}`)) {
        existentes.add(`${c.empresa}|${c.servico}|${c.dataInicio}`);
      }
    }
    for (const [empresa, servico, valor, responsavel, dataInicio, dataFim, observacoes, status] of contratos) {
      if (existentes.has(`${empresa}|${servico}|${dataInicio}`)) { ctx.existente(); continue; }
      try {
        const criado = await ctx.api.post(ctx.admin, '/contratos', {
          condominioId, empresa, servico, valor, responsavel, dataInicio, dataFim, observacoes: observacoes ?? null,
          status: status ?? (dataFim < iso(HOJE) ? 'FINALIZADO' : 'ATIVO'),
        });
        ajustes.push(`update gc_contrato set ctr_dt_cadastro = ${q(dataInicio + ' 10:00:00')} where ctr_cod = ${criado.id};`);
        ctx.incluido();
      } catch (e) {
        ctx.recusado(e, `${empresa} (${chave})`);
      }
    }
  }
  if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
}
