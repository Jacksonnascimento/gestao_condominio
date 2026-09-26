'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { FotoDoPerfil } from '@/components/perfil/FotoDoPerfil';
import { TrocarSenha } from '@/components/perfil/TrocarSenha';
import { formatarCpf } from '@/components/usuarios/cpf';
import { Botao, CabecalhoDaPagina, Campo, Cartao, Selo, TituloDoCartao, Vazio } from '@/components/Interface';
import { useSessao } from '@/context/SessaoContext';
import type { UsuarioLogado } from '@/services/autenticacaoService';
import { perfilService } from '@/services/perfilService';
import { mensagemErroApi } from '@/services/utilitarios';

function documento(cpfCnpj: string | null): string {
  if (!cpfCnpj) return '';
  return cpfCnpj.replace(/\D/g, '').length === 11 ? formatarCpf(cpfCnpj) : cpfCnpj;
}

/** Nome e telefones, que a própria pessoa altera. E-mail e CPF só a administração muda. */
function DadosPessoais({ perfil, aoSalvar }: { perfil: UsuarioLogado; aoSalvar: (novo: UsuarioLogado) => void }) {
  const [nome, setNome] = useState(perfil.nome);
  const [telefone, setTelefone] = useState(perfil.telefone ?? '');
  const [telefone2, setTelefone2] = useState(perfil.telefone2 ?? '');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      // Telefone vazio apaga; nulo manteria o anterior
      const novo = await perfilService.atualizar({ nome: nome.trim(), telefone: telefone.trim(), telefone2: telefone2.trim() });
      toast.success('Dados salvos.');
      aoSalvar(novo);
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível salvar os seus dados.'));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Cartao>
      <TituloDoCartao titulo="Dados pessoais" />
      <form onSubmit={salvar} className="flex flex-col gap-5 border-t border-borda-suave px-5 py-5">
        <div className="flex items-center gap-4">
          <FotoDoPerfil nome={perfil.nome} possuiFoto={perfil.possuiFoto} />
          <div className="flex min-w-0 flex-col gap-0.5">
            <span className="truncate text-lg font-extrabold">{perfil.nome}</span>
            <span className="truncate text-sm text-apagado">{perfil.email}</span>
          </div>
        </div>
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo" role="alert">
            {erro}
          </p>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            rotulo="Nome"
            autoComplete="name"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            obrigatorio
            maxLength={100}
            className="sm:col-span-2"
          />
          <Campo rotulo="Telefone" type="tel" autoComplete="tel" value={telefone} onChange={(e) => setTelefone(e.target.value)} maxLength={20} />
          <Campo rotulo="Outro telefone" type="tel" value={telefone2} onChange={(e) => setTelefone2(e.target.value)} maxLength={20} />
          <Campo
            rotulo="E-mail"
            value={perfil.email}
            readOnly
            ajuda="É com ele que você entra. Para mudar, fale com a administração do condomínio."
            className="[&_input]:bg-cabecalho [&_input]:text-tinta-2"
          />
          <Campo rotulo="CPF" value={documento(perfil.cpfCnpj)} readOnly className="[&_input]:bg-cabecalho [&_input]:text-tinta-2" />
        </div>
        <div className="flex justify-end">
          <Botao type="submit" variante="primario" carregando={salvando} className="max-sm:w-full">
            Salvar dados
          </Botao>
        </div>
      </form>
    </Cartao>
  );
}

function Acessos({ perfil }: { perfil: UsuarioLogado }) {
  return (
    <Cartao>
      <TituloDoCartao titulo="Seus acessos" />
      {perfil.administradorGeral && (
        <p className="border-t border-borda-suave px-5 py-3.5 text-sm">
          <Selo tom="aviso">Administrador geral</Selo>
          <span className="mt-1.5 block text-[13px] text-apagado">Você vê e administra todos os condomínios do sistema.</span>
        </p>
      )}
      {perfil.vinculos.length === 0 && !perfil.administradorGeral && <Vazio>Você ainda não tem acesso a nenhum condomínio.</Vazio>}
      {perfil.vinculos.length > 0 && (
        <ul className="m-0 list-none border-t border-borda-suave p-0">
          {perfil.vinculos.map((v) => (
            <li
              key={`${v.condominioCodigo}-${v.papel}`}
              className="flex items-center justify-between gap-3 border-b border-borda-suave px-5 py-3 text-sm last:border-b-0"
            >
              <span className="min-w-0 font-bold">{v.condominioNome || 'Condomínio'}</span>
              <Selo tom="neutro">{v.papelDescricao}</Selo>
            </li>
          ))}
        </ul>
      )}
    </Cartao>
  );
}

export default function PaginaDoPerfil() {
  const { recarregar } = useSessao();
  const [perfil, setPerfil] = useState<UsuarioLogado | null>(null);
  const [falhou, setFalhou] = useState(false);

  useEffect(() => {
    let ativa = true;
    perfilService
      .eu()
      .then((eu) => {
        if (ativa) setPerfil(eu);
      })
      .catch((e) => {
        if (!ativa) return;
        setFalhou(true);
        toast.error(mensagemErroApi(e, 'Não foi possível carregar os seus dados.'));
      });
    return () => {
      ativa = false;
    };
  }, []);

  return (
    <div className="flex flex-col gap-5">
      <CabecalhoDaPagina secao="Sua conta" titulo="Meu perfil" />
      {!perfil && (
        <Cartao aria-busy={!falhou}>
          <Vazio>{falhou ? 'Não foi possível carregar os seus dados. Tente atualizar a página.' : 'Carregando…'}</Vazio>
        </Cartao>
      )}
      {perfil && (
        <div className="grid gap-5 xl:grid-cols-[1.6fr_1fr]">
          <div className="flex flex-col gap-5">
            <DadosPessoais
              perfil={perfil}
              aoSalvar={(novo) => {
                setPerfil(novo);
                // O nome no menu e no topo acompanha a mudança
                recarregar();
              }}
            />
            <TrocarSenha />
          </div>
          <div className="flex flex-col gap-5 self-start">
            <Acessos perfil={perfil} />
          </div>
        </div>
      )}
    </div>
  );
}
