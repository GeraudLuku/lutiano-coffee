export type StockTone = 'green' | 'amber' | 'red';

/** Stock status shared by every stock table: out, low (at or below threshold) or healthy. */
export function stockStatus(quantity: number, threshold: number): [StockTone, string] {
  if (quantity <= 0) return ['red', 'Out of stock'];
  if (quantity <= threshold) return ['amber', 'Low stock'];
  return ['green', 'Healthy'];
}
/** Scale for level bars: three times the threshold, or the quantity if larger. */
export const stockScale = (quantity: number, threshold: number) => Math.max(threshold * 3, quantity, 1);
