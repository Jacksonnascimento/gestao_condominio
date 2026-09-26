'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { CamposDoVinculo, type DadosDoVinculo } from '@/components/ocupantes/CamposDoVinculo';
import { formatarDocumento, problemaDoDocumento, somenteDigitos, tipoDePessoa } from '@/components/ocupantes/documento';
import { Botao, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { ErroDoFormulario } from '@/components/unidades/PecasDeCadastro';
import { useSessao } from '@/context/SessaoContext';
import { condominioService, type UnidadeResumo } from '@/services/condominioService';
import { ocupanteService, type OpcoesOcupante } from '@/services/ocupanteService';
import { pessoaService, type PessoaResumo } from '@/services/pessoaService';
import { hojeParaCampo, mensagemErroApi, rotuloUnidade, statusDoErro } from '@/services/utilitarios';

type Consulta =
  | { situacao: 'aguardando' }
  | { situacao: 'consultando' }
  | { situacao: 'encontrada'; pessoa: PessoaResumo }
  | { situacao: 'nova' };

const CLASSE_DA_LEGENDA = 'mb-3 text-xs font-bold tracking-[0.04em] text-apagado uppercase';

/**
 * Vincula uma pessoa a uma unidade. O caminho é: escolher a unidade, informar o CPF/CNPJ e, se a pessoa já tem
 * cadastro, aproveitar o dela; se não tem, preencher nome, e-mail e telefone. Por fim, o vínculo e o período.
 */
export function CadastrarOcupante({
  opcoes,
  unidadeFixa,
  aoFechar,
  aoCadastrar,
}: {
  opcoes: OpcoesOcupante;
  /** Quando a janela é aberta a partir de uma unidade, ela já vem escolhida. */
  unidadeFixa?: { id: number; rotulo: string };
  aoFechar: () => void;
  aoCadastrar: () => void;
}) {
  const { condominio } = useSessao();
  const [unidades, setUnidades] = useState<UnidadeResumo[] | null>(null);
  const [unidadeId, setUnidadeId] = useState(unidadeFixa ? String(unidadeFixa.id) : '');
  const [documento, setDocumento] = useState('');
  const [erroDoDocumento, setErroDoDocumento] = useState('');
  const [consulta, setConsulta] = useState<Consulta>({ situacao: 'aguardando' });
  const [pessoaNova, setPessoaNova] = useState({ nome: '', email: '', telefone: '' });
  const [vinculo, setVinculo] = useState<DadosDoVinculo>({
    vinculo: opcoes.vinculos[0]?.valor ?? '',
    inicioOcupacao: hojeParaCampo(),
    fimOcupacao: '',
    periodoUso: '',
    tipoPeriodo: '',
  });
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  useEffect(() => {
    if (unidadeFixa || !condominio) return;
    let ativa = true;
    condominioService
      .unidades(condominio.id)
      .then((lista) => {
        if (!ativa) return;
        setUnidades(
          [...lista].sort((a, b) =>
            rotuloUnidade(a.numero, a.bloco).localeCompare(rotuloUnidade(b.numero, b.bloco), 'pt-BR', { numeric: true }),
          ),
        );
      })
      .catch((e) => {
        if (!ativa) return;
        setUnidades([]);
        setErro(mensagemErroApi(e, 'Não foi possível carregar as unidades.'));
      });
    return () => {
      ativa = false;
    };
  }, [unidadeFixa, condominio]);

  const digitos = somenteDigitos(documento);
  const consultada = consulta.situacao === 'encontrada' || consulta.situacao === 'nova';

  function mudarDocumento(texto: string) {
    setDocumento(formatarDocumento(texto));
    setErroDoDocumento('');
    setConsulta({ situacao: 'aguardando' });
  }

  async function consultar() {
    const problema = problemaDoDocumento(digitos);
    if (problema) {
      setErroDoDocumento(problema);
      return;
    }
    setErroDoDocumento('');
    setConsulta({ situacao: 'consultando' });
    try {
      const pessoa = await pessoaService.consultarPorDocumento(digitos);
      setConsulta({ situacao: 'encontrada', pessoa });
    } catch (e) {
      if (statusDoErro(e) === 404) {
        setConsulta({ situacao: 'nova' });
      } else {
        setConsulta({ situacao: 'aguardando' });
        setErroDoDocumento(mensagemErroApi(e, 'Não foi possível consultar o documento.'));
      }
    }
  }

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    // Enquanto a pessoa não foi consultada, o Enter e o botão principal fazem a consulta
    if (!consultada) {
      await consultar();
      return;
    }
    setErro('');
    setSalvando(true);
    const multiproprietario = vinculo.vinculo === 'MULTIPROPRIETARIO';
    try {
      await ocupanteService.cadastrar({
        cpfCnpj: digitos,
        tipoPessoa: tipoDePessoa(digitos),
        ...(consulta.situacao === 'nova'
          ? { nome: pessoaNova.nome.trim(), email: pessoaNova.email.trim(), telefone: pessoaNova.telefone.trim() || undefined }
          : {}),
        unidadeId: Number(unidadeId),
        vinculo: vinculo.vinculo,
        inicioOcupacao: vinculo.inicioOcupacao,
        fimOcupacao: vinculo.fimOcupacao || null,
        periodoUso: multiproprietario ? vinculo.periodoUso.trim() || null : null,
        tipoPeriodo: multiproprietario ? vinculo.tipoPeriodo || null : null,
      });
      toast.success('Ocupante cadastrado.');
      aoCadastrar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível cadastrar o ocupante.'));
      setSalvando(false);
    }
  }

  const mudarPessoa = (campo: keyof typeof pessoaNova) => (evento: React.ChangeEvent<HTMLInputElement>) =>
    setPessoaNova((atual) => ({ ...atual, [campo]: evento.target.value }));

  const eCnpj = digitos.length > 11;

  return (
    <Modal
      titulo="Cadastrar ocupante"
      subtitulo={unidadeFixa ? `Unidade ${unidadeFixa.rotulo}` : condominio?.nome}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao
            type="submit"
            form="cadastrar-ocupante"
            variante="primario"
            carregando={salvando || consulta.situacao === 'consultando'}
          >
            {consultada ? 'Cadastrar ocupante' : 'Continuar'}
          </Botao>
        </>
      }
    >
      <form id="cadastrar-ocupante" onSubmit={salvar} className="flex flex-col gap-6">
        {erro && <ErroDoFormulario>{erro}</ErroDoFormulario>}

        {!unidadeFixa && (
          <CampoDeSelecao rotulo="Unidade" value={unidadeId} onChange={(e) => setUnidadeId(e.target.value)} obrigatorio disabled={!unidades}>
            <option value="">{unidades ? 'Escolha a unidade' : 'Carregando…'}</option>
            {unidades?.map((u) => (
              <option key={u.id} value={u.id}>
                {rotuloUnidade(u.numero, u.bloco)}
              </option>
            ))}
          </CampoDeSelecao>
        )}

        <fieldset className="m-0 border-0 p-0">
          <legend className={CLASSE_DA_LEGENDA}>Pessoa</legend>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
              <Campo
                rotulo="CPF ou CNPJ"
                value={documento}
                onChange={(e) => mudarDocumento(e.target.value)}
                obrigatorio
                inputMode="numeric"
                autoComplete="off"
                placeholder="000.000.000-00"
                aria-invalid={!!erroDoDocumento}
                className="grow"
              />
              {!consultada && (
                <Botao variante="secundario" onClick={consultar} carregando={consulta.situacao === 'consultando'}>
                  Buscar cadastro
                </Botao>
              )}
            </div>
            {erroDoDocumento && <ErroDoFormulario>{erroDoDocumento}</ErroDoFormulario>}
            {!consultada && !erroDoDocumento && (
              <p className="text-xs text-apagado">
                Informe o documento para ver se a pessoa já tem cadastro no sistema. Se tiver, os dados dela são aproveitados.
              </p>
            )}

            {consulta.situacao === 'encontrada' && (
              <div className="flex flex-col gap-1 rounded-xl bg-cabecalho p-4 text-sm">
                <span className="text-xs font-bold text-apagado">Pessoa já cadastrada</span>
                <span className="font-extrabold">{consulta.pessoa.nome}</span>
                {consulta.pessoa.email && <span className="text-tinta-2">{consulta.pessoa.email}</span>}
                {consulta.pessoa.telefone && <span className="text-tinta-2">{consulta.pessoa.telefone}</span>}
                <span className="mt-1 text-xs text-apagado">Os dados de contato ficam como estão. Para mudá-los, edite o ocupante depois.</span>
              </div>
            )}

            {consulta.situacao === 'nova' && (
              <div className="grid gap-4 sm:grid-cols-2">
                <p className="text-sm text-tinta-2 sm:col-span-2">
                  Nenhuma pessoa cadastrada com este {eCnpj ? 'CNPJ' : 'CPF'}. Preencha os dados para cadastrá-la.
                </p>
                <Campo
                  rotulo={eCnpj ? 'Razão social' : 'Nome completo'}
                  value={pessoaNova.nome}
                  onChange={mudarPessoa('nome')}
                  obrigatorio
                  maxLength={100}
                  autoComplete="off"
                  className="sm:col-span-2"
                />
                <Campo rotulo="E-mail" type="email" value={pessoaNova.email} onChange={mudarPessoa('email')} obrigatorio maxLength={100} autoComplete="off" />
                <Campo rotulo="Telefone" type="tel" value={pessoaNova.telefone} onChange={mudarPessoa('telefone')} maxLength={20} autoComplete="off" placeholder="(77) 99999-0000" />
              </div>
            )}
          </div>
        </fieldset>

        {consultada && (
          <fieldset className="m-0 border-0 p-0">
            <legend className={CLASSE_DA_LEGENDA}>Vínculo com a unidade</legend>
            <CamposDoVinculo opcoes={opcoes} dados={vinculo} aoMudar={setVinculo} />
          </fieldset>
        )}
      </form>
    </Modal>
  );
}
