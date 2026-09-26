'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';
import { Botao, Campo, CampoDeSelecao } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import {
  cadastroDeCondominiosService,
  formatarCep,
  type Condominio,
  type DadosDoCondominio,
} from '@/services/cadastroDeCondominiosService';
import type { Opcao } from '@/services/tipos';
import { mensagemErroApi } from '@/services/utilitarios';

const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
];

/** 45000000 vira 45000-000 enquanto se digita. */
function mascaraDeCep(texto: string): string {
  const numeros = texto.replace(/\D/g, '').slice(0, 8);
  return numeros.length > 5 ? `${numeros.slice(0, 5)}-${numeros.slice(5)}` : numeros;
}

/** Os campos como aparecem no formulário, todos em texto. */
type CamposDeTexto = Record<keyof DadosDoCondominio, string>;

function camposIniciais(condominio: Condominio | null, tipologiaPadrao: string): CamposDeTexto {
  return {
    nome: condominio?.nome ?? '',
    tipologia: condominio?.tipologia ?? tipologiaPadrao,
    numeroUnidades: condominio?.numeroUnidades != null ? String(condominio.numeroUnidades) : '',
    diaVencimentoTaxa: condominio?.diaVencimentoTaxa != null ? String(condominio.diaVencimentoTaxa) : '',
    cep: formatarCep(condominio?.cep),
    logradouro: condominio?.logradouro ?? '',
    numero: condominio?.numero ?? '',
    complemento: condominio?.complemento ?? '',
    bairro: condominio?.bairro ?? '',
    cidade: condominio?.cidade ?? '',
    estado: condominio?.estado ?? '',
    pais: condominio?.pais ?? (condominio ? '' : 'Brasil'),
    referencia: condominio?.referencia ?? '',
  };
}

/** Cadastro e edição do condomínio, para o administrador geral. */
export function FormularioDoCondominio({
  condominio,
  tipologias,
  aoFechar,
  aoSalvar,
}: {
  /** Nulo para cadastrar um novo. */
  condominio: Condominio | null;
  tipologias: Opcao[];
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const [campos, setCampos] = useState<CamposDeTexto>(() => camposIniciais(condominio, tipologias[0]?.valor ?? ''));
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  const mudar = (campo: keyof CamposDeTexto) => (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setCampos((atual) => ({ ...atual, [campo]: campo === 'cep' ? mascaraDeCep(evento.target.value) : evento.target.value }));

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    // Texto vazio apaga o campo na edição; nulo o manteria
    const dados: DadosDoCondominio = {
      nome: campos.nome.trim(),
      tipologia: campos.tipologia,
      numeroUnidades: campos.numeroUnidades ? Number(campos.numeroUnidades) : null,
      diaVencimentoTaxa: campos.diaVencimentoTaxa ? Number(campos.diaVencimentoTaxa) : null,
      cep: campos.cep.trim(),
      logradouro: campos.logradouro.trim(),
      numero: campos.numero.trim(),
      complemento: campos.complemento.trim(),
      bairro: campos.bairro.trim(),
      cidade: campos.cidade.trim(),
      estado: campos.estado,
      pais: campos.pais.trim(),
      referencia: campos.referencia.trim(),
    };
    setErro('');
    setSalvando(true);
    try {
      if (condominio) {
        await cadastroDeCondominiosService.atualizar(condominio.id, dados);
        toast.success('Condomínio atualizado.');
      } else {
        await cadastroDeCondominiosService.cadastrar(dados);
        toast.success('Condomínio cadastrado.');
      }
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível salvar o condomínio.'));
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo={condominio ? 'Editar condomínio' : 'Novo condomínio'}
      subtitulo={condominio?.nome}
      largura="lg"
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="formulario-condominio" variante="primario" carregando={salvando}>
            {condominio ? 'Salvar' : 'Cadastrar'}
          </Botao>
        </>
      }
    >
      <form id="formulario-condominio" onSubmit={salvar} className="grid gap-4 sm:grid-cols-6">
        {erro && (
          <p className="rounded-xl bg-perigo-fundo px-4 py-3 text-sm text-perigo sm:col-span-6" role="alert">
            {erro}
          </p>
        )}
        <Campo rotulo="Nome" value={campos.nome} onChange={mudar('nome')} obrigatorio maxLength={100} className="sm:col-span-4" />
        <CampoDeSelecao rotulo="Tipo" value={campos.tipologia} onChange={mudar('tipologia')} obrigatorio className="sm:col-span-2">
          {tipologias.map((t) => (
            <option key={t.valor} value={t.valor}>
              {t.descricao}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo
          rotulo="Número de unidades"
          type="number"
          inputMode="numeric"
          min={0}
          value={campos.numeroUnidades}
          onChange={mudar('numeroUnidades')}
          className="sm:col-span-3"
        />
        <Campo
          rotulo="Dia de vencimento da taxa"
          type="number"
          inputMode="numeric"
          min={1}
          max={31}
          value={campos.diaVencimentoTaxa}
          onChange={mudar('diaVencimentoTaxa')}
          ajuda="De 1 a 31."
          className="sm:col-span-3"
        />

        <h3 className="mt-2 text-sm font-extrabold sm:col-span-6">Endereço</h3>
        <Campo rotulo="CEP" inputMode="numeric" value={campos.cep} onChange={mudar('cep')} placeholder="00000-000" className="sm:col-span-2" />
        <Campo rotulo="Logradouro" value={campos.logradouro} onChange={mudar('logradouro')} maxLength={100} placeholder="Rua, avenida…" className="sm:col-span-4" />
        <Campo rotulo="Número" value={campos.numero} onChange={mudar('numero')} maxLength={10} className="sm:col-span-2" />
        <Campo rotulo="Complemento" value={campos.complemento} onChange={mudar('complemento')} maxLength={50} className="sm:col-span-4" />
        <Campo rotulo="Bairro" value={campos.bairro} onChange={mudar('bairro')} maxLength={50} className="sm:col-span-3" />
        <Campo rotulo="Cidade" value={campos.cidade} onChange={mudar('cidade')} maxLength={50} className="sm:col-span-3" />
        <CampoDeSelecao rotulo="UF" value={campos.estado} onChange={mudar('estado')} className="sm:col-span-2">
          <option value="">—</option>
          {UFS.map((uf) => (
            <option key={uf} value={uf}>
              {uf}
            </option>
          ))}
        </CampoDeSelecao>
        <Campo rotulo="País" value={campos.pais} onChange={mudar('pais')} maxLength={50} className="sm:col-span-4" />
        <Campo
          rotulo="Ponto de referência"
          value={campos.referencia}
          onChange={mudar('referencia')}
          maxLength={100}
          className="sm:col-span-6"
        />
      </form>
    </Modal>
  );
}
