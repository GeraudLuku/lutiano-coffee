/** Payment method labels and colours. Only CASH exists today; Mobile Money methods arrive with A07. */
export const PAYMENT_LABEL: Record<string, string> = { CASH: 'Cash', MTN_MOMO: 'MTN Mobile Money', ORANGE_MONEY: 'Orange Money' };
export const PAYMENT_COLOR: Record<string, string> = { CASH: '#12805C', MTN_MOMO: '#F5B800', ORANGE_MONEY: '#F2741F' };
export const paymentLabel = (m: string) => PAYMENT_LABEL[m] ?? m;
