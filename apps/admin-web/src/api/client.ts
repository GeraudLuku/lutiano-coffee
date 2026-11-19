import type { Session } from '../types/models';

export const API_BASE: string = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api/v1';
const SESSION_KEY = 'lutiano-admin-session';
export const SIGNED_OUT_EVENT = 'lutiano:signed-out';

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

// ---------- session (only the short-lived token is stored, never the password) ----------
export function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    if (!s.token || new Date(s.expiresAt).getTime() <= Date.now()) { localStorage.removeItem(SESSION_KEY); return null; }
    return s;
  } catch { return null; }
}
export function saveSession(s: Session | null) {
  try { if (s) localStorage.setItem(SESSION_KEY, JSON.stringify(s)); else localStorage.removeItem(SESSION_KEY); } catch { /* storage unavailable */ }
}

// ---------- transport ----------
function authHeaders(extra?: Record<string, string>): Record<string, string> {
  const s = loadSession();
  return { ...(s ? { Authorization: `Bearer ${s.token}` } : {}), ...extra };
}

async function failure(r: Response): Promise<never> {
  if (r.status === 401) { saveSession(null); window.dispatchEvent(new Event(SIGNED_OUT_EVENT)); }
  let message = r.status === 403 ? 'You do not have permission to do this.' : `Request failed (${r.status}).`;
  try { const body = await r.json(); message = body?.error?.message ?? body?.message ?? message; } catch { /* not JSON */ }
  throw new ApiError(r.status, message);
}

async function send(path: string, init: RequestInit): Promise<Response> {
  let r: Response;
  try { r = await fetch(API_BASE + path, init); }
  catch { throw new ApiError(0, 'Cannot reach the LUTIANO server. Check your connection and try again.'); }
  if (!r.ok) await failure(r);
  return r;
}

async function json<T>(method: string, path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
  const r = await send(path, {
    method,
    headers: authHeaders(body !== undefined ? { 'Content-Type': 'application/json', ...headers } : headers),
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return unwrap<T>(r);
}

/**
 * Every JSON response is a GenericResponse envelope: { success, data, error, timestamp }. Callers get `data`;
 * failures never reach here (send() throws first).
 */
async function unwrap<T>(r: Response): Promise<T> {
  const text = await r.text();
  if (!text) return null as T;
  const body = JSON.parse(text);
  return (body && typeof body === 'object' && 'success' in body ? body.data : body) as T;
}

export const http = {
  get: <T>(path: string) => json<T>('GET', path),
  post: <T>(path: string, body?: unknown, headers?: Record<string, string>) => json<T>('POST', path, body ?? {}, headers),
  put: <T>(path: string, body: unknown) => json<T>('PUT', path, body),
  del: <T>(path: string) => json<T>('DELETE', path),
  async upload<T>(path: string, file: File): Promise<T> {
    const fd = new FormData();
    fd.append('file', file);
    return unwrap<T>(await send(path, { method: 'POST', headers: authHeaders(), body: fd }));
  },
  /** Multipart form with several fields (the browser sets the boundary). */
  async uploadForm<T>(path: string, form: FormData): Promise<T> {
    return unwrap<T>(await send(path, { method: 'POST', headers: authHeaders(), body: form }));
  },
  async blob(path: string): Promise<Blob> {
    return (await send(path, { headers: authHeaders() })).blob();
  },
};

/** Query string from defined, non-empty values. */
export function qs(params: Record<string, string | number | undefined | null>): string {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.set(k, String(v)); });
  const s = p.toString();
  return s ? `?${s}` : '';
}

/** Product images are served by the API; older records may hold absolute URLs. */
export function mediaUrl(url?: string | null): string | undefined {
  if (!url) return undefined;
  return /^https?:\/\//.test(url) ? url : API_BASE + url;
}
