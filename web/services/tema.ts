/** Claro, escuro ou o mesmo do aparelho. É do aparelho, não da conta: fica guardado mesmo depois de sair. */
export type Tema = 'claro' | 'escuro' | 'sistema';

export const CHAVE_TEMA = 'condigtal:tema';

const EVENTO = 'condigtal:tema';
const ESCURO_NO_APARELHO = '(prefers-color-scheme: dark)';

/** Cor da barra do navegador no celular, a mesma do fundo de cada tema (--color-fundo no globals.css). */
const COR_DA_BARRA = { claro: '#f6f2ea', escuro: '#15120e' };

/**
 * Roda no <head>, antes de a página aparecer, para não piscar o tema claro em quem escolheu o escuro. Também cria a
 * cor da barra do navegador, que aplicar() atualiza depois. Repete a regra de temaEfetivo(): fica em texto porque vai
 * direto no HTML.
 */
export const SCRIPT_DO_TEMA = `(function(){var e=false;try{var t=localStorage.getItem('${CHAVE_TEMA}');e=t==='escuro'||(t==='sistema'&&matchMedia('${ESCURO_NO_APARELHO}').matches)}catch(x){}if(e)document.documentElement.dataset.tema='escuro';var m=document.createElement('meta');m.name='theme-color';m.content=e?'${COR_DA_BARRA.escuro}':'${COR_DA_BARRA.claro}';document.head.appendChild(m)})()`;

export function lerTema(): Tema {
  try {
    const guardado = localStorage.getItem(CHAVE_TEMA);
    return guardado === 'escuro' || guardado === 'sistema' ? guardado : 'claro';
  } catch {
    return 'claro';
  }
}

function temaEfetivo(tema: Tema): 'claro' | 'escuro' {
  if (tema === 'sistema') return window.matchMedia(ESCURO_NO_APARELHO).matches ? 'escuro' : 'claro';
  return tema;
}

function aplicar(tema: Tema) {
  const efetivo = temaEfetivo(tema);
  if (efetivo === 'escuro') document.documentElement.dataset.tema = 'escuro';
  else delete document.documentElement.dataset.tema;
  document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => meta.setAttribute('content', COR_DA_BARRA[efetivo]));
}

export function escolherTema(tema: Tema) {
  try {
    localStorage.setItem(CHAVE_TEMA, tema);
  } catch {
    // Sem armazenamento (navegação privada), o tema vale só até recarregar a página
  }
  aplicar(tema);
  window.dispatchEvent(new Event(EVENTO));
}

/** Acompanha a escolha feita em outra aba e, no tema do aparelho, a troca de claro para escuro nele. */
export function acompanharTema(avisar: () => void) {
  const aoMudar = () => {
    aplicar(lerTema());
    avisar();
  };
  const aparelho = window.matchMedia(ESCURO_NO_APARELHO);
  window.addEventListener(EVENTO, avisar);
  window.addEventListener('storage', aoMudar);
  aparelho.addEventListener('change', aoMudar);
  return () => {
    window.removeEventListener(EVENTO, avisar);
    window.removeEventListener('storage', aoMudar);
    aparelho.removeEventListener('change', aoMudar);
  };
}
