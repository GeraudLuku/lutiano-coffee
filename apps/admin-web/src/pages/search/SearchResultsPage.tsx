import { useSearchParams } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { searchApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, Empty, ErrorNote, Loading, Pager, Panel, PageHeader, TitleCell } from '../../components/ui';
import { Highlighted, searchLabel, searchTarget } from '../../utils/search';
import { num } from '../../utils/format';
import type { SearchHit } from '../../types/models';

/** Full search results, most relevant first, with a filter per type. Counts are exact up to the server's cap ("500+" beyond it). */
export function SearchResultsPage() {
  const [params, setParams] = useSearchParams();
  const nav = useNavigate();
  const q = (params.get('q') ?? '').trim();
  const type = params.get('type') ?? '';
  // counts per type always come from the unfiltered search, so the chips stay put while one is selected
  const counts = useLoad(() => (q ? searchApi.search(q, undefined, 1, 1) : Promise.resolve(null)), [q]);
  const { data, items, pager, error, loading, reload } = usePagedQuery(
    (page, size) => searchApi.search(q, type ? [type] : undefined, page, size), [q, type], 10);
  const byType = counts.data?.summary?.counts ?? {};
  const capped = counts.data?.summary?.capped ?? false;
  const total = Object.values(byType).reduce((a, b) => a + b, 0);
  const more = capped ? '+' : '';
  const choose = (t: string) => setParams(t ? { q, type: t } : { q }, { replace: true });

  return (
    <>
      <PageHeader title="Search" subtitle={q ? `Results for “${q}”` : 'Type in the search box at the top of the page.'} crumbs={[['Search']]} />
      {q && (
        <Panel>
          <div className="chips" role="tablist" aria-label="Result type">
            <button className={'chip' + (type ? '' : ' active')} onClick={() => choose('')}>All ({num(total)}{more})</button>
            {Object.entries(byType).sort((a, b) => b[1] - a[1]).map(([t, n]) => (
              <button key={t} className={'chip' + (type === t ? ' active' : '')} onClick={() => choose(t)}>{searchLabel(t)} ({num(n)}{more})</button>
            ))}
          </div>
          <ErrorNote error={error || counts.error} retry={() => { reload(); counts.reload(); }} />
          {loading && !data ? <Loading /> : items.length === 0 && !error ? <Empty title="No matches" text="Try fewer or different words. Accents and small typos are fine." /> : (
            <DataTable<SearchHit> rows={items} rowKey={h => h.type + h.id} onRowClick={h => nav(searchTarget(h.type, h.id), { state: { highlight: q } })} columns={[
              { header: 'Type', cell: h => searchLabel(h.type) },
              { header: 'Match', cell: h => <TitleCell title={<Highlighted html={h.highlight} fallback={h.title} />} sub={h.subtitle ?? undefined} /> },
              { header: 'Reference', cell: h => h.reference ?? '—' },
            ]} />
          )}
          <Pager {...pager} />
        </Panel>
      )}
    </>
  );
}
