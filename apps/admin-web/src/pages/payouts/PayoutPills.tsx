import { LabelPill } from '../../components/ui';
import { DESTINATION_STATUS, PAYOUT_STATUS } from '../../utils/payouts';
import type { DestinationStatus, PayoutStatus } from '../../types/models';

export const PayoutStatusPill = ({ status }: { status: PayoutStatus }) => <LabelPill map={PAYOUT_STATUS} value={status} />;
export const DestinationStatusPill = ({ status }: { status: DestinationStatus }) => <LabelPill map={DESTINATION_STATUS} value={status} />;
