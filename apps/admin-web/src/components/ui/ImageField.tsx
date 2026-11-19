import { ImagePlus } from 'lucide-react';
import { mediaUrl } from '../../api/client';
import { useObjectUrl } from '../../hooks/useObjectUrl';
import { MAX_IMAGE_BYTES } from '../../utils/images';
import { toast } from './Toasts';

/** Image picker with a preview of the chosen file, or of the saved image until one is chosen. */
export function ImageField({ url, file, onChange, label = 'Choose an image', round, compact }: {
  url?: string | null; file: File | null; onChange: (file: File | null) => void; label?: string; round?: boolean; compact?: boolean;
}) {
  const chosen = useObjectUrl(file);
  const preview = chosen ?? mediaUrl(url);
  function pick(picked: File | null) {
    if (picked && picked.size > MAX_IMAGE_BYTES) { toast('Images must be 5 MB or smaller.', 'error'); return; }
    onChange(picked);
  }
  return (
    <label className={`image-drop${round ? ' round' : ''}${compact ? ' compact' : ''}`}>
      {preview ? <img src={preview} alt="" /> : <><ImagePlus size={compact ? 26 : 36} /><span>{label}</span></>}
      <input type="file" accept="image/jpeg,image/png,image/webp" onChange={e => pick(e.target.files?.[0] ?? null)} />
    </label>
  );
}
