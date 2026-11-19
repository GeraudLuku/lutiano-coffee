import { Pill } from '../../components/ui';
import { ADJUSTMENT_STATUS, MOVEMENT_TYPE, STOCKTAKE_STATUS, TRANSFER_STATUS } from '../../utils/inventoryLabels';
import type { AdjustmentStatus, MovementType, StocktakeStatus, TransferStatus } from '../../types/models';

export const TransferStatusPill = ({ status }: { status: TransferStatus }) => <Pill tone={TRANSFER_STATUS[status][1]}>{TRANSFER_STATUS[status][0]}</Pill>;
export const AdjustmentStatusPill = ({ status }: { status: AdjustmentStatus }) => <Pill tone={ADJUSTMENT_STATUS[status][1]}>{ADJUSTMENT_STATUS[status][0]}</Pill>;
export const StocktakeStatusPill = ({ status }: { status: StocktakeStatus }) => <Pill tone={STOCKTAKE_STATUS[status][1]}>{STOCKTAKE_STATUS[status][0]}</Pill>;
export const MovementTypePill = ({ type }: { type: MovementType }) => <Pill tone={MOVEMENT_TYPE[type]?.[1] ?? 'grey'}>{MOVEMENT_TYPE[type]?.[0] ?? type}</Pill>;

/** Signed quantity in green (in) or red (out). */
export const Delta = ({ value }: { value: number }) => (
  <b className={value > 0 ? 'delta-in' : value < 0 ? 'delta-out' : ''}>{value > 0 ? `+${value}` : value}</b>
);
