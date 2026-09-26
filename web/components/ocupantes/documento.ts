/** Só os dígitos do CPF/CNPJ, no máximo 14. */
export function somenteDigitos(texto: string): string {
  return texto.replace(/\D/g, '').slice(0, 14);
}

/** Máscara conforme a pessoa digita: até 11 dígitos, CPF (000.000.000-00); a partir de 12, CNPJ (00.000.000/0000-00). */
export function formatarDocumento(texto?: string | null): string {
  const d = somenteDigitos(texto ?? '');
  if (d.length <= 11) {
    let s = d.slice(0, 3);
    if (d.length > 3) s += '.' + d.slice(3, 6);
    if (d.length > 6) s += '.' + d.slice(6, 9);
    if (d.length > 9) s += '-' + d.slice(9, 11);
    return s;
  }
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}${d.length > 12 ? '-' + d.slice(12, 14) : ''}`;
}

function digitoVerificador(numeros: string, pesos: number[]): number {
  const soma = pesos.reduce((total, peso, i) => total + Number(numeros[i]) * peso, 0);
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

function cpfValido(d: string): boolean {
  if (d.length !== 11 || /^(\d)\1+$/.test(d)) return false;
  const primeiro = digitoVerificador(d, [10, 9, 8, 7, 6, 5, 4, 3, 2]);
  const segundo = digitoVerificador(d, [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);
  return primeiro === Number(d[9]) && segundo === Number(d[10]);
}

function cnpjValido(d: string): boolean {
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
