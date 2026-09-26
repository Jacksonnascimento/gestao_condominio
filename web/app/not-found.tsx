import Link from 'next/link';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';

export default function NaoEncontrada() {
  return (
    <TelaDeAcesso titulo="Página não encontrada" subtitulo="O endereço pode ter mudado, ou esta tela ainda não existe no sistema novo.">
      <Link href="/" className="text-sm font-semibold">
        Voltar ao painel
      </Link>
    </TelaDeAcesso>
  );
}
