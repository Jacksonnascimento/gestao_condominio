'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, CaixaDeErro, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import type { Papel } from '@/services/autenticacaoService';
import { digitosDoCpf, formatarCpf } from '@/services/documentos';
import type { Opcao } from '@/services/tipos';
import {
  usuarioService,
  type AcaoSenha,
  type NovoAcesso,
  type OcupanteSemAcesso,
  type PessoaCadastrada,
} from '@/services/usuarioService';
import { mensagemErroApi, rotuloUnidade, statusDoErro } from '@/services/utilitarios';

/** Resultado da procura pelo CPF: a pessoa, ou null quando não há cadastro. */
interface ConsultaDoCpf {
  cpf: string;
  pessoa: PessoaCadastrada | null;
}

/**
 * Dá acesso ao condomínio a alguém. Morador é escolhido entre os ocupantes das unidades que ainda não têm acesso;
 * os demais papéis são pelo CPF: se a pessoa já tem cadastro, recebe o acesso; senão, é cadastrada aqui.
 */
export function DarAcesso({
  condominio,
  papeis,
  acoesSenha,
  aoFechar,
  aoSalvar,
}: {
  condominio: { id: number; nome: string };
  papeis: Opcao[];
  acoesSenha: Opcao[];
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const [papel, setPapel] = useState('');
  const [ocupantes, setOcupantes] = useState<OcupanteSemAcesso[] | null>(null);
  const [pessoaId, setPessoaId] = useState('');
  const [cpf, setCpf] = useState('');
  const [consultaDoCpf, setConsultaDoCpf] = useState<ConsultaDoCpf | null>(null);
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [telefone, setTelefone] = useState('');
  const [acaoSenha, setAcaoSenha] = useState<AcaoSenha>('ENVIAR_LINK');
  const [senha, setSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let ativa = true;
    usuarioService
      .ocupantesSemAcesso(condominio.id)
      .then((lista) => {
        if (ativa) setOcupantes(lista);
      })
      .catch((e) => {
        if (!ativa) return;
        setOcupantes([]);
        setErro(mensagemErroApi(e, 'Não foi possível carregar os ocupantes das unidades.'));
      });
    return () => {
      ativa = false;
    };
  }, [condominio.id]);

  const cpfDigitos = digitosDoCpf(cpf);
  const cpfCompleto = cpfDigitos.length === 11;

  // Com o CPF completo, procura se a pessoa já tem cadastro, depois que se para de digitar
  useEffect(() => {
    if (!cpfCompleto) return;
    let ativa = true;
    const espera = setTimeout(() => {
      usuarioService
        .pessoaPorCpf(cpfDigitos)
        .then((pessoa) => {
          if (ativa) setConsultaDoCpf({ cpf: cpfDigitos, pessoa });
        })
        .catch((e) => {
          if (!ativa) return;
          if (statusDoErro(e) !== 404) toast.error(mensagemErroApi(e, 'Não foi possível conferir o CPF.'));
          setConsultaDoCpf({ cpf: cpfDigitos, pessoa: null });
        });
    }, 350);
    return () => {
      ativa = false;
      clearTimeout(espera);
    };
  }, [cpfDigitos, cpfCompleto]);

  const morador = papel === 'MORADOR';
  const consultaAtual = cpfCompleto && consultaDoCpf?.cpf === cpfDigitos ? consultaDoCpf : null;
  const procurando = !morador && cpfCompleto && !consultaAtual;
  const pessoaExistente = consultaAtual?.pessoa ?? null;
  const pessoaNova = !morador && consultaAtual !== null && consultaAtual.pessoa === null;
  const criarSenha = pessoaNova && acaoSenha === 'CRIAR_SENHA';

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!morador && !consultaAtual) {
      setErro(cpfCompleto ? 'Aguarde a conferência do CPF.' : 'Informe o CPF completo.');
      return;
    }
    if (criarSenha && senha !== confirmacao) {
      setErro('A confirmação não é igual à senha.');
      return;
    }
    const pedido: NovoAcesso = morador
      ? { condominioId: condominio.id, papel: 'MORADOR', pessoaId: Number(pessoaId), acaoSenha: 'ENVIAR_LINK' }
      : pessoaExistente
        ? { condominioId: condominio.id, papel: papel as Papel, cpf: cpfDigitos, acaoSenha: 'ENVIAR_LINK' }
        : {
            condominioId: condominio.id,
            papel: papel as Papel,
            cpf: cpfDigitos,
            nome: nome.trim(),
            email: email.trim(),
            telefone: telefone.trim() || undefined,
            acaoSenha,
            senha: criarSenha ? senha : undefined,
          };
    setErro('');
    setSalvando(true);
    try {
      await usuarioService.cadastrar(pedido);
      toast.success(
        pedido.acaoSenha === 'ENVIAR_LINK'
          ? 'Acesso criado. A pessoa vai receber por e-mail o link para definir a senha.'
          : 'Acesso criado.',
      );
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível dar o acesso.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo="Dar acesso"
      subtitulo={condominio.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="dar-acesso" variante="primario" carregando={salvando} disabled={procurando}>
            Dar acesso
          </Botao>
        </>
      }
    >
      <form id="dar-acesso" onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        {erro && <CaixaDeErro className="sm:col-span-2">{erro}</CaixaDeErro>}

        <CampoDeSelecao rotulo="Papel" value={papel} onChange={(e) => setPapel(e.target.value)} obrigatorio className="sm:col-span-2">
          <option value="">Escolha o papel</option>
          {papeis.map((p) => (
            <option key={p.valor} value={p.valor}>
              {p.descricao}
            </option>
          ))}
        </CampoDeSelecao>

        {morador && (
          <>
            <CampoDeSelecao
              rotulo="Ocupante"
              value={pessoaId}
              onChange={(e) => setPessoaId(e.target.value)}
              obrigatorio
              disabled={!ocupantes}
              className="sm:col-span-2"
              ajuda={
                ocupantes && ocupantes.length === 0
                  ? 'Todos os ocupantes já têm acesso. Cadastre o morador em Ocupantes antes de dar o acesso.'
                  : 'Só aparecem os ocupantes das unidades que ainda não têm acesso.'
              }
            >
              <option value="">{ocupantes ? 'Escolha o ocupante' : 'Carregando…'}</option>
              {ocupantes?.map((o) => (
                <option key={o.pessoaCodigo} value={o.pessoaCodigo}>
                  {[o.nome, rotuloUnidade(o.unidadeNumero, o.unidadeBloco)].filter(Boolean).join(' · ')}
                </option>
              ))}
            </CampoDeSelecao>
            <p className="rounded-xl bg-info-fundo px-4 py-3 text-[13px] text-info sm:col-span-2">
              O morador recebe por e-mail o link para definir a senha. O link vale por 24 horas.
            </p>
          </>
        )}

        {papel && !morador && (
          <>
            <Campo
              rotulo="CPF"
              inputMode="numeric"
              autoComplete="off"
              value={cpf}
              onChange={(e) => setCpf(formatarCpf(e.target.value))}
              obrigatorio
              placeholder="000.000.000-00"
              ajuda={procurando ? 'Procurando cadastro…' : 'Se a pessoa já tiver cadastro, os dados dela aparecem aqui.'}
              className="sm:col-span-2"
            />

            {pessoaExistente && (
              <div className="flex flex-col gap-1 rounded-xl bg-info-fundo px-4 py-3 text-sm text-info sm:col-span-2">
                <span className="font-bold">{pessoaExistente.nome}</span>
                {pessoaExistente.email && <span>{pessoaExistente.email}</span>}
                <span className="text-[13px]">
                  Esta pessoa já tem cadastro. Ela recebe o acesso e, por e-mail, o link para definir a senha.
                </span>
              </div>
            )}

            {pessoaNova && (
              <>
                <Campo rotulo="Nome" value={nome} onChange={(e) => setNome(e.target.value)} obrigatorio maxLength={100} className="sm:col-span-2" />
                <Campo rotulo="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} obrigatorio maxLength={100} />
                <Campo rotulo="Telefone" type="tel" value={telefone} onChange={(e) => setTelefone(e.target.value)} maxLength={20} />
                <fieldset className="m-0 flex flex-col gap-2 border-0 p-0 sm:col-span-2">
                  <legend className="mb-1.5 text-[13px] font-semibold text-tinta-2">Senha</legend>
                  {acoesSenha.map((a) => (
                    <label key={a.valor} className="flex min-h-11 cursor-pointer items-center gap-2.5 text-sm">
                      <input
                        type="radio"
                        name="acao-senha"
                        value={a.valor}
                        checked={acaoSenha === a.valor}
                        onChange={() => setAcaoSenha(a.valor as AcaoSenha)}
                        className="size-4 accent-tinta"
                      />
                      {a.descricao}
                    </label>
                  ))}
                </fieldset>
                {criarSenha && (
                  <>
                    <Campo
                      rotulo="Senha"
                      type="password"
                      autoComplete="new-password"
                      value={senha}
                      onChange={(e) => setSenha(e.target.value)}
                      obrigatorio
                      minLength={6}
                      ajuda="Pelo menos 6 caracteres."
                    />
                    <Campo
                      rotulo="Confirme a senha"
                      type="password"
                      autoComplete="new-password"
                      value={confirmacao}
                      onChange={(e) => setConfirmacao(e.target.value)}
                      obrigatorio
                      minLength={6}
                    />
                  </>
                )}
              </>
            )}
          </>
        )}
      </form>
    </Modal>
  );
}
