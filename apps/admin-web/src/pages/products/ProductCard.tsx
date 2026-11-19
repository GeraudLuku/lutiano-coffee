import { Pill } from '../../components/ui';
import { ProductImage, StatusPill } from '../../components/domain';
import { num } from '../../utils/format';
import { centralUnits, priceLabel, variantsLabel } from '../../utils/catalog';
import type { Product } from '../../types/models';

/** Catalogue card (design: Product Catalog board): category, status, "3 variants · From 1,500 XAF". */
export function ProductCard({ product: p, onOpen }: { product: Product; onOpen: () => void }) {
  const stockText = p.track_stock || p.kind === 'PARENT' ? `${num(centralUnits(p))} in central stock` : 'Stock not tracked';
  return (
    <button className="product-card" onClick={onOpen}>
      <ProductImage url={p.image_url} />
      <div className="body">
        <b>{p.name}</b>
        <div className="row"><Pill tone="purple">{p.category}</Pill><StatusPill active={p.active} /></div>
        <small>{[variantsLabel(p), p.sku].filter(Boolean).join(' · ')}</small>
        <small>{stockText}</small>
        <strong>{priceLabel(p)}</strong>
      </div>
    </button>
  );
}
