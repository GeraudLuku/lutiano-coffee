import { useEffect, useState, type RefObject } from 'react';
import { useLocation } from 'react-router-dom';

type HighlightRegistry = { set(name: string, h: unknown): void; delete(name: string): void };
const NAME = 'search-hit';
const MAX_RANGES = 500;

/** Lowercase and strip accents one character at a time, so positions in the folded text are positions in the original. */
function fold(s: string): string {
  let out = '';
  for (const ch of s) {
    const f = ch.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
    out += f.length === ch.length ? f : ch.toLowerCase().length === ch.length ? ch.toLowerCase() : ch;
  }
  return out;
}

/**
 * After a search result is opened, marks the searched words on the page (accent- and case-insensitive) and scrolls to the first.
 * Uses the browser's CSS Custom Highlight API, so the page's DOM is never changed; browsers without it simply show no marks.
 * The words arrive in the navigation state ({ highlight }), so they survive a reload but not a visit to another page.
 */
export function useSearchHighlight(root: RefObject<HTMLElement | null>) {
  const location = useLocation();
  const term = ((location.state as { highlight?: string } | null)?.highlight ?? '').trim();
  const [dismissedKey, setDismissedKey] = useState('');
  const active = term.length >= 2 && dismissedKey !== location.key;

  useEffect(() => {
    const registry = (globalThis.CSS as unknown as { highlights?: HighlightRegistry } | undefined)?.highlights;
    const HighlightCtor = (globalThis as unknown as { Highlight?: new (...r: Range[]) => unknown }).Highlight;
    const el = root.current;
    if (!active || !registry || !HighlightCtor || !el) { registry?.delete(NAME); return; }
    const words = fold(term).split(/\s+/).filter(w => w.length >= 2);
    let scrolled = false;
    const paint = () => {
      const ranges: Range[] = [];
      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, {
        acceptNode: n => (n.parentElement?.closest('script,style,textarea,input,select') ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT),
      });
      for (let n = walker.nextNode(); n && ranges.length < MAX_RANGES; n = walker.nextNode()) {
        const text = n.textContent ?? '';
        if (!text.trim()) continue;
        const folded = fold(text);
        for (const w of words) {
          for (let i = folded.indexOf(w); i !== -1 && ranges.length < MAX_RANGES; i = folded.indexOf(w, i + w.length)) {
            const r = document.createRange();
            r.setStart(n, i); r.setEnd(n, i + w.length);
            ranges.push(r);
          }
        }
      }
      if (ranges.length) {
        registry.set(NAME, new HighlightCtor(...ranges));
        if (!scrolled) { scrolled = true; ranges[0]!.startContainer.parentElement?.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
      } else registry.delete(NAME);
    };
    paint();
    // pages load their data after the first render, so repaint as the content changes (highlighting never changes the DOM, so this cannot loop)
    let timer: ReturnType<typeof setTimeout> | undefined;
    const observer = new MutationObserver(() => { clearTimeout(timer); timer = setTimeout(paint, 150); });
    observer.observe(el, { childList: true, subtree: true, characterData: true });
    return () => { observer.disconnect(); clearTimeout(timer); registry.delete(NAME); };
  }, [active, term, root, location.key]);

  return { term: active ? term : '', dismiss: () => setDismissedKey(location.key) };
}
