import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ClipboardCheck, ClipboardList, PackagePlus, Scale, Store, Truck, Warehouse } from 'lucide-react';
import { inventoryApi, operationsApi, productsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Kpi, KpiRow, Loading, PageHeader, Tabs, ReasonModal } from '../../components/ui';
import { num } from '../../utils/format';
import type { Allocation } from '../../types/models';
import { useInventoryActions } from './useInventoryActions';
import { ApprovalsTab, approvalCount } from './tabs/ApprovalsTab';
import { CentralTab, LocationStockTab } from './tabs/StockTabs';
import { RequestsTab } from './tabs/RequestsTab';
import { TransfersTab } from './tabs/TransfersTab';
import { AdjustmentsTab } from './tabs/AdjustmentsTab';
import { StocktakesTab } from './tabs/StocktakesTab';
import { ReorderTab } from './tabs/ReorderTab';
import { MovementsTab } from './tabs/MovementsTab';
import { DiscrepanciesTab, ReceiptsTab } from './tabs/HistoryTabs';
import { ReceiptModal } from './modals/ReceiptModal';
import { TransferRequestModal, type TransferPrefill } from './modals/TransferRequestModal';
import { AdjustmentModal } from './modals/AdjustmentModal';
import { StartStocktakeModal } from './modals/StartStocktakeModal';
import { ConfirmReceiptModal } from './modals/ConfirmReceiptModal';

type Tab = 'approvals' | 'central' | 'locations' | 'requests' | 'transfers' | 'adjustments' | 'stocktakes' | 'reorder' | 'movements' | 'receipts' | 'discrepancies';
type ModalState = null
  | { kind: 'receipt'; productId?: string }
  | { kind: 'transfer'; prefill?: TransferPrefill }
  | { kind: 'adjustment' }
  | { kind: 'stocktake' }
  | { kind: 'confirm'; allocation: Allocation };

const sum = (rows: { quantity: number }[]) => rows.reduce((s, r) => s + Number(r.quantity), 0);
/** Old dashboard links used ?action=allocate. */
const ACTIONS: Record<string, ModalState> = {
  receipt: { kind: 'receipt' }, transfer: { kind: 'transfer' }, allocate: { kind: 'transfer' }, adjustment: { kind: 'adjustment' }, count: { kind: 'stocktake' },
};

/** A06 Inventory: central and location stock, approvals, transfers, adjustments, counts, reorder alerts and the ledger. */
export function InventoryPage() {
  const [params, setParams] = useSearchParams();
  const [modal, setModal] = useState<ModalState>(null);
  const [version, setVersion] = useState(0);

  const { data, error, loading, reload } = useLoad(async () => {
    const [central, locations, approvals, products, operations] = await Promise.all([
      inventoryApi.central(), inventoryApi.locations(), inventoryApi.approvals(), productsApi.all('sellable'), operationsApi.all(),
    ]);
    return { central, locations, approvals, products, operations };
  }, [version]);
  const refresh = () => setVersion(v => v + 1);
  const actions = useInventoryActions(data?.approvals.secondApproverRequired ?? true, refresh);
  const pending = approvalCount(data?.approvals);
  const tab = (params.get('tab') as Tab) || (pending > 0 ? 'approvals' : 'central');

  // Deep links from the dashboard: /inventory?action=receipt|transfer|adjustment|count
  useEffect(() => {
    const action = params.get('action');
    if (action && ACTIONS[action]) {
      const operationId = params.get('operationId') ?? undefined;
      setModal(action === 'transfer' && operationId ? { kind: 'transfer', prefill: { operationId } } : ACTIONS[action]);
      const next = new URLSearchParams(params); next.delete('action'); next.delete('operationId'); setParams(next, { replace: true });
    }
  }, [params, setParams]);

  const close = () => setModal(null);
  const done = () => { setModal(null); refresh(); };
  const secondApprover = data?.approvals.secondApproverRequired ?? true;

  return (
    <>
      <PageHeader title="Inventory" subtitle="Central and location stock, approvals, transfers, adjustments, stock counts and the stock ledger." crumbs={[['Inventory']]}
        actions={<>
          <button className="btn" onClick={() => setModal({ kind: 'receipt' })}><PackagePlus size={17} />Record Receipt</button>
          <button className="btn" onClick={() => setModal({ kind: 'adjustment' })}><Scale size={17} />Adjust Stock</button>
          <button className="btn" onClick={() => setModal({ kind: 'stocktake' })}><ClipboardList size={17} />Start Stock Count</button>
          <button className="btn gold" onClick={() => setModal({ kind: 'transfer' })}><Truck size={17} />Request Transfer</button>
        </>} />
      <ErrorNote error={error} retry={reload} />
      <ErrorNote error={actions.prompt ? undefined : actions.error} />
      {loading && !data ? <Loading /> : data && (
        <>
          <KpiRow>
            <Kpi tone="navy" icon={<Warehouse />} value={num(sum(data.central))} label="Units in Central Stock" />
            <Kpi tone="green" icon={<Store />} value={num(sum(data.locations))} label="Units at Sales Locations" />
            <Kpi tone="gold" icon={<ClipboardCheck />} value={num(pending)} label="Waiting for Approval or Dispatch" />
            <Kpi tone="purple" icon={<Truck />} value={num(data.approvals.transfersToDispatch.length + data.approvals.requestsToDispatch.length)} label="Approved, Not Yet Dispatched" />
          </KpiRow>
          <Tabs value={tab} onChange={t => setParams({ tab: t })} tabs={[
            ['approvals', `Approvals${pending ? ` (${pending})` : ''}`], ['central', 'Central Stock'], ['locations', 'Location Stock'],
            ['requests', 'Stock Requests'], ['transfers', 'Transfers'], ['adjustments', 'Adjustments'], ['stocktakes', 'Stock Counts'], ['reorder', 'Reorder Alerts'],
            ['movements', 'Movements'], ['receipts', 'Receipts'], ['discrepancies', 'Discrepancies'],
          ]} />
          {tab === 'approvals' && <ApprovalsTab data={data.approvals} actions={actions} />}
          {tab === 'central' && <CentralTab rows={data.central} />}
          {tab === 'locations' && <LocationStockTab rows={data.locations} operations={data.operations} />}
          {tab === 'requests' && <RequestsTab operations={data.operations} actions={actions} version={version} onConfirm={a => setModal({ kind: 'confirm', allocation: a })} />}
          {tab === 'transfers' && <TransfersTab operations={data.operations} actions={actions} version={version} onConfirm={a => setModal({ kind: 'confirm', allocation: a })} />}
          {tab === 'adjustments' && <AdjustmentsTab operations={data.operations} actions={actions} version={version} />}
          {tab === 'stocktakes' && <StocktakesTab version={version} />}
          {tab === 'reorder' && (
            <ReorderTab version={version}
              onRequestTransfer={r => setModal({ kind: 'transfer', prefill: { operationId: r.operation_id ?? undefined, productId: r.product_id, quantity: r.suggested_quantity } })}
              onRecordReceipt={r => setModal({ kind: 'receipt', productId: r.product_id })} />
          )}
          {tab === 'movements' && <MovementsTab operations={data.operations} products={data.products} version={version} />}
          {tab === 'receipts' && <ReceiptsTab version={version} />}
          {tab === 'discrepancies' && <DiscrepanciesTab version={version} />}
        </>
      )}
      {modal?.kind === 'receipt' && <ReceiptModal products={data?.products ?? []} productId={modal.productId} onClose={close} onDone={done} />}
      {modal?.kind === 'transfer' && <TransferRequestModal central={data?.central ?? []} operations={data?.operations ?? []} prefill={modal.prefill} secondApprover={secondApprover} onClose={close} onDone={done} />}
      {modal?.kind === 'adjustment' && <AdjustmentModal central={data?.central ?? []} locationStock={data?.locations ?? []} operations={data?.operations ?? []} secondApprover={secondApprover} onClose={close} onDone={done} />}
      {modal?.kind === 'stocktake' && <StartStocktakeModal operations={data?.operations ?? []} onClose={close} />}
      {modal?.kind === 'confirm' && <ConfirmReceiptModal allocation={modal.allocation} onClose={close} onDone={done} />}
      {actions.prompt && <ReasonModal prompt={actions.prompt} busy={actions.busy} error={actions.error} onSubmit={actions.submitPrompt} onClose={() => { actions.closePrompt(); actions.setError(''); }} />}
    </>
  );
}
