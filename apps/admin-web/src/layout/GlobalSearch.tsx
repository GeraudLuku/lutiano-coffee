import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { searchApi } from '../api/services';
import { useDebounced } from '../hooks/useDebounced';
import { useOutsideClick } from '../hooks/useOutsideClick';
import { errorMessage } from '../utils/errors';
import { Highlighted, searchLabel, searchTarget } from '../utils/search';
import type { SearchHit } from '../types/models';

/** As-you-type suggestions from the search service, grouped by type. Enter (or "See all results") opens the full results page. */
export function GlobalSearch() {
  const nav = useNavigate();
  const [q, setQ] = useState('');
  const [hits, setHits] = useState<SearchHit[] | null>(null);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useOutsideClick(ref, useCallback(() => setOpen(false), []));

  const term = useDebounced(q.trim(), 250);
  useEffect(() => {
    if (term.length < 2) { setHits(null); setError(''); return; }
    let cancelled = false;
    searchApi.suggestions(term)
      .then(r => { if (!cancelled) { setHits(r); setError(''); } })
      .catch(e => { if (!cancelled) { setHits([]); setError(errorMessage(e)); } });
    return () => { cancelled = true; };
  }, [term]);

  const close = () => { setOpen(false); setQ(''); setHits(null); };
  const go = (to: string, highlight?: string) => { nav(to, highlight ? { state: { highlight } } : undefined); close(); };
  const all = () => { const term = q.trim(); if (term) go(`/search?q=${encodeURIComponent(term)}`); };

  return (
    <div className="search" ref={ref}>
      <Search size={18} aria-hidden="true" />
      <input value={q} placeholder="Search anything… (e.g. user, sale, product, location)" aria-label="Search" maxLength={100}
        onFocus={() => setOpen(true)} onChange={e => { setQ(e.target.value); setOpen(true); }}
        onKeyDown={e => { if (e.key === 'Enter') all(); if (e.key === 'Escape') setOpen(false); }} />
      {open && q.trim().length >= 2 && (
        <div className="dropdown search-results">
          {error ? <p className="muted">{error}</p>
            : !hits ? <p className="muted">Searching…</p>
            : hits.length === 0 ? <p className="muted">No matches for “{q}”.</p>
            : (
              <>
                {hits.map(h => (
                  <button key={h.type + h.id} onClick={() => go(searchTarget(h.type, h.id), q.trim())}>
                    <span className="kind">{searchLabel(h.type)}</span>
                    <b><Highlighted html={h.highlight} fallback={h.title} /></b>
                    <small>{h.subtitle ?? h.reference ?? ''}</small>
                  </button>
                ))}
                <button className="see-all" onClick={all}>See all results for “{q.trim()}”</button>
              </>
            )}
        </div>
      )}
    </div>
  );
}
