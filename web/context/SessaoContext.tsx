'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { autenticacaoService, type Papel, type UsuarioLogado } from '@/services/autenticacaoService';
import { condominioService } from '@/services/condominioService';
import { encerrarSessao } from '@/services/sessao';

export interface CondominioDaSessao {
  id: number;
  nome: string;
}

/** O que quem está logado pode fazer no condomínio escolhido. A API confere de novo em cada chamada. */
export interface Permissoes {
  /** Síndico, administração ou administrador geral. */
  gestao: boolean;
  /** Gestão ou portaria. */
  portaria: boolean;
  /** Pode dar e tirar acesso de outras pessoas ao condomínio. */
  administraUsuarios: boolean;
  /** Administrador geral da instalação: cadastra condomínios. */
  administradorGeral: boolean;
}

interface Sessao {
  usuario: UsuarioLogado | null;
  condominios: CondominioDaSessao[];
  condominio: CondominioDaSessao | null;
  trocarCondominio: (id: number) => void;
  papeis: Papel[];
  /** Nome do papel no condomínio escolhido, para mostrar junto do nome ("Síndica", "Morador"...). */
  descricaoDoPapel: string;
  permissoes: Permissoes;
  carregando: boolean;
  /** Lê de novo quem está logado e os condomínios: depois de mudar o próprio nome ou cadastrar um condomínio. */
  recarregar: () => void;
  sair: () => void;
}

const CHAVE_CONDOMINIO = 'condigtal:condominio';

const SessaoContext = createContext<Sessao | null>(null);

export function SessaoProvider({ children }: { children: React.ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(null);
  const [condominios, setCondominios] = useState<CondominioDaSessao[]>([]);
  const [condominioId, setCondominioId] = useState<number | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    let ativo = true;
    (async () => {
      try {
        const eu = await autenticacaoService.eu();
        // O administrador geral vê todos os condomínios; os demais, os dos seus vínculos
        const lista: CondominioDaSessao[] = eu.administradorGeral
          ? (await condominioService.listar()).map((c) => ({ id: c.id, nome: c.nome }))
          : eu.vinculos
              .filter((v, i, todos) => todos.findIndex((o) => o.condominioCodigo === v.condominioCodigo) === i)
              .map((v) => ({ id: v.condominioCodigo, nome: v.condominioNome || 'Condomínio' }));
        if (!ativo) return;
        let guardado: number | null = null;
        try {
          guardado = Number(localStorage.getItem(CHAVE_CONDOMINIO)) || null;
        } catch {
          guardado = null;
        }
        setUsuario(eu);
        setCondominios(lista);
        // Numa recarga, fica o condomínio que já estava escolhido, se ele continuar na lista
        setCondominioId((atual) => {
          if (lista.some((c) => c.id === atual)) return atual;
          return lista.some((c) => c.id === guardado) ? guardado : (lista[0]?.id ?? null);
        });
      } catch {
        // Sem /auth/eu não há como montar as telas; o 401 já leva ao login, os demais erros também
        if (ativo) encerrarSessao(true);
      } finally {
        if (ativo) setCarregando(false);
      }
    })();
    return () => {
      ativo = false;
    };
  }, [versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  const trocarCondominio = useCallback((id: number) => {
    setCondominioId(id);
    try {
      localStorage.setItem(CHAVE_CONDOMINIO, String(id));
    } catch {
      // Sem armazenamento no navegador a escolha vale só até recarregar a página
    }
  }, []);

  const valor = useMemo<Sessao>(() => {
    const condominio = condominios.find((c) => c.id === condominioId) ?? null;
    const vinculos = usuario?.vinculos.filter((v) => v.condominioCodigo === condominioId) ?? [];
    const papeis = vinculos.map((v) => v.papel);
    const admin = usuario?.administradorGeral === true;
    const tem = (...lista: Papel[]) => papeis.some((p) => lista.includes(p));
    const gestao = admin || tem('SINDICO', 'ADMIN', 'FUNCIONARIO_ADM');
    return {
      usuario,
      condominios,
      condominio,
      trocarCondominio,
      papeis,
      descricaoDoPapel: admin ? 'Administrador geral' : vinculos.map((v) => v.papelDescricao).join(' · '),
      permissoes: {
        gestao,
        portaria: gestao || tem('PORTEIRO'),
        administraUsuarios: admin || tem('SINDICO', 'ADMIN'),
        administradorGeral: admin,
      },
      carregando,
      recarregar,
      sair: () => encerrarSessao(false),
    };
  }, [usuario, condominios, condominioId, trocarCondominio, carregando, recarregar]);

  return <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>;
}

export function useSessao(): Sessao {
  const sessao = useContext(SessaoContext);
  if (!sessao) throw new Error('useSessao fora do SessaoProvider');
  return sessao;
}
