import { EstruturaDoSistema } from '@/components/EstruturaDoSistema';

// Todas as telas de quem está logado ficam neste grupo e dividem o menu, o topo e a sessão, que não se
// recarregam ao trocar de tela.
export default function LayoutDoSistema({ children }: { children: React.ReactNode }) {
  return <EstruturaDoSistema>{children}</EstruturaDoSistema>;
}
