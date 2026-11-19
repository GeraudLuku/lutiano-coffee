import { campaignsApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, ListState, Modal, Pager } from '../../components/ui';
import { date, xaf } from '../../utils/format';
import type { Campaign } from '../../types/models';

/** Every seller the campaign reaches, with net sales so far and the bonus they would get now. */
export function CampaignStandings({ campaign, onClose }: { campaign: Campaign; onClose: () => void }) {
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => campaignsApi.standings(campaign.id, { page, size }), [campaign.id]);
  return (
    <Modal title={`${campaign.name} · ${date(campaign.starts_on)} – ${date(campaign.ends_on)}`} onClose={onClose}>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No sellers', text: 'No active seller takes part in this campaign.' }}>
        <DataTable rows={items} rowKey={s => s.seller_id} columns={[
          { header: 'Seller', cell: s => s.seller_name },
          { header: 'Net sales', cell: s => xaf(s.sales_xaf) },
          { header: campaign.settled ? 'Bonus' : 'Bonus now', cell: s => (s.bonus_xaf ? xaf(s.bonus_xaf) : '—') },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Modal>
  );
}
