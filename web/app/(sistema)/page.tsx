'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { EntregarEncomenda } from '@/components/encomendas/EntregarEncomenda';
import { RegistrarEncomenda } from '@/components/encomendas/RegistrarEncomenda';
import { Botao, Cartao, Selo, TituloDoCartao, Vazio } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import { encomendaService, type Encomenda, type OpcoesEncomenda } from '@/services/encomendaService';
import { painelService, type Indicadores, type OcorrenciaResumo, type ReservaResumo } from '@/services/painelService';
import {
  diaDaSemanaCurto,
  formatarMomento,
  hojeParaCampo,
  mensagemErroApi,
  rotuloUnidade,
  statusDoErro,
} from '@/services/utilitarios';

function saudacao(): string {
  const hora = new Date().getHours();
  if (hora < 12) return 'Bom dia';
  if (hora < 18) return 'Boa tarde';
  return 'Boa noite';
}

function hojePorExtenso(): string {
  const texto = format(new Date(), "EEEE, d 'de' MMMM", { locale: ptBR });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** Uma seção que não é do papel de quem está logado (403) some do painel, sem aviso de erro. */
async function sePermitido<T>(chamada: Promise<T>, aoFalhar: (e: unknown) => void): Promise<T | null> {
  try {
    return await chamada;
  } catch (e) {
    if (statusDoErro(e) !== 403) aoFalhar(e);
    return null;
  }
}

function CartaoDeIndicador({ rotulo, valor, destaque, href }: { rotulo: string; valor: number; destaque?: boolean; href: string }) {
  return (
    <Link
      href={href}
      className={`flex items-end justify-between gap-3 rounded-2xl px-[22px] py-[18px] no-underline transition-colors ${
        destaque ? 'bg-tinta text-fundo hover:bg-tinta-2 hover:text-fundo' : 'border border-borda bg-superficie text-tinta hover:border-contorno hover:text-tinta'
      }`}
    >
      <span className={`text-[13px] font-semibold ${destaque ? 'text-apagado-inverso' : 'text-apagado'}`}>{rotulo}</span>
      <span className={`font-titulo text-5xl leading-[0.9] ${destaque && valor > 0 ? 'text-ouro-claro' : ''}`}>{valor}</span>
    </Link>
  );
}

export default function PaginaDoPainel() {
  const { usuario, condominio, permissoes } = useSessao();
  const [indicadores, setIndicadores] = useState<Indicadores | null>(null);
  const [encomendas, setEncomendas] = useState<Encomenda[] | null>(null);
  const [opcoes, setOpcoes] = useState<OpcoesEncomenda | null>(null);
  const [reservas, setReservas] = useState<ReservaResumo[] | null>(null);
  const [ocorrencias, setOcorrencias] = useState<OcorrenciaResumo[] | null>(null);
  const [janela, setJanela] = useState<{ tipo: 'registrar' } | { tipo: 'entregar'; encomenda: Encomenda } | null>(null);

  // Sobe a cada registro ou entrega de encomenda, para o painel ser lido de novo
  const [versao, setVersao] = useState(0);

  const condominioId = condominio?.id;

  useEffect(() => {
    let ativo = true;
    const avisar = (e: unknown) => toast.error(mensagemErroApi(e, 'Parte do painel não carregou. Tente atualizar a página.'));
    Promise.all([
      sePermitido(painelService.indicadores(condominioId), avisar),
      sePermitido(encomendaService.listar({ condominioId, status: 'PENDENTE', tamanho: 5 }), avisar),
      sePermitido(encomendaService.opcoes(), avisar),
      sePermitido(painelService.proximasReservas(condominioId, hojeParaCampo()), avisar),
      sePermitido(painelService.ocorrenciasAbertas(condominioId), avisar),
    ]).then(([novosIndicadores, novasEncomendas, novasOpcoes, novasReservas, novasOcorrencias]) => {
      if (!ativo) return;
      setIndicadores(novosIndicadores);
      setEncomendas(novasEncomendas?.itens ?? null);
      setOpcoes(novasOpcoes);
      setReservas(
        novasReservas
          ? novasReservas.itens
              .filter((r) => r.status === 'APROVADA' || r.status === 'PENDENTE_APROVACAO')
              .sort((a, b) => a.data.localeCompare(b.data))
              .slice(0, 3)
          : null,
      );
      setOcorrencias(novasOcorrencias?.itens ?? null);
    });
    return () => {
      ativo = false;
    };
  }, [condominioId, versao]);

  const aposAlterar = () => {
    setJanela(null);
    setVersao((v) => v + 1);
  };

  const podeGerenciar = opcoes?.podeGerenciar === true;
  const primeiroNome = usuario?.nome.split(' ')[0] ?? '';

  const cartoes = [
    { rotulo: 'Unidades ativas', valor: indicadores?.totalUnidades, href: '/unidades' },
    { rotulo: 'Ocupantes', valor: indicadores?.totalOcupantes, href: '/ocupantes' },
    { rotulo: 'Contratos ativos', valor: indicadores?.totalContratosAtivos, href: '/contratos' },
    { rotulo: 'Ocorrências pendentes', valor: indicadores?.totalOcorrenciasPendentes, href: '/ocorrencias', destaque: true },
  ].filter((c): c is { rotulo: string; valor: number; href: string; destaque?: boolean } => typeof c.valor === 'number');

  return (
    <div className="flex flex-col gap-[22px]">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-apagado">{hojePorExtenso()}</span>
          <h1 className="font-titulo text-[36px] leading-none font-normal sm:text-[42px]">
            {saudacao()}, {primeiroNome}
          </h1>
        </div>
        {podeGerenciar && (
          <Botao variante="primario" onClick={() => setJanela({ tipo: 'registrar' })}>
            <Plus size={16} className="text-ouro-claro" strokeWidth={2.2} aria-hidden />
            Registrar encomenda
          </Botao>
        )}
      </div>

      {cartoes.length > 0 && (
        <section aria-label="Indicadores" className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
          {cartoes.map((c) => (
            <CartaoDeIndicador key={c.rotulo} rotulo={c.rotulo} valor={c.valor} destaque={c.destaque} href={c.href} />
          ))}
        </section>
      )}

      <div className="grid gap-5 xl:grid-cols-[1.9fr_1fr]">
        {encomendas && (
          <Cartao className="self-start">
            <TituloDoCartao titulo={permissoes.portaria ? 'Encomendas aguardando retirada' : 'Suas encomendas na portaria'}>
              <Link href="/encomendas" className="text-sm font-bold">
                Ver todas
              </Link>
            </TituloDoCartao>
            {encomendas.length === 0 ? (
              <Vazio>Nenhuma encomenda aguardando retirada.</Vazio>
            ) : (
              <>
                <div className="hidden grid-cols-[110px_1.4fr_1fr_1fr_110px] gap-3 border-y border-borda-suave bg-cabecalho px-5 py-[9px] text-xs font-bold tracking-[0.04em] text-apagado uppercase md:grid">
                  <span>Unidade</span>
                  <span>Destinatário</span>
                  <span>Tipo</span>
                  <span>Recebida</span>
                  <span />
                </div>
                <ul className="m-0 list-none border-t border-borda-suave p-0 md:border-t-0">
                  {encomendas.map((e) => (
                    <li
                      key={e.id}
                      className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-0.5 border-b border-borda-suave px-5 py-3 text-sm tabular-nums last:border-b-0 md:min-h-[52px] md:grid-cols-[110px_1.4fr_1fr_1fr_110px] md:py-1.5"
                    >
                      <span className="font-extrabold">{rotuloUnidade(e.unidadeNumero, e.unidadeBloco)}</span>
                      <span className="text-apagado md:hidden">{formatarMomento(e.dataRecebimento)}</span>
                      <span className="col-span-2 md:col-span-1">
                        {e.destinatario}
                        <span className="text-tinta-2 md:hidden"> · {e.tipoDescricao ?? e.tipo}</span>
                      </span>
                      <span className="hidden text-tinta-2 md:block">{e.tipoDescricao ?? e.tipo}</span>
                      <span className="hidden text-apagado md:block">{formatarMomento(e.dataRecebimento)}</span>
                      {podeGerenciar ? (
                        <span className="col-span-2 mt-2 md:col-span-1 md:mt-0 md:justify-self-end">
                          <Botao pequeno onClick={() => setJanela({ tipo: 'entregar', encomenda: e })}>
                            Entregar
                          </Botao>
                        </span>
                      ) : (
                        <span className="hidden md:block" />
                      )}
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Cartao>
        )}

        <div className="flex flex-col gap-5">
          {ocorrencias && (
            <Cartao>
              <TituloDoCartao titulo="Ocorrências abertas">
                <Link href="/ocorrencias" className="text-sm font-bold">
                  Todas
                </Link>
              </TituloDoCartao>
              {ocorrencias.length === 0 ? (
                <Vazio>Nenhuma ocorrência aberta.</Vazio>
              ) : (
                <ul className="m-0 list-none border-t border-borda-suave p-0">
                  {ocorrencias.map((o) => (
                    <li key={o.id} className="flex flex-col gap-[5px] border-b border-borda-suave px-5 py-3 last:border-b-0">
                      <span className="flex items-start justify-between gap-2">
                        <span className="text-sm font-bold">{o.titulo}</span>
                        <Selo tom="aviso">Aberta</Selo>
                      </span>
                      <span className="text-[13px] text-apagado">
                        {[rotuloUnidade(o.unidadeNumero, o.unidadeBloco) || 'Área comum', formatarMomento(o.dataRegistro)].join(' · ')}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </Cartao>
          )}

          {reservas && (
            <Cartao>
              <TituloDoCartao titulo="Próximas reservas">
                <Link href="/reservas" className="text-sm font-bold">
                  Agenda
                </Link>
              </TituloDoCartao>
              {reservas.length === 0 ? (
                <Vazio>Nenhuma reserva marcada.</Vazio>
              ) : (
                <ul className="m-0 flex list-none flex-col gap-3.5 px-5 pt-0.5 pb-[18px]">
                  {reservas.map((r) => (
                    <li key={r.codigo} className="flex items-center gap-3.5">
                      <span className="flex size-12 shrink-0 flex-col items-center justify-center rounded-xl border border-borda">
                        <span className="text-[10px] font-extrabold text-ouro">{diaDaSemanaCurto(r.data)}</span>
                        <span className="font-titulo text-[22px] leading-none">{r.data.slice(8, 10)}</span>
                      </span>
                      <span className="flex min-w-0 flex-col gap-0.5">
                        <span className="text-sm font-bold">{r.areaNome}</span>
                        <span className="text-[13px] text-apagado">
                          {[
                            rotuloUnidade(r.unidadeNumero, r.unidadeBloco),
                            r.turnoNome,
                            r.status === 'PENDENTE_APROVACAO' ? 'aguarda aprovação' : null,
                          ]
                            .filter(Boolean)
                            .join(' · ')}
                        </span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </Cartao>
          )}
        </div>
      </div>

      {janela?.tipo === 'registrar' && opcoes && (
        <RegistrarEncomenda tipos={opcoes.tipos} aoFechar={() => setJanela(null)} aoRegistrar={aposAlterar} />
      )}
      {janela?.tipo === 'entregar' && (
        <EntregarEncomenda encomenda={janela.encomenda} aoFechar={() => setJanela(null)} aoEntregar={aposAlterar} />
      )}
    </div>
  );
}
