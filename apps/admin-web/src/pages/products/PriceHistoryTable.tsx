import { productsApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, Empty, ErrorNote, Pager } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import type { PriceChange } from '../../types/models';
import { Delta } from '../inventory/InventoryBadges';

/** Dated price changes for a product (and, for a parent, all its variants), a page at a time. */
export function PriceHistoryTable({ productId, showVariant }: { productId: string; showVariant: boolean }) {
  const { items, pager, error, loading, reload } = usePagedQuery((page, size) => productsApi.priceHistory(productId, { page, size }), [productId]);
  if (!loading && !items.length && !error) return <Empty title="No price changes recorded" />;
  return (
    <>
    <ErrorNote error={error} retry={reload} />
    <DataTable rows={items} rowKey={h => h.id} columns={[
      { header: 'Effective from', cell: h => dateTime(h.changed_at) },
      ...(showVariant ? [{ header: 'Variant', cell: (h: PriceChange) => h.variant_name ?? h.product_name }] : []),
      { header: 'Previous', align: 'right', cell: h => (h.previous_price_xaf == null ? 'Initial price' : xaf(h.previous_price_xaf)) },
      { header: 'New price', align: 'right', cell: h => <b>{xaf(h.price_xaf)}</b> },
      { header: 'Change', align: 'right', cell: h => (h.previous_price_xaf == null ? '—' : <Delta value={h.price_xaf - h.previous_price_xaf} />) },
      { header: 'By', cell: h => h.changed_by_name ?? 'System' },
    ]} />
    <Pager {...pager} />
    </>
  );
}
