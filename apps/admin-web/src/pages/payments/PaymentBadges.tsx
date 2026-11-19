import { Banknote, Smartphone } from 'lucide-react';
import { Pill } from '../../components/ui';
import { PAYMENT_STATUS } from '../../utils/paymentStatus';
import type { PaymentStatus } from '../../types/models';

export const PaymentStatusPill = ({ status }: { status: PaymentStatus }) => <Pill tone={PAYMENT_STATUS[status][1]}>{PAYMENT_STATUS[status][0]}</Pill>;

export const MethodIcon = ({ code, size = 18 }: { code: string; size?: number }) =>
  code === 'CASH' ? <Banknote size={size} aria-hidden="true" /> : <Smartphone size={size} aria-hidden="true" />;

export const MethodPill = ({ code, label }: { code: string; label: string }) => (
  <Pill tone={code === 'CASH' ? 'green' : code === 'MTN_MOMO' ? 'gold' : 'amber'}><MethodIcon code={code} size={12} />{label}</Pill>
);
