import type { Metadata, Viewport } from 'next';
import { Instrument_Serif, Manrope } from 'next/font/google';
import { Toaster } from 'react-hot-toast';
import { ConfirmacaoDoSistema } from '@/components/ConfirmacaoDoSistema';
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

export const viewport: Viewport = {
  themeColor: '#f6f2ea',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <body className={`${manrope.variable} ${instrument.variable} antialiased`} suppressHydrationWarning>
        {children}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 6000,
            style: { fontSize: '14px', maxWidth: '420px', color: '#1f1b16', border: '1px solid #e6ddcd' },
          }}
        />
        <ConfirmacaoDoSistema />
      </body>
    </html>
  );
}
