import { auditApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, Pager } from '../ui';
import { ActivityList } from './ActivityList';

/** The audit log for one record, one person or everything, a page at a time (newest first). */
export function PagedActivity({ entityType, entityId, actorId, showModule, empty }: {
  entityType?: string; entityId?: string; actorId?: string; showModule?: boolean; empty?: string;
}) {
  const { items, pager, error, loading, reload } = usePagedQuery((page, size) => auditApi.list({ entityType, entityId, actorId, page, size }), [entityType, entityId, actorId]);
  return (
    <>
      <ErrorNote error={error} retry={reload} />
      {!loading || items.length ? <ActivityList rows={items} showModule={showModule} empty={empty} /> : null}
      <Pager {...pager} />
    </>
  );
}
