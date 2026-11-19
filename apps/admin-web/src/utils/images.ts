import { toast } from '../components/ui';
import { errorMessage } from './errors';

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** Uploads a picked image after its owner was saved. A failed upload only warns: the saved record is kept. */
export async function uploadPicked(file: File | null, upload: (file: File) => Promise<unknown>, what: string) {
  if (!file) return;
  try { await upload(file); }
  catch (x) { toast(`${what} saved, but the image upload failed: ${errorMessage(x)}`, 'error'); }
}
