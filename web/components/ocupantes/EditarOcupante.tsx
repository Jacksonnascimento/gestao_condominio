'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { CamposDoVinculo, type DadosDoVinculo } from '@/components/ocupantes/CamposDoVinculo';
import { Botao, CaixaDeErro, Campo } from '@/components/Interface';
import { Modal } from '@/components/Modal';
import { formatarDocumento } from '@/services/documentos';
import { ocupanteService, type OpcoesOcupante, type Ocupante } from '@/services/ocupanteService';
import { mensagemErroApi, rotuloUnidade } from '@/services/utilitarios';

/**
 * Edição do vínculo e dos dados de contato. O CPF/CNPJ e a unidade não mudam: para trocar de unidade, remova o
 * ocupante e cadastre de novo.
 */
export function EditarOcupante({
  ocupante,
  opcoes,
  aoFechar,
  aoSalvar,
}: {
  ocupante: Ocupante;
  opcoes: OpcoesOcupante;
  aoFechar: () => void;
  aoSalvar: () => void;
}) {
  const [documento, setDocumento] = useState(ocupante.cpfCnpj);
  const [contato, setContato] = useState({
    nome: ocupante.nome ?? '',
    email: ocupante.email ?? '',
    telefone: ocupante.telefone ?? '',
  });
  const [vinculo, setVinculo] = useState<DadosDoVinculo>({
    vinculo: ocupante.vinculo ?? '',
    inicioOcupacao: ocupante.inicioOcupacao ?? '',
    fimOcupacao: ocupante.fimOcupacao ?? '',
    periodoUso: ocupante.periodoUso ?? '',
    tipoPeriodo: ocupante.tipoPeriodo ?? '',
  });
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  // As listas não trazem o CPF/CNPJ; a busca pelo ocupante traz, para quem gerencia o condomínio
  useEffect(() => {
    if (ocupante.cpfCnpj) return;
    let ativa = true;
    ocupanteService
      .buscar(ocupante.id)
      .then((completo) => {
        if (ativa) setDocumento(completo.cpfCnpj);
      })
      .catch(() => {
        // Sem o documento a edição continua possível; ele só aparece como informação
      });
    return () => {
      ativa = false;
    };
  }, [ocupante.id, ocupante.cpfCnpj]);

  const mudar = (campo: keyof typeof contato) => (evento: React.ChangeEvent<HTMLInputElement>) =>
    setContato((atual) => ({ ...atual, [campo]: evento.target.value }));

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro('');
    setSalvando(true);
    const multiproprietario = vinculo.vinculo === 'MULTIPROPRIETARIO';
    try {
      await ocupanteService.atualizar(ocupante.id, {
        nome: contato.nome.trim(),
        email: contato.email.trim(),
        telefone: contato.telefone.trim() || null,
        vinculo: vinculo.vinculo,
        inicioOcupacao: vinculo.inicioOcupacao,
        fimOcupacao: vinculo.fimOcupacao || null,
        periodoUso: multiproprietario ? vinculo.periodoUso.trim() || null : null,
        tipoPeriodo: multiproprietario ? vinculo.tipoPeriodo || null : null,
      });
      toast.success('Ocupante atualizado.');
      aoSalvar();
    } catch (e) {
      setErro(mensagemErroApi(e, 'Não foi possível salvar o ocupante.'));
      setSalvando(false);
    }
  }

  const unidade = rotuloUnidade(ocupante.unidadeNumero, ocupante.unidadeBloco);

  return (
    <Modal
      titulo="Editar ocupante"
      subtitulo={[ocupante.nome, unidade && `Unidade ${unidade}`].filter(Boolean).join(' · ')}
      aoFechar={aoFechar}
      ocupado={salvando}
      rodape={
        <>
          <Botao variante="texto" onClick={aoFechar} disabled={salvando}>
            Cancelar
          </Botao>
          <Botao type="submit" form="editar-ocupante" variante="primario" carregando={salvando}>
            Salvar
          </Botao>
        </>
      }
    >
      <form id="editar-ocupante" onSubmit={salvar} className="flex flex-col gap-6">
        {erro && <CaixaDeErro>{erro}</CaixaDeErro>}

        <fieldset className="m-0 border-0 p-0">
          <legend className="mb-3 text-xs font-bold tracking-[0.04em] text-apagado uppercase">Pessoa</legend>
          <div className="grid gap-4 sm:grid-cols-2">
            {documento && (
              <p className="text-sm text-tinta-2 sm:col-span-2">
                {documento.length > 11 ? 'CNPJ' : 'CPF'} <span className="font-semibold text-tinta tabular-nums">{formatarDocumento(documento)}</span>
              </p>
            )}
            <Campo rotulo="Nome" value={contato.nome} onChange={mudar('nome')} obrigatorio maxLength={100} className="sm:col-span-2" />
            <Campo
              rotulo="E-mail"
              type="email"
              value={contato.email}
              onChange={mudar('email')}
              obrigatorio
              maxLength={100}
              ajuda="Se a pessoa entra no sistema, é também o e-mail de acesso dela."
            />
            <Campo rotulo="Telefone" type="tel" value={contato.telefone} onChange={mudar('telefone')} maxLength={20} />
            <p className="text-xs text-apagado sm:col-span-2">
              Nome, e-mail e telefone são da pessoa: a mudança vale em todas as unidades em que ela está.
            </p>
          </div>
        </fieldset>

        <fieldset className="m-0 border-0 p-0">
          <legend className="mb-3 text-xs font-bold tracking-[0.04em] text-apagado uppercase">Vínculo com a unidade</legend>
          <CamposDoVinculo opcoes={opcoes} dados={vinculo} aoMudar={setVinculo} />
        </fieldset>
      </form>
    </Modal>
  );
}
