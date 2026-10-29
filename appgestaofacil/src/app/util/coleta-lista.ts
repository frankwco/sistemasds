/**
 * Helpers para as telas que listam coletas (aba Coletas e Pesquisar Coletas): calculam
 * uma vez, ao montar a lista, o que o template antes refazia a cada detecção de mudanças.
 */

/** Grupos de coleta na ordem em que aparecem; `texto` é a cor da letra sobre o fundo `cor`. */
export const GRUPOS_COLETA = [
  { campo: 'a', nome: 'A', cor: '#f3f3f3', texto: '#000' },
  { campo: 'e', nome: 'E', cor: '#fff766', texto: '#000' },
  { campo: 'b', nome: 'B', cor: '#868083', texto: '#fff' },
  { campo: 'a2', nome: 'A2', cor: '#fc877e', texto: '#000' },
  { campo: 'a3', nome: 'A3', cor: '#f75045', texto: '#fff' },
  { campo: 'a4', nome: 'A4', cor: '#970e04', texto: '#fff' },
];

export interface GrupoColeta {
  nome: string;
  valor: string;
  cor: string;
  texto: string;
}

/** Mesmo formato do pipe number '1.3-3' em pt-BR. */
export function formatarPeso(valor: any): string {
  const numero = typeof valor === 'number' ? valor : parseFloat(String(valor).replace(',', '.'));
  if (isNaN(numero)) {
    return '';
  }
  return numero.toLocaleString('pt-BR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

/** Grupos que o ponto coleta, já com o valor formatado. */
export function gruposDaColeta(coleta: any): GrupoColeta[] {
  const gruposPonto: string = (coleta.pontoColetaSelecionado && coleta.pontoColetaSelecionado.gruposColetados) || '';
  return GRUPOS_COLETA
    .filter(g => gruposPonto.includes(g.nome + ';'))
    .map(g => ({ nome: g.nome, valor: formatarPeso(coleta[g.campo]), cor: g.cor, texto: g.texto }));
}

/** Instante em que a coleta foi finalizada (ms): campos dia/mês/ano/hora do app, ou dataUltimaColeta. */
export function instanteDaColeta(coleta: any): number {
  if (coleta.anoUltimaColeta) {
    return new Date(coleta.anoUltimaColeta, (coleta.mesUltimaColeta || 1) - 1, coleta.diaUltimaColeta || 1,
      coleta.horaUltimaColeta || 0, coleta.minutoUltimaColeta || 0, coleta.segundoUltimaColeta || 0).getTime();
  }
  const tempo = new Date(coleta.dataUltimaColeta).getTime();
  return isNaN(tempo) ? 0 : tempo;
}
