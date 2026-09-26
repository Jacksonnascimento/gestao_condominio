/** Só os números do CPF. */
export function digitosDoCpf(texto?: string | null): string {
  return (texto ?? '').replace(/\D/g, '').slice(0, 11);
}

/** 12345678901 vira 123.456.789-01, formatando também enquanto a pessoa digita. */
export function formatarCpf(texto?: string | null): string {
  const d = digitosDoCpf(texto);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}
