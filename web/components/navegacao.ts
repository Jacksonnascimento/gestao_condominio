import {
  Building2,
  CalendarDays,
  FileText,
  House,
  IdCard,
  Megaphone,
  Package,
  TreePine,
  TriangleAlert,
  UserCog,
  Users,
  Wallet,
  type LucideIcon,
} from 'lucide-react';
import type { Permissoes } from '@/context/SessaoContext';

export interface ItemDoMenu {
  rotulo: string;
  href: string;
  icone: LucideIcon;
  /** Tela já migrada para o sistema novo. As outras aparecem no menu marcadas como "em breve". */
  pronta: boolean;
  visivel?: (permissoes: Permissoes) => boolean;
}

export interface GrupoDoMenu {
  titulo?: string;
  itens: ItemDoMenu[];
}

/** Catálogo do menu lateral, na ordem em que aparece. */
export const MENU: GrupoDoMenu[] = [
  {
    itens: [{ rotulo: 'Painel', href: '/', icone: House, pronta: true }],
  },
  {
    titulo: 'Cadastros',
    itens: [
      { rotulo: 'Unidades', href: '/unidades', icone: Building2, pronta: false },
      { rotulo: 'Ocupantes', href: '/ocupantes', icone: Users, pronta: false },
      { rotulo: 'Contratos', href: '/contratos', icone: FileText, pronta: false, visivel: (p) => p.gestao },
    ],
  },
  {
    titulo: 'Portaria',
    itens: [
      { rotulo: 'Visitantes', href: '/visitantes', icone: IdCard, pronta: false },
      { rotulo: 'Encomendas', href: '/encomendas', icone: Package, pronta: true },
    ],
  },
  {
    titulo: 'Convivência',
    itens: [
      { rotulo: 'Áreas comuns', href: '/areas-comuns', icone: TreePine, pronta: false, visivel: (p) => p.gestao },
      { rotulo: 'Reservas', href: '/reservas', icone: CalendarDays, pronta: false },
      { rotulo: 'Comunicados', href: '/comunicados', icone: Megaphone, pronta: false },
      { rotulo: 'Ocorrências', href: '/ocorrencias', icone: TriangleAlert, pronta: false },
    ],
  },
  {
    titulo: 'Administração',
    itens: [
      { rotulo: 'Financeiro', href: '/financeiro', icone: Wallet, pronta: false },
      { rotulo: 'Usuários', href: '/usuarios', icone: UserCog, pronta: false, visivel: (p) => p.administraUsuarios },
    ],
  },
];

/** Atalhos da barra inferior no celular; o resto do menu abre pelo botão "Mais". */
export const ATALHOS_DO_CELULAR = ['/', '/encomendas', '/reservas', '/comunicados'];
