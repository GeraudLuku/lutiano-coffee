import { salesApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, Pager } from '../ui';
import { SalesTable } from './SalesTable';

/** Sales of one location or one seller, newest first, a page at a time. */
export function PagedSales({ operationId, sellerId, variant }: { operationId?: string; sellerId?: string; variant: 'bySeller' | 'byLocation' }) {
  const { items, pager, error, loading, reload } = usePagedQuery((page, size) => salesApi.list({ operationId, sellerId, page, size }), [operationId, sellerId]);
  return (
    <>
      <ErrorNote error={error} retry={reload} />
      {!loading || items.length ? <SalesTable rows={items} variant={variant} /> : null}
      <Pager {...pager} />
    </>
  );
}
