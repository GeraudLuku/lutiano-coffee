/** Fire-and-forget notifications; rendered by <Toasts /> (components/ui/Toasts.tsx). */
export const TOAST_EVENT = 'lutiano:toast';
export type ToastKind = 'success' | 'error';

export function toast(message: string, kind: ToastKind = 'success') {
  window.dispatchEvent(new CustomEvent(TOAST_EVENT, { detail: { message, kind } }));
}
