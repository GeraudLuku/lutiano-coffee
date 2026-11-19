import { useState } from 'react';
import { referenceApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { Field, FormGrid } from '../../components/ui';

const OTHER = '__other__';

/** Region dropdown (from the API) and a city dropdown limited to that region, with "Other…" for unlisted towns. */
export function RegionCityFields({ region, city, onChange }: {
  region: string; city: string; onChange: (region: string, city: string) => void;
}) {
  const { data: regions, error } = useLoad(() => referenceApi.regions(), []);
  const [typing, setTyping] = useState(false);
  const cities = regions?.find(r => r.name === region)?.cities ?? [];
  const custom = typing || (!!city && !!regions && !cities.includes(city));

  return (
    <FormGrid>
      <Field label="Region" hint={error ? 'Could not load regions.' : undefined}>
        <select className="input" value={region} disabled={!regions}
          onChange={e => { setTyping(false); onChange(e.target.value, ''); }}>
          <option value="">{regions ? 'Select region' : 'Loading…'}</option>
          {(regions ?? []).map(r => <option key={r.name} value={r.name}>{r.name}</option>)}
        </select>
      </Field>
      <Field label="City" hint={!region ? 'Choose a region first.' : undefined}>
        <select className="input" value={custom ? OTHER : city} disabled={!region}
          onChange={e => {
            if (e.target.value === OTHER) { setTyping(true); onChange(region, ''); }
            else { setTyping(false); onChange(region, e.target.value); }
          }}>
          <option value="">Select city</option>
          {cities.map(c => <option key={c} value={c}>{c}</option>)}
          <option value={OTHER}>Other…</option>
        </select>
        {custom && (
          <input className="input" aria-label="City name" placeholder="Type the town name" value={city}
            onChange={e => onChange(region, e.target.value)} maxLength={80} autoFocus={typing} />
        )}
      </Field>
    </FormGrid>
  );
}
