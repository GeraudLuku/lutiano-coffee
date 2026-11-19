import { useState } from 'react';
import { Download } from 'lucide-react';
import { productsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormModal, Note } from '../../components/ui';

const TEMPLATE = 'sku,name,category,price_xaf,unit,description,low_stock_threshold,track_stock,active\nCOF-EXAMPLE,Example Coffee,Beverage,1500,cup,Short description,10,yes,yes\n';

/** Creates products from a CSV file. All or nothing: any wrong line stops the import and is listed. */
export function ImportProductsModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const { busy, error, setError, run } = useAction();
  function template() {
    const url = URL.createObjectURL(new Blob([TEMPLATE], { type: 'text/csv' }));
    const a = document.createElement('a'); a.href = url; a.download = 'lutiano-products-template.csv'; a.click(); URL.revokeObjectURL(url);
  }
  async function submit() {
    if (!file) { setError('Choose a CSV file.'); return; }
    const r = await run(() => productsApi.importCsv(file));
    if (r) onDone();
  }
  return (
    <FormModal title="Import products" submitLabel="Import" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Note>Upload a CSV with one product per line. Required columns: sku, name, category, price_xaf. Optional: unit, description, low_stock_threshold, track_stock, active (yes/no).
        Categories must already exist. Products are created as single items; add variants afterwards. If any line is wrong, nothing is imported.</Note>
      <button type="button" className="btn sm" onClick={template}><Download size={14} />Download template</button>
      <Field label="CSV file" required><input className="input" type="file" accept=".csv,text/csv" onChange={e => setFile(e.target.files?.[0] ?? null)} required /></Field>
    </FormModal>
  );
}
