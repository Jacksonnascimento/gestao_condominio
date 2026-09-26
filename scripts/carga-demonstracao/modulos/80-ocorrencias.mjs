// Ocorrências dos últimos seis meses, registradas pelos moradores: barulho, vazamentos, lâmpadas, portão, vagas,
// animais. A gestão responde nos comentários (o primeiro coloca a ocorrência em análise), às vezes anexa um orçamento,
// e finaliza com o parecer. As antigas estão resolvidas; as recentes, abertas ou em análise. A API grava as datas do
// momento; a carga as leva no banco para as datas planejadas. Só carrega num condomínio sem nenhuma ocorrência.

import { emParalelo, HOJE, iso, momento, noHorario, pdfBlob, q, somarDias, somarMinutos, sorteio } from '../comum.mjs';
import { gerarPdf } from '../pdf.mjs';

export const descricao = 'ocorrências, com respostas da gestão';

const DIAS = 180;
const QUANTIDADE = { palmeiras: 38, mirante: 14, villaggio: 24 };

// onde: 'predio' (apartamentos), 'casa' (Villaggio) ou 'todos'
const MODELOS = [
  {
    tipo: 'BARULHO', onde: 'predio', titulo: 'Barulho depois das 22h no apartamento de cima',
    descricao: 'Há três noites seguidas o apartamento de cima arrasta móveis e faz barulho de salto depois das 23h. Já tentei conversar, sem resultado.',
    respostas: ['Recebemos a sua reclamação. Vamos enviar uma notificação por escrito à unidade, lembrando o horário de silêncio do regimento.', 'A notificação foi entregue e o morador assinou o recebimento. Se voltar a acontecer, registre aqui com data e horário.'],
    parecer: 'Unidade notificada formalmente. Sem novas reclamações nos 15 dias seguintes.',
  },
  {
    tipo: 'BARULHO', onde: 'casa', titulo: 'Som alto na casa vizinha no fim de semana',
    descricao: 'No sábado a casa ao lado ficou com som alto na área da piscina até quase 2h da manhã.',
    respostas: ['A ronda passou pelo local às 0h40 e pediu a redução do volume. Vamos notificar a unidade.', 'Notificação entregue. Em caso de reincidência, o regimento prevê multa.'],
    parecer: 'Unidade notificada; caso registrado no histórico para eventual multa em reincidência.',
  },
  {
    tipo: 'MANUTENCAO', onde: 'predio', titulo: 'Lâmpada queimada no corredor do meu andar',
    descricao: 'As duas lâmpadas do hall do elevador estão queimadas e o corredor fica escuro à noite.',
    respostas: ['Obrigado pelo aviso. O zelador vai trocar ainda hoje.'],
    parecer: 'Lâmpadas trocadas por LED.',
  },
  {
    tipo: 'MANUTENCAO', onde: 'todos', titulo: 'Vazamento na garagem, perto da minha vaga',
    descricao: 'Está pingando água do teto da garagem bem em cima da minha vaga, e o carro amanhece molhado. Parece vir de um cano.',
    respostas: ['Vamos chamar o encanador para avaliar a origem do vazamento.', 'O vazamento vem da tubulação de águas pluviais. Segue anexo o orçamento do reparo, aprovado pelo conselho.', 'Reparo agendado para quinta-feira de manhã; a vaga ficará isolada por algumas horas.'],
    parecer: 'Tubulação substituída e teste feito com chuva forte no fim de semana: sem vazamento.',
    orcamento: { servico: 'Reparo da tubulação de águas pluviais da garagem', valor: 'R$ 2.480,00' },
  },
  {
    tipo: 'MANUTENCAO', onde: 'todos', titulo: 'Portão da garagem demorando para fechar',
    descricao: 'O portão da garagem está demorando muito para fechar e às vezes volta a abrir sozinho. Fica aberto tempo demais.',
    respostas: ['A empresa de automação foi chamada. Por segurança, o porteiro vai acompanhar o fechamento até o conserto.', 'Técnico identificou desgaste na cremalheira. Segue o orçamento da troca.'],
    parecer: 'Cremalheira e sensor de obstáculo trocados. Portão funcionando normalmente.',
    orcamento: { servico: 'Troca da cremalheira e do sensor do portão da garagem', valor: 'R$ 1.350,00' },
  },
  {
    tipo: 'MANUTENCAO', onde: 'predio', titulo: 'Elevador parando desnivelado',
    descricao: 'O elevador social tem parado uns 5 cm abaixo do piso em alguns andares. Minha mãe quase tropeçou.',
    respostas: ['Acionamos a empresa de manutenção dos elevadores, que virá ainda hoje.', 'Foi feito o ajuste do nivelamento e a troca de um sensor de posição.'],
    parecer: 'Nivelamento ajustado pela empresa de manutenção. Acompanhamos por uma semana sem novos problemas.',
  },
  {
    tipo: 'MANUTENCAO', onde: 'casa', titulo: 'Poste da rua sem luz',
    descricao: 'O poste em frente à minha casa está apagado há quase uma semana e a rua fica muito escura.',
    respostas: ['Obrigado pelo aviso. A equipe de manutenção vai verificar a luminária.'],
    parecer: 'Reator e lâmpada trocados. Poste incluído na próxima etapa da troca para LED.',
  },
  {
    tipo: 'MANUTENCAO', onde: 'todos', titulo: 'Interfone da minha unidade não funciona',
    descricao: 'O interfone não toca quando a portaria chama. Já perdi duas entregas por isso.',
    respostas: ['Vamos verificar a central de interfonia; pode ser a fiação da prumada.', 'O problema era no aparelho da unidade, que é de responsabilidade do morador. Indicamos a empresa que atende o condomínio.'],
    parecer: 'Defeito no aparelho da unidade. Morador orientado a fazer a troca por conta própria.',
  },
  {
    tipo: 'RECLAMACAO', onde: 'todos', titulo: 'Carro estacionado na minha vaga',
    descricao: 'Pela segunda vez nesta semana encontrei um carro prata estacionado na minha vaga quando cheguei do trabalho.',
    respostas: ['A portaria identificou o veículo pelas câmeras; é de uma visita da unidade vizinha. Vamos orientar o morador.'],
    parecer: 'Morador responsável orientado sobre o uso das vagas de visitantes.',
  },
  {
    tipo: 'RECLAMACAO', onde: 'todos', titulo: 'Cachorro sem guia na área comum',
    descricao: 'Um cachorro grande estava solto perto do playground e assustou as crianças. O tutor não recolheu a sujeira.',
    respostas: ['Vamos identificar o tutor pelas câmeras e notificar.', 'Tutor notificado. Reforçamos a regra no comunicado da semana.'],
    parecer: 'Tutor notificado por escrito; regra reforçada em comunicado.',
  },
  {
    tipo: 'RECLAMACAO', onde: 'predio', titulo: 'Bitucas de cigarro jogadas na minha varanda',
    descricao: 'Toda semana aparecem bitucas de cigarro na minha varanda, jogadas de algum andar de cima.',
    respostas: ['Vamos enviar um aviso a todas as unidades da prumada de cima.'],
    parecer: 'Aviso entregue às unidades da prumada. O morador informou que não apareceram mais bitucas.',
  },
  {
    tipo: 'CONFLITO', onde: 'todos', titulo: 'Desentendimento com o vizinho sobre a vaga de garagem',
    descricao: 'O vizinho da vaga ao lado estaciona encostado na faixa e não consigo abrir a porta do carro. Conversamos e ele ficou agressivo.',
    respostas: ['Sentimos muito pelo ocorrido. Vamos marcar uma conversa com as duas unidades, com a mediação da administração.', 'A reunião foi marcada para sexta-feira às 19h, na administração.'],
    parecer: 'Mediação feita. As duas unidades concordaram em respeitar a faixa, que foi repintada.',
  },
  {
    tipo: 'OUTRO', onde: 'todos', titulo: 'Sugestão: bicicletário coberto',
    descricao: 'Muitos moradores usam bicicleta e elas ficam no sol e na chuva. Sugiro um bicicletário coberto perto da portaria.',
    respostas: ['Ótima sugestão. Vamos levar para a pauta da próxima assembleia com um orçamento.'],
    parecer: 'Sugestão incluída na pauta da próxima assembleia, com orçamento anexado ao edital.',
  },
  {
    tipo: 'MANUTENCAO', onde: 'todos', titulo: 'Infiltração no teto do banheiro',
    descricao: 'Apareceu uma mancha de umidade no teto do banheiro, que está crescendo. Acho que vem da unidade de cima ou da área comum.',
    respostas: ['Vamos agendar uma vistoria com o zelador para identificar a origem.', 'A vistoria indicou que a infiltração vem do ralo da unidade de cima. O proprietário de lá foi comunicado.'],
    parecer: 'Origem na unidade superior; o reparo ficou a cargo do proprietário dela, conforme o regimento.',
  },
];

function orcamentoPdf(condominio, o) {
  return pdfBlob(gerarPdf({
    cabecalho: condominio.dados.nome, subcabecalho: 'Orçamento recebido pela administração',
    titulo: `Orçamento — ${o.servico}`,
    linhas: [`Serviço: ${o.servico}.`, `Valor total: ${o.valor}, com material e mão de obra.`,
      'Prazo de execução: até 5 dias úteis depois da aprovação.', 'Garantia: 6 meses sobre o serviço executado.',
      'Validade da proposta: 15 dias.'],
    rodape: 'Anexado à ocorrência pela administração',
  }));
}

function planejar(c, familias) {
  const s = sorteio(`ocorrencias|${c.chave}|${iso(HOJE)}`);
  const agora = new Date();
  const modelos = MODELOS.filter((m) => m.onde === 'todos' || m.onde === (c.chave === 'villaggio' ? 'casa' : 'predio'));
  const lista = [];
  for (let i = 0; i < QUANTIDADE[c.chave]; i++) {
    const idade = Math.floor(Math.pow(s.r(), 1.3) * DIAS); // um pouco mais de ocorrências recentes
    let registro = noHorario(somarDias(HOJE, -idade), s.int(7, 22), s.int(0, 59));
    if (registro > agora) registro = somarMinutos(agora, -s.int(30, 240));
    const modelo = s.um(modelos);
    const status = idade > 20 ? s.pesado([['RESOLVIDA', 90], ['EM_ANALISE', 10]])
      : idade > 4 ? s.pesado([['RESOLVIDA', 50], ['EM_ANALISE', 40], ['ABERTA', 10]])
        : s.pesado([['ABERTA', 55], ['EM_ANALISE', 45]]);
    const familia = s.um(familias);
    // Respostas espaçadas: a primeira entre 1 hora e 2 dias, as outras a cada 1 a 4 dias
    const quantas = status === 'ABERTA' ? 0 : status === 'EM_ANALISE' ? Math.max(1, modelo.respostas.length - 1) : modelo.respostas.length;
    let quando = registro;
    const respostas = [];
    for (let r = 0; r < quantas; r++) {
      quando = somarMinutos(quando, r === 0 ? s.int(60, 2880) : s.int(1440, 5760));
      if (quando > agora) break;
      respostas.push({ texto: modelo.respostas[r], quando, anexo: modelo.orcamento && r === 1 });
    }
    let finalizacao = status === 'RESOLVIDA' ? somarMinutos(quando, s.int(720, 4320)) : null;
    if (finalizacao && finalizacao > agora) finalizacao = null;
    lista.push({
      modelo, familia, morador: familia.comAcesso[0], registro, respostas, finalizacao,
      gestores: s.alguns(['sindico', 'admin', 'gerente'], 3),
    });
  }
  return lista;
}

export async function carregar(ctx) {
  const { api, mundo } = ctx;
  await ctx.resolverUnidades();

  for (const c of mundo.CONDOMINIOS) {
    const condominioId = ctx.ids.condominio.get(c.chave);
    if (!condominioId) continue;
    const totais = await api.get(ctx.admin, `/ocorrencias/totais?condominioId=${condominioId}`);
    if (totais.TOTAL > 0) {
      ctx.log(`${c.dados.nome}: já tem ${totais.TOTAL} ocorrências; nada a fazer`);
      ctx.existente(totais.TOTAL);
      continue;
    }
    const gestao = mundo.GESTAO[c.chave];
    const gestorDa = (o, i) => gestao[o.gestores[i % o.gestores.length]] ?? gestao.sindico;
    const plano = planejar(c, mundo.familiasComAcesso(c.chave));
    const ajustes = [];
    await emParalelo(plano, 4, async (o) => {
      try {
        const criada = await api.post(await ctx.token(o.morador), '/ocorrencias', {
          unidadeId: ctx.ids.unidade.get(o.familia.unidade.chave), tipo: o.modelo.tipo, titulo: o.modelo.titulo,
          descricao: o.modelo.descricao, condominioId,
        });
        let ultima = o.registro;
        for (const [i, r] of o.respostas.entries()) {
          const token = await ctx.token(gestorDa(o, i));
          const comentario = await api.post(token, `/ocorrencias/${criada.id}/comentarios`, { comentario: r.texto });
          ajustes.push(`update gc_ocorrencia_comentario set occ_dt_comentario = ${q(momento(r.quando))} where occ_cod = ${comentario.id};`);
          if (r.anexo) {
            const anexo = await api.enviar(token, 'POST', `/ocorrencias/${criada.id}/anexos`,
              [['anexo', orcamentoPdf(c, o.modelo.orcamento), `Orçamento - ${o.modelo.orcamento.servico}.pdf`]]);
            ajustes.push(`update gc_ocorrencia_anexo set oca_dt_anexo = ${q(momento(somarMinutos(r.quando, 2)))} where oca_cod = ${anexo.id};`);
          }
          ultima = r.quando;
        }
        if (o.finalizacao) {
          await api.post(await ctx.token(gestorDa(o, 0)), `/ocorrencias/${criada.id}/finalizacao`, { parecerFinal: o.modelo.parecer });
          ultima = o.finalizacao;
        }
        ajustes.push(`update gc_ocorrencia set oco_dt_registro = ${q(momento(o.registro))}, oco_dt_atualizacao = ${q(momento(ultima))},
          oco_dt_finalizacao = ${o.finalizacao ? q(momento(o.finalizacao)) : 'NULL'} where oco_cod = ${criada.id};`);
        ctx.incluido();
      } catch (erro) {
        ctx.recusado(erro, `ocorrência "${o.modelo.titulo}" (${c.chave})`);
      }
    });
    if (ajustes.length) ctx.banco.consultar(ajustes.join('\n'));
    ctx.log(`${c.dados.nome}: ${plano.length} ocorrências`);
  }
}
