// Comunicados dos últimos seis meses: assembleias, obras, falta de água, regras, avisos da portaria. Publicados pelo
// síndico ou pela administradora; dois vêm da administração geral para os três condomínios. Alguns têm PDF anexo.
// O que já existe (pelo título, no condomínio) fica.

import { HOJE, json, momento, noHorario, pdfBlob, q, somarDias } from '../comum.mjs';
import { gerarPdf } from '../pdf.mjs';

export const descricao = 'comunicados, com anexos';

// [dias atrás, autor, título, público, urgente, mensagem, anexo?: { nome, titulo, linhas }]
const DOS_CONDOMINIOS = {
  palmeiras: [
    [178, 'sindico', 'Resultado da assembleia geral ordinária', 'TODOS', false,
      'A assembleia aprovou as contas do último exercício, a previsão orçamentária e a reforma da fachada da Torre 2, que começa no segundo semestre. A ata completa está anexa.',
      { nome: 'Ata da assembleia geral ordinária.pdf', titulo: 'Ata da Assembleia Geral Ordinária', linhas: [
        'Aos vinte e oito dias do mês, às 19h30, em segunda convocação, reuniram-se no salão de festas os condôminos do Residencial Parque das Palmeiras, com a presença de 41 unidades.',
        'Item 1 — Prestação de contas: aprovada por unanimidade, com o parecer favorável do conselho fiscal.',
        'Item 2 — Previsão orçamentária: aprovada por maioria, com reajuste de 6,5% na taxa condominial.',
        'Item 3 — Reforma da fachada da Torre 2: aprovada a proposta de menor valor entre as três apresentadas, com rateio em 10 parcelas.',
        'Nada mais havendo a tratar, a síndica encerrou a reunião às 21h50.'] }],
    [160, 'admin', 'Reajuste da taxa condominial', 'PROPRIETARIOS', false,
      'Conforme aprovado em assembleia, a taxa condominial passa a ter reajuste de 6,5% a partir do próximo boleto. Unidades de 98 m²: R$ 812,40; unidades de 76 m²: R$ 629,10.'],
    [141, 'sindico', 'Limpeza das caixas d\'água: falta de água na quinta-feira', 'TODOS', true,
      'Na quinta-feira, das 8h às 14h, as duas torres ficarão sem água para a limpeza semestral dos reservatórios. Recomendamos armazenar água na véspera.'],
    [120, 'sindico', 'Regras para mudanças e entrega de móveis', 'TODOS', false,
      'Mudanças só de segunda a sábado, das 8h às 18h, com agendamento na portaria com 48 horas de antecedência e uso do elevador de serviço com proteção. O regulamento completo está anexo.',
      { nome: 'Regulamento de mudanças.pdf', titulo: 'Regulamento de mudanças e entregas', linhas: [
        '1. As mudanças devem ser agendadas na portaria com pelo menos 48 horas de antecedência.',
        '2. Horário: de segunda a sábado, das 8h às 18h. Não há mudanças aos domingos e feriados.',
        '3. Só o elevador de serviço pode ser usado, com as mantas de proteção fornecidas pela portaria.',
        '4. Danos às áreas comuns são cobrados da unidade responsável.'] }],
    [104, 'admin', 'Dedetização das áreas comuns', 'TODOS', false,
      'No sábado pela manhã faremos a dedetização trimestral das garagens, escadas e áreas de lazer. Mantenha os animais de estimação dentro do apartamento até as 14h.'],
    [88, 'sindico', 'Vacinação dos pets no condomínio', 'TODOS', false,
      'No domingo, das 9h às 12h, uma clínica veterinária parceira aplicará vacinas antirrábica e V10 no salão de festas, com preço especial para os moradores.'],
    [71, 'sindico', 'Obra de impermeabilização da garagem da Torre 2', 'TODOS', false,
      'A obra de impermeabilização da garagem da Torre 2 começa na segunda-feira e dura cerca de quatro semanas. As vagas do setor B serão remanejadas conforme o cronograma anexo.',
      { nome: 'Cronograma da impermeabilização.pdf', titulo: 'Cronograma da impermeabilização da garagem — Torre 2', linhas: [
        'Semana 1: vagas B01 a B12 — remoção do piso e tratamento das trincas.',
        'Semana 2: vagas B13 a B24 — aplicação da manta.',
        'Semana 3: vagas B25 a B36 — teste de estanqueidade e proteção mecânica.',
        'Semana 4: pintura da sinalização e liberação das vagas.',
        'Durante a obra, os carros das vagas interditadas ficam no estacionamento de visitantes.'] }],
    [65, 'admin', 'Cadastro de inquilinos e seguro-fiança', 'INQUILINOS', false,
      'Inquilinos que chegaram este ano devem atualizar o cadastro na administração, com cópia do contrato de locação, para liberação do acesso às áreas de lazer.'],
    [55, 'admin', 'Novo horário da portaria de serviço', 'FUNCIONARIOS', false,
      'A partir de segunda-feira, a portaria de serviço funciona das 7h às 19h. Entregas de fornecedores fora desse horário passam pela portaria social.'],
    [30, 'sindico', 'Convocação: assembleia extraordinária', 'PROPRIETARIOS', false,
      'Convocamos os proprietários para a assembleia extraordinária que vai deliberar sobre a instalação de carregadores para carros elétricos na garagem. Edital anexo.',
      { nome: 'Edital de convocação - assembleia extraordinária.pdf', titulo: 'Edital de convocação — Assembleia Geral Extraordinária', linhas: [
        'Ficam convocados os senhores condôminos para a Assembleia Geral Extraordinária, no salão de festas, às 19h em primeira convocação e às 19h30 em segunda.',
        'Pauta: 1) instalação de carregadores para veículos elétricos nas garagens; 2) forma de rateio e cobrança da energia consumida.',
        'Procurações devem ter firma reconhecida e ser entregues à administração até o início da reunião.'] }],
    [18, 'sindico', 'Desligamento de energia para manutenção da subestação', 'TODOS', true,
      'A concessionária fará manutenção na subestação do condomínio no sábado, das 7h às 11h. Os elevadores ficarão parados; o gerador atende só as bombas e a iluminação de emergência.'],
    [9, 'admin', 'Boletos com novo banco a partir do próximo mês', 'TODOS', false,
      'A partir do próximo vencimento, os boletos passam a ser emitidos por outro banco. O valor e o dia de vencimento não mudam; os débitos automáticos precisam ser cadastrados de novo.'],
    [3, 'sindico', 'Elevador social da Torre 1 parado para conserto', 'TODOS', true,
      'O elevador social da Torre 1 está parado aguardando uma peça da placa de comando. A previsão é de retorno em até cinco dias. Use o elevador de serviço, que foi liberado para todos.'],
    [1, 'sindico', 'Achados e perdidos', 'TODOS', false,
      'Estão na portaria da Torre 2: uma bicicleta infantil azul, um par de óculos de grau e uma chave com chaveiro de time. Quem reconhecer pode retirar com documento.'],
  ],
  mirante: [
    [170, 'sindico', 'Prestação de contas do semestre', 'PROPRIETARIOS', false,
      'O balancete do semestre está disponível na administração. As despesas ficaram 3% abaixo do previsto, e a sobra foi para o fundo de reserva.'],
    [132, 'admin', 'Troca do gerador de vapor da sauna', 'TODOS', false,
      'A sauna ficará fechada até a troca do gerador de vapor, que apresentou defeito sem conserto. A compra foi aprovada pelo conselho.'],
    [97, 'sindico', 'Horário de silêncio', 'TODOS', false,
      'Recebemos reclamações de barulho depois das 22h. Lembramos que o regimento interno prevê silêncio das 22h às 7h, inclusive nas varandas.'],
    [74, 'sindico', 'Limpeza da fachada', 'TODOS', false,
      'A limpeza da fachada com balancim será feita ao longo de duas semanas, de cima para baixo. Mantenha as janelas fechadas no dia do seu andar, conforme o cronograma anexo.',
      { nome: 'Cronograma da limpeza da fachada.pdf', titulo: 'Cronograma da limpeza da fachada', linhas: [
        'Dias 1 a 3: andares 16 a 12.', 'Dias 4 a 6: andares 11 a 7.', 'Dias 7 a 9: andares 6 a 2.',
        'Dias 10 e 11: 1º andar, mezanino e lojas.', 'Em caso de chuva, o cronograma avança um dia.'] }],
    [46, 'admin', 'Seguro predial renovado', 'PROPRIETARIOS', false,
      'O seguro predial foi renovado com a mesma cobertura e reajuste abaixo da inflação. A apólice está à disposição na administração.'],
    [22, 'sindico', 'Falta de água na terça-feira', 'TODOS', true,
      'A distribuidora de água fará obra na rede da Rua T-37 na terça-feira. O abastecimento pode ser interrompido entre 9h e 16h; a caixa d\'água do prédio deve atender o consumo normal.'],
    [6, 'sindico', 'Nova regra para o espaço gourmet da cobertura', 'TODOS', false,
      'A partir deste mês, o espaço gourmet da cobertura só pode ser reservado duas vezes por mês por unidade, para que todos consigam usar.'],
  ],
  villaggio: [
    [175, 'sindico', 'Resultado da assembleia ordinária', 'TODOS', false,
      'Aprovadas as contas, o orçamento do ano e a troca da iluminação das ruas internas por LED. Ata anexa.',
      { nome: 'Ata da assembleia ordinária.pdf', titulo: 'Ata da Assembleia Geral Ordinária', linhas: [
        'Com a presença de 29 unidades, a assembleia aprovou por unanimidade a prestação de contas.',
        'Aprovado o orçamento anual, com reajuste de 5,8% na taxa condominial.',
        'Aprovada a troca da iluminação das ruas internas por luminárias LED, em três etapas, uma por quadra.'] }],
    [150, 'admin', 'Cadastro de prestadores de serviço', 'TODOS', false,
      'Diaristas, jardineiros e outros prestadores frequentes precisam ser cadastrados na portaria, com documento e foto, para agilizar a entrada.'],
    [121, 'sindico', 'Velocidade máxima nas ruas internas', 'TODOS', true,
      'Lembramos que a velocidade máxima nas ruas do condomínio é de 20 km/h. Há muitas crianças brincando e ciclistas, principalmente no fim da tarde.'],
    [99, 'sindico', 'Troca da iluminação da Quadra A', 'TODOS', false,
      'Começa na próxima semana a troca das luminárias da Quadra A por LED. As ruas podem ficar com iluminação parcial à noite durante três dias.'],
    [80, 'admin', 'Coleta seletiva: novos dias', 'TODOS', false,
      'A cooperativa passa a recolher os recicláveis às terças e sextas pela manhã. Deixe o material separado no coletor verde em frente à sua casa até as 8h.'],
    [58, 'sindico', 'Poda das árvores das calçadas', 'PROPRIETARIOS', false,
      'A poda das árvores das calçadas é responsabilidade de cada proprietário, conforme o regimento. A equipe de paisagismo pode fazer o serviço mediante orçamento.'],
    [37, 'admin', 'Treinamento da equipe de segurança', 'FUNCIONARIOS', false,
      'Na quarta-feira, a equipe de portaria e ronda participa de treinamento de primeiros socorros e combate a incêndio, no salão de festas, das 14h às 17h.'],
    [20, 'sindico', 'Cachorros soltos nas ruas internas', 'TODOS', false,
      'Tivemos dois incidentes com cachorros soltos nesta semana. Os animais devem circular sempre com guia, e a sujeira deve ser recolhida pelo tutor.'],
    [8, 'sindico', 'Portão de visitantes em manutenção', 'TODOS', true,
      'O portão de visitantes está em manutenção até sexta-feira. A entrada de visitantes e prestadores passa pela portaria principal, com identificação normal.'],
    [2, 'admin', 'Mutirão de limpeza do lago', 'TODOS', false,
      'No sábado às 8h faremos um mutirão com a equipe de paisagismo para limpar as margens do lago. Moradores e crianças são bem-vindos; haverá café da manhã no Quiosque 1.'],
  ],
};

// Da administração geral para os três condomínios
const DA_ADMINISTRACAO_GERAL = [
  [168, 'Novo sistema de gestão do condomínio', 'TODOS', false,
    'Reservas das áreas comuns, encomendas, visitantes, ocorrências e comunicados agora ficam no sistema. Cada morador recebe por e-mail o link para criar a sua senha.'],
  [15, 'Consulta da disponibilidade das áreas comuns', 'TODOS', false,
    'Ao pedir uma reserva, o sistema passa a mostrar na hora os turnos já ocupados na data escolhida, sem precisar esperar a resposta da administração.'],
];

function anexoPdf(condominio, anexo) {
  const d = condominio.dados;
  return pdfBlob(gerarPdf({
    cabecalho: d.nome, subcabecalho: `${d.logradouro}, ${d.numero} — ${d.bairro}, ${d.cidade}/${d.estado}`,
    titulo: anexo.titulo, linhas: anexo.linhas, rodape: 'Documento da administração do condomínio',
  }));
}

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverCondominios();
  const ajustes = [];
  const titulosExistentes = async (condominioId) => new Set(
    (await api.todas(ctx.admin, `/comunicados?condominioId=${condominioId}`)).map((c) => c.titulo));

  for (const [chave, lista] of Object.entries(DOS_CONDOMINIOS)) {
    const condominioId = ctx.ids.condominio.get(chave);
    if (!condominioId) continue;
    const existentes = await titulosExistentes(condominioId);
    const gestao = mundo.GESTAO[chave];
    for (const [dias, autor, titulo, publicoDestino, urgente, mensagem, anexo] of lista) {
      if (existentes.has(titulo)) { ctx.existente(); continue; }
      try {
        const token = await ctx.token(autor === 'sindico' ? gestao.sindico : gestao.admin);
        const partes = [['comunicado', json({ titulo, mensagem, publicoDestino, urgente })]];
        if (anexo) partes.push(['anexo', anexoPdf(mundo.condominio(chave), anexo), anexo.nome]);
        const criado = await api.enviar(token, 'POST', `/comunicados?condominioId=${condominioId}`, partes);
        const quando = noHorario(somarDias(HOJE, -dias), 9 + (dias % 9), (dias * 7) % 60);
        ajustes.push(`update gc_comunicado set com_dt_cadastro = ${q(momento(quando))} where com_cod = ${criado.id};`);
        ctx.incluido();
      } catch (e) {
        ctx.recusado(e, `${titulo} (${chave})`);
      }
    }
  }

  const todos = [...ctx.ids.condominio.values()];
  if (todos.length) {
    const existentes = await titulosExistentes(todos[0]);
    for (const [dias, titulo, publicoDestino, urgente, mensagem] of DA_ADMINISTRACAO_GERAL) {
      if (existentes.has(titulo)) { ctx.existente(); continue; }
      try {
        const criado = await api.enviar(ctx.admin, 'POST', '/comunicados',
          [['comunicado', json({ titulo, mensagem, publicoDestino, urgente, condominioIds: todos })]]);
        ajustes.push(`update gc_comunicado set com_dt_cadastro = ${q(momento(noHorario(somarDias(HOJE, -dias), 8, 30)))} where com_cod = ${criado.id};`);
        ctx.incluido();
      } catch (e) {
        ctx.recusado(e, titulo);
      }
    }
  }
  if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
}
