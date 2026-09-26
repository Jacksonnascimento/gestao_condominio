import api from '@/services/api';

/** O que o formulário de ocupante preenche quando o CPF/CNPJ já tem cadastro. */
export interface PessoaResumo {
  id: number;
  nome: string;
  /** F (física) ou J (jurídica). */
  tipo: string | null;
  email: string | null;
  telefone: string | null;
}

export const pessoaService = {
  /**
   * Procura a pessoa pelo CPF/CNPJ (com ou sem pontuação). Só para quem gerencia ocupantes. Sem cadastro, a API
   * responde 404, e o formulário segue pedindo os dados da pessoa nova. O documento vai no corpo do pedido, e não
   * no endereço, para não ficar gravado em registros de acesso.
   */
  consultarPorDocumento: (cpfCnpj: string) =>
    api.post<PessoaResumo>('/pessoas/consulta-por-documento', { cpfCnpj }).then((r) => r.data),
};
