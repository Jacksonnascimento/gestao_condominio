// Gera um PDF simples de uma ou mais páginas A4, com cabeçalho do condomínio, título e parágrafos.
// Usa Helvetica com WinAnsiEncoding, que cobre os acentos do português.

const LARGURA = 595, ALTURA = 842, MARGEM = 60;

function winAnsi(texto) {
  const bytes = [];
  for (const ch of texto) {
    const c = ch.codePointAt(0);
    if (c < 128) bytes.push(c);
    else if (c >= 160 && c <= 255) bytes.push(c);
    else if (c === 0x2013) bytes.push(0x96);
    else if (c === 0x2014) bytes.push(0x97);
    else if (c === 0x201c) bytes.push(0x93);
    else if (c === 0x201d) bytes.push(0x94);
    else if (c === 0x2019) bytes.push(0x92);
    else if (c === 0x2022) bytes.push(0x95);
    else if (c === 0xba || c === 0xaa) bytes.push(c);
    else bytes.push(0x3f);
  }
  return bytes;
}

function literal(texto) {
  let s = '';
  for (const b of winAnsi(texto)) {
    if (b === 0x28 || b === 0x29 || b === 0x5c) s += '\\' + String.fromCharCode(b);
    else if (b < 32 || b > 126) s += '\\' + b.toString(8).padStart(3, '0');
    else s += String.fromCharCode(b);
  }
  return `(${s})`;
}

/** quebra o texto em linhas de até `max` caracteres (aproximação para Helvetica) */
function quebrar(texto, max) {
  const linhas = [];
  for (const paragrafo of String(texto).split('\n')) {
    let atual = '';
    for (const palavra of paragrafo.split(/\s+/)) {
      if ((atual + ' ' + palavra).trim().length > max) { if (atual) linhas.push(atual); atual = palavra; }
      else atual = (atual + ' ' + palavra).trim();
    }
    linhas.push(atual);
  }
  return linhas;
}

/**
 * @param {{cabecalho: string, subcabecalho?: string, titulo: string, linhas: string[], rodape?: string}} doc
 * @returns {Buffer}
 */
export function gerarPdf({ cabecalho, subcabecalho, titulo, linhas, rodape }) {
  // monta a sequência de "desenhos" de cada página
  const paginas = [];
  let ops = [];
  let y = ALTURA - MARGEM;
  const novaPagina = (primeira) => {
    if (ops.length) paginas.push(ops);
    ops = [];
    y = ALTURA - MARGEM;
    ops.push(`BT /F2 13 Tf ${MARGEM} ${y} Td ${literal(cabecalho)} Tj ET`);
    y -= 16;
    if (subcabecalho) { ops.push(`BT /F1 9 Tf ${MARGEM} ${y} Td ${literal(subcabecalho)} Tj ET`); y -= 10; }
    ops.push(`0.6 w ${MARGEM} ${y} m ${LARGURA - MARGEM} ${y} l S`);
    y -= 30;
    if (primeira) {
      for (const l of quebrar(titulo, 62)) { ops.push(`BT /F2 14 Tf ${MARGEM} ${y} Td ${literal(l)} Tj ET`); y -= 19; }
      y -= 14;
    }
  };
  novaPagina(true);
  for (const paragrafo of linhas) {
    for (const l of quebrar(paragrafo, 95)) {
      if (y < MARGEM + 40) novaPagina(false);
      ops.push(`BT /F1 10.5 Tf ${MARGEM} ${y} Td ${literal(l)} Tj ET`);
      y -= 15;
    }
    y -= 8;
  }
  paginas.push(ops);

  // objetos: 1 catálogo, 2 páginas, 3 F1, 4 F2, depois página+conteúdo
  const objetos = [];
  const add = (conteudo) => { objetos.push(conteudo); return objetos.length; };
  add('<< /Type /Catalog /Pages 2 0 R >>');
  add(null); // páginas, preenchido depois
  add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>');
  add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>');
  const kids = [];
  paginas.forEach((p, i) => {
    const rodapeOps = rodape ? [`BT /F1 8 Tf ${MARGEM} 30 Td ${literal(rodape)} Tj ET`] : [];
    rodapeOps.push(`BT /F1 8 Tf ${LARGURA - MARGEM - 60} 30 Td ${literal(`Página ${i + 1} de ${paginas.length}`)} Tj ET`);
    const stream = [...p, ...rodapeOps].join('\n');
    const bytes = Buffer.from(stream, 'latin1');
    const c = add(`<< /Length ${bytes.length} >>\nstream\n${stream}\nendstream`);
    const pg = add(`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${LARGURA} ${ALTURA}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${c} 0 R >>`);
    kids.push(`${pg} 0 R`);
  });
  objetos[1] = `<< /Type /Pages /Kids [${kids.join(' ')}] /Count ${kids.length} >>`;

  let saida = '%PDF-1.4\n%\xe2\xe3\xcf\xd3\n';
  const offsets = [];
  objetos.forEach((o, i) => {
    offsets.push(Buffer.byteLength(saida, 'latin1'));
    saida += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = Buffer.byteLength(saida, 'latin1');
  saida += `xref\n0 ${objetos.length + 1}\n0000000000 65535 f \n`;
  for (const off of offsets) saida += `${String(off).padStart(10, '0')} 00000 n \n`;
  saida += `trailer\n<< /Size ${objetos.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(saida, 'latin1');
}
