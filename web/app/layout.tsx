import type { Metadata } from 'next';
import { Instrument_Serif, Manrope } from 'next/font/google';
import { Toaster } from 'react-hot-toast';
import { ConfirmacaoDoSistema } from '@/components/ConfirmacaoDoSistema';
import { SCRIPT_DO_TEMA } from '@/services/tema';
import './globals.css';

const manrope = Manrope({
  variable: '--font-manrope',
  subsets: ['latin'],
});

// Só nos títulos de página e nos números de destaque
const instrument = Instrument_Serif({
  variable: '--font-instrument',
  subsets: ['latin'],
  weight: '400',
});

export const metadata: Metadata = {
  title: 'Condigtal',
  description: 'Gestão de condomínio: portaria, encomendas, reservas, comunicados e ocorrências.',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    // O script do tema muda o data-tema do <html> antes de o React assumir a página
    <html lang="pt-BR" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: SCRIPT_DO_TEMA }} />
      </head>
      <body className={`${manrope.variable} ${instrument.variable} antialiased`} suppressHydrationWarning>
        {children}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 6000,
            style: {
              fontSize: '14px',
              maxWidth: '420px',
              color: 'var(--color-tinta)',
              background: 'var(--color-superficie)',
              border: '1px solid var(--color-borda)',
            },
          }}
        />
        <ConfirmacaoDoSistema />
      </body>
    </html>
  );
}
