import { Pill } from '../../components/ui';
import { REFUND_STATUS } from '../../utils/refunds';
import type { RefundStatus } from '../../types/models';

export const RefundStatusPill = ({ status }: { status: RefundStatus }) => <Pill tone={REFUND_STATUS[status][1]}>{REFUND_STATUS[status][0]}</Pill>;
