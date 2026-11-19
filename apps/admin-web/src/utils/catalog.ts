import { xaf } from './format';
import type { Product } from '../types/models';

/** "From 1,800 XAF" for a parent, otherwise its own price. */
export function priceLabel(p: Product): string {
  if (p.kind === 'PARENT') return p.min_variant_price == null ? 'No active variants' : `From ${xaf(p.min_variant_price)}`;
  return xaf(p.price_xaf);
}

/** "3 variants" for a parent, otherwise the unit, e.g. "cup". */
export function variantsLabel(p: Product): string {
  if (p.kind === 'PARENT') return `${p.variant_count} ${p.variant_count === 1 ? 'variant' : 'variants'}`;
  return p.unit ?? '';
}

/** Units in central stock: the sum over variants for a parent. */
export const centralUnits = (p: Product) => (p.kind === 'PARENT' ? p.variants_central_quantity : p.central_quantity);
