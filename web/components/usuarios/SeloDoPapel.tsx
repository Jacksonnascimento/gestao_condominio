import { Selo, type TomDoSelo } from '@/components/Interface';
import type { AcessoDeUsuario } from '@/services/usuarioService';
import { descricaoDoEnum, valorDoEnum } from '@/services/utilitarios';

const TOM: Record<string, TomDoSelo> = {
  SINDICO: 'aviso',
  ADMIN: 'aviso',
  FUNCIONARIO_ADM: 'info',
  PORTEIRO: 'info',
  MORADOR: 'neutro',
};

export function SeloDoPapel({ acesso }: { acesso: AcessoDeUsuario }) {
  const papel = valorDoEnum(acesso.papel);
  return <Selo tom={TOM[papel] ?? 'neutro'}>{descricaoDoEnum(acesso.papel, acesso.papelDescricao)}</Selo>;
}
