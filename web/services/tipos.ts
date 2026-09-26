/** Página de uma listagem, como a API devolve. A primeira página é a 0. */
export interface Pagina<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

/** Item de uma lista de escolha: o valor vai nos pedidos, a descrição aparece na tela. */
export interface Opcao {
  valor: string;
  descricao: string;
}
