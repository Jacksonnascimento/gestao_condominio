/*
 * CPF, CNPJ, telefone e CEP: as máscaras aplicadas enquanto a pessoa digita e a conferência dos dígitos do CPF e do
 * CNPJ antes de consultar a API.
 */

/** Só os dígitos do CPF/CNPJ, no máximo 14. */
export function digitosDoDocumento(texto?: string | null): string {
  return (texto ?? '').replace(/\D/g, '').slice(0, 14);
}

/** Só os dígitos do CPF, no máximo 11. */
export function digitosDoCpf(texto?: string | null): string {
  return (texto ?? '').replace(/\D/g, '').slice(0, 11);
}

/** Máscara conforme a pessoa digita: até 11 dígitos, CPF (000.000.000-00); a partir de 12, CNPJ (00.000.000/0000-00). */
export function formatarDocumento(texto?: string | null): string {
  const d = digitosDoDocumento(texto);
  if (d.length <= 11) {
    let s = d.slice(0, 3);
    if (d.length > 3) s += '.' + d.slice(3, 6);
    if (d.length > 6) s += '.' + d.slice(6, 9);
    if (d.length > 9) s += '-' + d.slice(9, 11);
    return s;
  }
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}${d.length > 12 ? '-' + d.slice(12, 14) : ''}`;
}

/** 12345678901 vira 123.456.789-01, formatando também enquanto a pessoa digita; nunca passa de 11 dígitos. */
export function formatarCpf(texto?: string | null): string {
  return formatarDocumento(digitosDoCpf(texto));
}

function digitoVerificador(numeros: string, pesos: number[]): number {
  const soma = pesos.reduce((total, peso, i) => total + Number(numeros[i]) * peso, 0);
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

/** Confere os dígitos verificadores de um CPF (só os 11 números). */
export function cpfValido(d: string): boolean {
  if (d.length !== 11 || /^(\d)\1+$/.test(d)) return false;
  const primeiro = digitoVerificador(d, [10, 9, 8, 7, 6, 5, 4, 3, 2]);
  const segundo = digitoVerificador(d, [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);
  return primeiro === Number(d[9]) && segundo === Number(d[10]);
}

/** Confere os dígitos verificadores de um CNPJ (só os 14 números). */
export function cnpjValido(d: string): boolean {
  if (d.length !== 14 || /^(\d)\1+$/.test(d)) return false;
  const primeiro = digitoVerificador(d, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const segundo = digitoVerificador(d, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  return primeiro === Number(d[12]) && segundo === Number(d[13]);
}

/**
 * Confere o CPF/CNPJ antes de consultar a API. Devolve a mensagem para a tela, ou vazio quando está certo. A API
 * confere de novo ao cadastrar a pessoa.
 */
export function problemaDoDocumento(digitos: string): string {
  if (digitos.length === 11) return cpfValido(digitos) ? '' : 'Este CPF não é válido. Confira os números.';
  if (digitos.length === 14) return cnpjValido(digitos) ? '' : 'Este CNPJ não é válido. Confira os números.';
  return 'Informe o CPF (11 números) ou o CNPJ (14 números).';
}

/** F para CPF, J para CNPJ. */
export function tipoDePessoa(digitos: string): 'F' | 'J' {
  return digitos.length === 14 ? 'J' : 'F';
}

/** 77999991234 vira (77) 99999-1234; com 10 dígitos, (77) 3421-1234. Formata enquanto a pessoa digita. */
export function formatarTelefone(texto?: string | null): string {
  const d = (texto ?? '').replace(/\D/g, '').slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : '';
  const ddd = d.slice(0, 2);
  const resto = d.slice(2);
  const corte = resto.length > 8 ? 5 : 4;
  return resto.length > corte ? `(${ddd}) ${resto.slice(0, corte)}-${resto.slice(corte)}` : `(${ddd}) ${resto}`;
}

/** 45000000 vira 45000-000, formatando também enquanto a pessoa digita. */
export function formatarCep(texto?: string | null): string {
  const numeros = (texto ?? '').replace(/\D/g, '').slice(0, 8);
  return numeros.length > 5 ? `${numeros.slice(0, 5)}-${numeros.slice(5)}` : numeros;
}
