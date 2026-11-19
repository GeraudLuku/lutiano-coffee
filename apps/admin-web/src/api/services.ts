/** One service object per API domain. Pages call these instead of building URLs. */
import { http, qs, saveSession } from './client';
import { fetchAll } from './pages';
import type {
  OpeningHours, CustomerOrder, Announcement, AnnouncementInput, SupportRequest, Campaign, CampaignInput, CampaignStanding,
  StockRequest,
  Addon, AddonInput, AssignmentChange, CommissionBasis, CommissionLine, CommissionRule, EarningAdjustment, EarningAdjustmentType, EarningPeriod,
  EarningPeriodDetail, EarningsOverview, SellerRole, SellerStatement, SellerTarget, TargetInput, CashLedgerEntry, PriceChange, Customer, Page, PageParams, PointBatch, CustomerDetail, EarningRule, LoyaltyLedgerEntry, LoyaltyOverview, LoyaltySettings, LoyaltyTier, PointsAdjustment,
  Redemption, Reward, RewardInput, RuleInput, CashOutflow, CashRemittance, CashShiftDetail, CashSummary, Payment, PaymentMethod, Reconciliation, Refund, RefundDetail, RefundableSale, RefundInput, RefundPayInput, SellerDocument, SellerSummary, Adjustment, AdjustmentInput, Allocation, Category, CategoryInput, VariantInput, Approvals, AuditEvent, Movement, ReceiptInput, ReorderAlert, Stocktake, StocktakeDetail, CashShift, CentralStock, DashboardData, Discrepancy, LocationStock, Operation, OperationDetail,
  OperationInput, Product, Region, ProductDetail, ProductInput, SaleDetail, SaleSummary, Session, SessionUser,
  StaffDetail, StaffInput, StaffSummary, StockReceipt,
  BusinessInfo, ReportOverview, SalesReport, BuilderReport, ProductPerformance, ProductRewards, InboxMessage, NotificationDelivery, NotificationEvent, NotificationSettings, MfaChallenge, DestinationInput, Payout, PayoutDestination, PayoutDetail, PayoutFilter, PayoutInput, PayoutOverview, PeriodBalance, SellerBalance, SearchHit, SearchSummary,
} from '../types/models';

type Status = { status: string };

export const authApi = {
  /** A session, or the second step (an SMS code) for administrators. */
  async login(email: string, password: string): Promise<Session | MfaChallenge> {
    const s = await http.post<Session | MfaChallenge>('/auth/login', { email, password });
    if (!('mfaRequired' in s)) saveSession(s);
    return s;
  },
  async verifyMfa(challenge: string, code: string): Promise<Session> {
    const s = await http.post<Session>('/auth/mfa/verify', { challenge, code });
    saveSession(s);
    return s;
  },
  resendMfa: (challenge: string) => http.post<{ resendAfterSeconds: number }>('/auth/mfa/resend', { challenge }),
  me: () => http.get<SessionUser>('/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) => http.post<Status>('/auth/password', { currentPassword, newPassword }),
};

/** Either the last N days, or an inclusive Douala date range. */
export type DashboardRange = { days: number } | { from: string; to: string };
export const dashboardApi = {
  get: (range: DashboardRange) => http.get<DashboardData>(`/dashboard${qs({ ...range })}`),
};

export const staffApi = {
  list: (f: PageParams & { role?: string; active?: string } = {}) => http.get<Page<StaffSummary>>(`/staff${qs({ ...f })}`),
  all: () => fetchAll(p => staffApi.list({ page: p, size: 200 })),
  get: (id: string) => http.get<StaffDetail>(`/staff/${id}`),
  create: (body: StaffInput) => http.post<StaffSummary>('/staff', body),
  update: (id: string, body: StaffInput) => http.put<StaffSummary>(`/staff/${id}`, body),
  uploadImage: (id: string, file: File) => http.upload<{ imageUrl: string }>(`/staff/${id}/image`, file),
  assignmentHistory: (id: string, f: PageParams = {}) => http.get<Page<AssignmentChange>>(`/staff/${id}/assignment-history${qs({ ...f })}`),
  sellers: (f: PageParams & { sellerRole?: string; active?: string; eligible?: string } = {}) => http.get<Page<SellerSummary>>(`/sellers${qs({ ...f })}`),
  /** Needs a reason; blocked while the seller has an open cash shift; signs the account out everywhere. */
  deactivate: (id: string, reason: string) => http.post<Status>(`/staff/${id}/deactivate`, { reason }),
  activate: (id: string) => http.post<Status>(`/staff/${id}/activate`),
  /** Also signs the account out everywhere. */
  resetPassword: (id: string, password: string) => http.post<Status>(`/staff/${id}/password`, { password }),
  resetMfa: (id: string) => http.post<Status>(`/staff/${id}/mfa/reset`),
  revokeSessions: (id: string) => http.post<Status>(`/staff/${id}/revoke-sessions`),
  uploadDocument: (id: string, form: { type: string; expiresOn?: string; note?: string; file: File }) => {
    const fd = new FormData();
    fd.append('type', form.type);
    if (form.expiresOn) fd.append('expiresOn', form.expiresOn);
    if (form.note) fd.append('note', form.note);
    fd.append('file', form.file);
    return http.uploadForm<SellerDocument>(`/staff/${id}/documents`, fd);
  },
  verifyDocument: (id: string, documentId: string, note?: string) => http.post<SellerDocument>(`/staff/${id}/documents/${documentId}/verify`, { note }),
  rejectDocument: (id: string, documentId: string, note: string) => http.post<SellerDocument>(`/staff/${id}/documents/${documentId}/reject`, { note }),
  documentFile: (id: string, documentId: string) => http.blob(`/staff/${id}/documents/${documentId}/file`),
  assign: (sellerId: string, operationId: string) => http.post<Status>(`/staff/${sellerId}/operations/${operationId}`),
  unassign: (sellerId: string, operationId: string) => http.del<Status>(`/staff/${sellerId}/operations/${operationId}`),
};

export const operationsApi = {
  list: (f: PageParams & { type?: string; active?: string } = {}) => http.get<Page<Operation>>(`/operations${qs({ ...f })}`),
  /** Every location, for filters and dropdowns. */
  all: () => fetchAll(p => operationsApi.list({ page: p, size: 200 })),
  get: (id: string) => http.get<OperationDetail>(`/operations/${id}`),
  create: (body: OperationInput) => http.post<Operation>('/operations', body),
  update: (id: string, body: OperationInput) => http.put<Operation>(`/operations/${id}`, body),
  setActive: (id: string, active: boolean) => http.post<Status>(`/operations/${id}/${active ? 'activate' : 'deactivate'}`),
  qrImage: (id: string) => http.blob(`/operations/${id}/qr.png`),
  /** Every QR code (optionally of one type or status) as a zip of PNGs. */
  qrZip: (f: { type?: string; active?: string } = {}) => http.blob(`/operations/qr-codes.zip${qs({ ...f })}`),
  uploadImage: (id: string, file: File) => http.upload<{ imageUrl: string }>(`/operations/${id}/image`, file),
  /** Replaces the opening hours; no rows means always open. */
  setHours: (id: string, hours: OpeningHours[]) => http.put<OpeningHours[]>(`/operations/${id}/hours`, hours),
};

export const productsApi = {
  list: (f: PageParams & { view?: 'catalog' | 'sellable' | 'all'; category?: string; active?: string } = {}) => http.get<Page<Product>>(`/products${qs({ ...f })}`),
  /** Every product in a view, for dropdowns and exports. */
  all: (view: 'catalog' | 'sellable' | 'all' = 'all') => fetchAll(p => productsApi.list({ view, page: p, size: 200 })),
  priceHistory: (id: string, f: PageParams = {}) => http.get<Page<PriceChange>>(`/products/${id}/price-history${qs({ ...f })}`),
  categories: () => http.get<string[]>('/products/categories'),
  get: (id: string) => http.get<ProductDetail>(`/products/${id}`),
  create: (body: ProductInput) => http.post<Product>('/products', body),
  update: (id: string, body: ProductInput) => http.put<Product>(`/products/${id}`, body),
  setActive: (id: string, active: boolean) => http.post<Status>(`/products/${id}/${active ? 'activate' : 'deactivate'}`),
  uploadImage: (id: string, file: File) => http.upload<{ imageUrl: string }>(`/products/${id}/image`, file),
  rewards: (id: string) => http.get<ProductRewards[]>(`/products/${id}/rewards`),
  performance: (id: string, days = 30) => http.get<ProductPerformance>(`/products/${id}/performance${qs({ days })}`),
  importCsv: (file: File) => http.upload<{ created: number }>('/products/import', file),
  addVariant: (parentId: string, body: VariantInput) => http.post<Product>(`/products/${parentId}/variants`, body),
  updateVariant: (parentId: string, variantId: string, body: VariantInput) => http.put<Product>(`/products/${parentId}/variants/${variantId}`, body),
  /** Add-ons allowed with a simple product, or with every variant of a parent. */
  setAddons: (id: string, addonIds: string[]) => http.put<ProductDetail>(`/products/${id}/addons`, { addonIds }),
};

export const categoriesApi = {
  list: () => http.get<Category[]>('/product-categories'),
  create: (body: CategoryInput) => http.post<Category>('/product-categories', body),
  update: (id: string, body: CategoryInput) => http.put<Category>(`/product-categories/${id}`, body),
  uploadImage: (id: string, file: File) => http.upload<{ imageUrl: string }>(`/product-categories/${id}/image`, file),
};

export const addonsApi = {
  list: () => http.get<Addon[]>('/addons'),
  create: (body: AddonInput) => http.post<Addon>('/addons', body),
  update: (id: string, body: AddonInput) => http.put<Addon>(`/addons/${id}`, body),
  uploadImage: (id: string, file: File) => http.upload<{ imageUrl: string }>(`/addons/${id}/image`, file),
};

export const inventoryApi = {
  central: () => http.get<CentralStock[]>('/inventory/central'),
  /** Every location's stock (the API pages it; this reads all pages). */
  locations: (operationId?: string) => fetchAll((page, size) => http.get<Page<LocationStock>>(`/inventory${qs({ operationId, page, size })}`)),
  receipts: (f: PageParams = {}) => http.get<Page<StockReceipt>>(`/inventory/receipts${qs({ ...f })}`),
  recordReceipt: (body: ReceiptInput) => http.post('/inventory/receipts', body),
  discrepancies: (f: PageParams = {}) => http.get<Page<Discrepancy>>(`/inventory/discrepancies${qs({ ...f })}`),
  reorder: () => http.get<ReorderAlert[]>('/inventory/reorder'),
  approvals: () => http.get<Approvals>('/inventory/approvals'),
  /** location: 'CENTRAL', an operation id, or undefined for all. Dates are Douala days. */
  ledger: (f: PageParams & { location?: string; productId?: string; type?: string; from?: string; to?: string }) =>
    http.get<Page<Movement>>(`/inventory/ledger${qs({ ...f })}`),
};

/** Transfers from central stock to sales locations (API path: /inventory/allocations). */
export const transfersApi = {
  list: (f: PageParams & { status?: Allocation['status'] | ''; operationId?: string } = {}) => http.get<Page<Allocation>>(`/inventory/allocations${qs({ ...f })}`),
  request: (operationId: string, productId: string, quantity: number, note?: string) =>
    http.post<Allocation>('/inventory/allocations', { operationId, productId, quantity, note }),
  approve: (id: string, note?: string) => http.post<Allocation>(`/inventory/allocations/${id}/approve`, { note }),
  reject: (id: string, note: string) => http.post<Allocation>(`/inventory/allocations/${id}/reject`, { note }),
  dispatch: (id: string) => http.post<Allocation>(`/inventory/allocations/${id}/dispatch`),
  cancel: (id: string, note: string) => http.post<Allocation>(`/inventory/allocations/${id}/cancel`, { note }),
  confirm: (allocationId: string, quantity: number, note?: string) => http.post('/inventory/allocations/confirm', { allocationId, quantity, note }),
};

/** Stock requests: several products for one sales location, approved, rejected, dispatched or cancelled as a whole. */
export const stockRequestsApi = {
  list: (f: PageParams & { status?: StockRequest['status'] | ''; operationId?: string; reference?: string } = {}) => http.get<Page<StockRequest>>(`/inventory/requests${qs({ ...f })}`),
  approve: (id: string, note?: string) => http.post<StockRequest>(`/inventory/requests/${id}/approve`, { note }),
  reject: (id: string, note: string) => http.post<StockRequest>(`/inventory/requests/${id}/reject`, { note }),
  dispatch: (id: string) => http.post<StockRequest>(`/inventory/requests/${id}/dispatch`),
  cancel: (id: string, note: string) => http.post<StockRequest>(`/inventory/requests/${id}/cancel`, { note }),
};

export const adjustmentsApi = {
  /** location: 'CENTRAL', an operation id, or undefined for all. */
  list: (f: PageParams & { status?: Adjustment['status'] | ''; location?: string } = {}) => http.get<Page<Adjustment>>(`/inventory/adjustments${qs({ ...f })}`),
  request: (body: AdjustmentInput) => http.post<Adjustment>('/inventory/adjustments', body),
  approve: (id: string, note?: string) => http.post<Adjustment>(`/inventory/adjustments/${id}/approve`, { note }),
  reject: (id: string, note: string) => http.post<Adjustment>(`/inventory/adjustments/${id}/reject`, { note }),
  cancel: (id: string) => http.post<Adjustment>(`/inventory/adjustments/${id}/cancel`),
};

export const stocktakesApi = {
  list: (f: PageParams & { status?: Stocktake['status'] | ''; location?: string } = {}) => http.get<Page<Stocktake>>(`/inventory/stocktakes${qs({ ...f })}`),
  /** operationId null counts central stock. */
  start: (operationId: string | null, note?: string) => http.post<StocktakeDetail>('/inventory/stocktakes', { operationId, note }),
  get: (id: string) => http.get<StocktakeDetail>(`/inventory/stocktakes/${id}`),
  saveCounts: (id: string, counts: { productId: string; countedQuantity: number | null }[]) =>
    http.put<StocktakeDetail>(`/inventory/stocktakes/${id}/counts`, { counts }),
  submit: (id: string) => http.post<StocktakeDetail>(`/inventory/stocktakes/${id}/submit`),
  approve: (id: string, note?: string) => http.post<StocktakeDetail>(`/inventory/stocktakes/${id}/approve`, { note }),
  reject: (id: string, note: string) => http.post<StocktakeDetail>(`/inventory/stocktakes/${id}/reject`, { note }),
  cancel: (id: string, note: string) => http.post<StocktakeDetail>(`/inventory/stocktakes/${id}/cancel`, { note }),
};

/** channel is an operation type (FIXED_POS, MOBILE, MARKET_AGENT); paymentMethod e.g. CASH. */
export interface SalesFilter { from?: string; to?: string; operationId?: string; sellerId?: string; channel?: string; paymentMethod?: string }
export const salesApi = {
  list: (f: SalesFilter & PageParams = {}) => http.get<Page<SaleSummary, { sales: number; revenue_xaf: number; units: number; points_issued: number; commission_xaf: number }>>(`/sales${qs({ ...f })}`),
  get: (id: string) => http.get<SaleDetail>(`/sales/${id}`),
  receipt: (id: string) => http.blob(`/sales/${id}/receipt.pdf`),
  openShifts: () => http.get<CashShift[]>('/shifts/open'),
  /** An administrator records a sale for a seller with an open shift. The key makes a retry return the same sale. */
  recordForSeller: (body: { sellerId: string; operationId: string; lines: { productId: string; quantity: number; addonIds: string[] }[]; paymentMethod: string; cashReceivedXaf?: number; customerToken?: string }, key: string) =>
    http.post<SaleDetail>('/sales/on-behalf', body, { 'Idempotency-Key': key }),
};

export const referenceApi = {
  regions: () => http.get<Region[]>('/reference/regions'),
};

export const auditApi = {
  list: (f: PageParams & { action?: string; entityType?: string; entityId?: string; actorId?: string; from?: string; to?: string } = {}) => http.get<Page<AuditEvent, { events: number; users: number; critical: number }>>(`/audit${qs({ ...f })}`),
  actions: () => http.get<string[]>('/audit/actions'),
};


export interface PaymentFilter { from?: string; to?: string; method?: string; status?: string; operationId?: string; sellerId?: string }
export const paymentsApi = {
  methods: () => http.get<PaymentMethod[]>('/payment-methods'),
  updateMethod: (code: string, body: { label: string; merchantAccount?: string | null; enabled: boolean }) => http.put<PaymentMethod>(`/payment-methods/${code}`, body),
  list: (f: PaymentFilter & PageParams = {}) => http.get<Page<Payment, { confirmed_count: number; confirmed_xaf: number; cash_xaf: number; mobile_money_xaf: number; pending_count: number }>>(`/payments${qs({ ...f })}`),
  get: (id: string) => http.get<Payment>(`/payments/${id}`),
  reconciliation: (from?: string, to?: string) => http.get<Reconciliation>(`/payments/reconciliation${qs({ from, to })}`),
};

export interface RefundFilter { from?: string; to?: string; status?: string; operationId?: string; sellerId?: string }
export const refundsApi = {
  list: (f: RefundFilter & PageParams = {}) => http.get<Page<Refund, { refunds: number; refunded_xaf: number; paid: number; pending: number; rejected: number }>>(`/refunds${qs({ ...f })}`),
  get: (id: string) => http.get<RefundDetail>(`/refunds/${id}`),
  settings: () => http.get<{ secondApprover: boolean }>('/refunds/settings'),
  sale: (q: { saleId?: string; saleReference?: string }) => http.get<RefundableSale>(`/refunds/sale${qs(q)}`),
  request: (body: RefundInput) => http.post<RefundDetail>('/refunds', body),
  approve: (id: string, note?: string) => http.post<RefundDetail>(`/refunds/${id}/approve`, { note }),
  reject: (id: string, note: string) => http.post<RefundDetail>(`/refunds/${id}/reject`, { note }),
  cancel: (id: string, note: string) => http.post<RefundDetail>(`/refunds/${id}/cancel`, { note }),
  pay: (id: string, body: RefundPayInput) => http.post<RefundDetail>(`/refunds/${id}/pay`, body),
  returnStock: (id: string, lines: { productId: string; quantity: number }[], note: string) => http.post<RefundDetail>(`/refunds/${id}/stock-returns`, { lines, note }),
  addEvidence: (id: string, file: File) => http.upload<RefundDetail>(`/refunds/${id}/evidence`, file),
  evidenceFile: (id: string, evidenceId: string) => http.blob(`/refunds/${id}/evidence/${evidenceId}`),
};

export const cashApi = {
  summary: () => http.get<CashSummary>('/cash/summary'),
  shifts: (f: PageParams & { status?: string; review?: string } = {}) => http.get<Page<CashShift>>(`/cash/shifts${qs({ ...f })}`),
  shiftLedger: (id: string, f: PageParams = {}) => http.get<Page<CashLedgerEntry>>(`/cash/shifts/${id}/ledger${qs({ ...f })}`),
  shift: (id: string) => http.get<CashShiftDetail>(`/cash/shifts/${id}`),
  review: (id: string, decision: 'ACCEPT' | 'RECOUNT', note: string) => http.post<CashShiftDetail>(`/cash/shifts/${id}/review`, { decision, note }),
  outflows: (f: PageParams & { status?: string } = {}) => http.get<Page<CashOutflow>>(`/cash/outflows${qs({ ...f })}`),
  approveOutflow: (id: string, note?: string) => http.post<CashOutflow>(`/cash/outflows/${id}/approve`, { note }),
  rejectOutflow: (id: string, note: string) => http.post<CashOutflow>(`/cash/outflows/${id}/reject`, { note }),
  remittances: (f: PageParams & { status?: string; mine?: boolean } = {}) =>
    http.get<Page<CashRemittance>>(`/cash/remittances${qs({ ...f, mine: f.mine ? 'true' : undefined })}`),
  acknowledge: (id: string, receivedXaf: number, note?: string) => http.post<CashRemittance>(`/cash/remittances/${id}/acknowledge`, { receivedXaf, note }),
};

export interface CustomerInput { fullName: string; phone: string; phoneVerified: boolean; status?: string }
export const customersApi = {
  list: (f: PageParams & { tier?: string; status?: string } = {}) => http.get<Page<Customer>>(`/customers${qs({ ...f })}`),
  ledger: (id: string, f: PageParams = {}) => http.get<Page<LoyaltyLedgerEntry>>(`/customers/${id}/ledger${qs({ ...f })}`),
  batches: (id: string, f: PageParams = {}) => http.get<Page<PointBatch>>(`/customers/${id}/batches${qs({ ...f })}`),
  redemptions: (id: string, f: PageParams = {}) => http.get<Page<Redemption>>(`/customers/${id}/redemptions${qs({ ...f })}`),
  get: (id: string) => http.get<CustomerDetail>(`/customers/${id}`),
  create: (body: CustomerInput) => http.post<CustomerDetail>('/customers', body),
  update: (id: string, body: CustomerInput) => http.put<CustomerDetail>(`/customers/${id}`, body),
  reissueQr: (id: string) => http.post<CustomerDetail>(`/customers/${id}/reissue-qr`),
  qr: (id: string) => http.blob(`/customers/${id}/qr.png`),
};

export const loyaltyApi = {
  overview: (from?: string, to?: string) => http.get<LoyaltyOverview>(`/loyalty/overview${qs({ from, to })}`),
  ledger: (f: PageParams & { type?: string; from?: string; to?: string } = {}) => http.get<Page<LoyaltyLedgerEntry>>(`/loyalty/ledger${qs({ ...f })}`),
  settings: () => http.get<LoyaltySettings>('/loyalty/settings'),
  saveSettings: (body: { programActive: boolean; basePoints: number; baseAmountXaf: number; pointValueXaf: number; expiryMonths: number; expiryNoticeDays: number; minRedeemPoints: number }) =>
    http.put<LoyaltySettings>('/loyalty/settings', body),
  rules: () => http.get<EarningRule[]>('/loyalty/rules'),
  createRule: (body: RuleInput) => http.post<EarningRule>('/loyalty/rules', body),
  updateRule: (id: string, body: RuleInput) => http.put<EarningRule>(`/loyalty/rules/${id}`, body),
  deleteRule: (id: string) => http.del<void>(`/loyalty/rules/${id}`),
  tiers: () => http.get<LoyaltyTier[]>('/loyalty/tiers'),
  updateTier: (code: string, body: { name: string; color: string; minAnnualSpendXaf: number; multiplierPct: number; benefits?: string; active: boolean }) =>
    http.put<LoyaltyTier[]>(`/loyalty/tiers/${code}`, body),
  rewards: () => http.get<Reward[]>('/loyalty/rewards'),
  createReward: (body: RewardInput) => http.post<Reward>('/loyalty/rewards', body),
  updateReward: (id: string, body: RewardInput) => http.put<Reward>(`/loyalty/rewards/${id}`, body),
  redemptions: (f: PageParams & { status?: string; from?: string; to?: string } = {}) => http.get<Page<Redemption>>(`/loyalty/redemptions${qs({ ...f })}`),
  redeem: (body: { customerId: string; rewardId: string; operationId?: string | null; customerConfirmed: boolean }) => http.post<Redemption>('/loyalty/redemptions', body),
  reverseRedemption: (id: string, reason: string) => http.post<Redemption>(`/loyalty/redemptions/${id}/reverse`, { reason }),
  adjustments: (f: PageParams & { reason?: string; from?: string; to?: string } = {}) =>
    http.get<Page<PointsAdjustment, { adjustments: number; added: number; deducted: number; members: number }>>(`/loyalty/adjustments${qs({ ...f })}`),
  adjust: (body: { customerId: string; points: number; reason: string; note: string }) => http.post<PointsAdjustment>('/loyalty/adjustments', body),
  expire: () => http.post<{ expiredPoints: number }>('/loyalty/expire'),
};

export const commissionsApi = {
  rules: (sellerRole?: SellerRole) => http.get<CommissionRule[]>(`/commissions/rules${qs({ sellerRole })}`),
  saveRule: (body: { productId: string; sellerRole: SellerRole; basis: CommissionBasis; rate: number; note?: string }) => http.post<CommissionRule>('/commissions/rules', body),
  endRule: (id: string) => http.del<void>(`/commissions/rules/${id}`),
  history: (productId: string, sellerRole: SellerRole, f: PageParams = {}) =>
    http.get<Page<CommissionRule>>(`/commissions/rules/history${qs({ productId, sellerRole, ...f })}`),
  lines: (f: PageParams & { sellerId?: string; from?: string; to?: string } = {}) =>
    http.get<Page<CommissionLine, { line_count: number; commission_xaf: number; sales_xaf: number }>>(`/commissions/lines${qs({ ...f })}`),
};

export const targetsApi = {
  list: (f: PageParams & { sellerId?: string; status?: string; state?: string } = {}) => http.get<Page<SellerTarget>>(`/targets${qs({ ...f })}`),
  get: (id: string) => http.get<SellerTarget>(`/targets/${id}`),
  create: (body: TargetInput) => http.post<SellerTarget>('/targets', body),
  cancel: (id: string, reason: string) => http.post<SellerTarget>(`/targets/${id}/cancel`, { reason }),
};

export const campaignsApi = {
  list: (f: PageParams & { state?: string } = {}) => http.get<Page<Campaign>>(`/campaigns${qs({ ...f })}`),
  get: (id: string) => http.get<Campaign>(`/campaigns/${id}`),
  standings: (id: string, f: PageParams = {}) => http.get<Page<CampaignStanding>>(`/campaigns/${id}/standings${qs({ ...f })}`),
  create: (body: CampaignInput) => http.post<Campaign>('/campaigns', body),
  cancel: (id: string) => http.post<Campaign>(`/campaigns/${id}/cancel`),
  settle: (id: string) => http.post<Campaign>(`/campaigns/${id}/settle`),
};

export const ordersApi = {
  list: (f: PageParams & { group?: string; status?: string; operationId?: string } = {}) =>
    http.get<Page<CustomerOrder, { placed: number; accepted: number; ready: number }>>(`/orders${qs({ ...f })}`),
  get: (id: string) => http.get<CustomerOrder>(`/orders/${id}`),
  cancel: (id: string, reason: string) => http.post<CustomerOrder>(`/orders/${id}/cancel`, { reason }),
};

export const earningsApi = {
  overview: () => http.get<EarningsOverview>('/earnings/overview'),
  periods: (f: PageParams = {}) => http.get<Page<EarningPeriod>>(`/earnings/periods${qs({ ...f })}`),
  period: (id: string) => http.get<EarningPeriodDetail>(`/earnings/periods/${id}`),
  periodSellers: (id: string, f: PageParams = {}) => http.get<Page<SellerStatement>>(`/earnings/periods/${id}/sellers${qs({ ...f })}`),
  finalize: (id: string) => http.post<EarningPeriodDetail>(`/earnings/periods/${id}/finalize`),
  seller: (id: string) => http.get<{ period: EarningPeriodDetail; current: Partial<SellerStatement> }>(`/earnings/sellers/${id}`),
  statements: (id: string, f: PageParams = {}) => http.get<Page<SellerStatement>>(`/earnings/sellers/${id}/statements${qs({ ...f })}`),
  adjustments: (f: PageParams & { sellerId?: string; type?: EarningAdjustmentType | ''; from?: string; to?: string } = {}) =>
    http.get<Page<EarningAdjustment, { adjustments: number; amount_xaf: number }>>(`/earnings/adjustments${qs({ ...f })}`),
  adjust: (body: { sellerId: string; amountXaf: number; note: string }) => http.post<EarningAdjustment>('/earnings/adjustments', body),
};

export const payoutsApi = {
  overview: () => http.get<PayoutOverview>('/payouts/overview'),
  settings: () => http.get<{ approvalThresholdXaf: number }>('/payouts/settings'),
  setThreshold: (approvalThresholdXaf: number) => http.put<{ approvalThresholdXaf: number }>('/payouts/settings', { approvalThresholdXaf }),
  balances: ({ owedOnly, ...f }: PageParams & { owedOnly?: boolean } = {}) =>
    http.get<Page<SellerBalance, { owed_xaf: number; paid_xaf: number; pending_xaf: number; outstanding_xaf: number; sellers_with_balance: number }>>(`/payouts/balances${qs({ ...f, owedOnly: owedOnly ? 'true' : undefined })}`),
  balance: (sellerId: string) => http.get<SellerBalance>(`/payouts/balances/${sellerId}`),
  periods: (sellerId: string, { openOnly, ...f }: PageParams & { openOnly?: boolean } = {}) =>
    http.get<Page<PeriodBalance>>(`/payouts/balances/${sellerId}/periods${qs({ ...f, openOnly: openOnly ? 'true' : undefined })}`),
  list: (f: PayoutFilter & PageParams = {}) =>
    http.get<Page<Payout, { payouts: number; paid_xaf: number; paid: number; pending: number; rejected: number }>>(`/payouts${qs({ ...f })}`),
  get: (id: string) => http.get<PayoutDetail>(`/payouts/${id}`),
  record: (body: PayoutInput) => http.post<PayoutDetail>('/payouts', body),
  approve: (id: string, note?: string) => http.post<PayoutDetail>(`/payouts/${id}/approve`, { note }),
  reject: (id: string, reason: string) => http.post<PayoutDetail>(`/payouts/${id}/reject`, { reason }),
  cancel: (id: string, note?: string) => http.post<PayoutDetail>(`/payouts/${id}/cancel`, { note }),
  pay: (id: string, body: { providerReference?: string; note?: string }) => http.post<PayoutDetail>(`/payouts/${id}/pay`, body),
  requestReversal: (id: string, reason: string) => http.post<PayoutDetail>(`/payouts/${id}/reversal`, { reason }),
  approveReversal: (id: string, note?: string) => http.post<PayoutDetail>(`/payouts/${id}/reversal/approve`, { note }),
  rejectReversal: (id: string, reason: string) => http.post<PayoutDetail>(`/payouts/${id}/reversal/reject`, { reason }),
  addEvidence: (id: string, file: File) => http.upload<PayoutDetail>(`/payouts/${id}/evidence`, file),
  evidenceFile: (id: string, evidenceId: string) => http.blob(`/payouts/${id}/evidence/${evidenceId}`),
  destinations: (f: PageParams & { status?: string; provider?: string; sellerId?: string } = {}) =>
    http.get<Page<PayoutDestination, { numbers: number; pending: number; verified: number }>>(`/payouts/destinations${qs({ ...f })}`),
  addDestination: (body: DestinationInput) => http.post<PayoutDestination & { code_sent: boolean; code_error?: string }>('/payouts/destinations', body),
  verifyDestination: (id: string, code: string) => http.post<PayoutDestination>(`/payouts/destinations/${id}/verify`, { code }),
  resendCode: (id: string) => http.post<{ status: string }>(`/payouts/destinations/${id}/resend-code`),
  rejectDestination: (id: string, reason: string) => http.post<PayoutDestination>(`/payouts/destinations/${id}/reject`, { reason }),
};

export const INBOX_CHANGED = 'lutiano:inbox-changed';

export const businessApi = {
  get: () => http.get<BusinessInfo>('/settings/business'),
  update: (body: Omit<BusinessInfo, 'updated_at'>) => http.put<BusinessInfo>('/settings/business', body),
  security: () => http.get<{ mfaEnabled: boolean; codeLength: number; codeMinutes: number; maxAttempts: number; sessionHours: number }>('/settings/business/security'),
};

export const reportsApi = {
  overview: (f: { from?: string; to?: string }) => http.get<ReportOverview>(`/reports/overview${qs({ ...f })}`),
  sales: (f: { groupBy?: string; from?: string; to?: string; operationId?: string; channel?: string; sellerId?: string }) => http.get<SalesReport>(`/reports/sales${qs({ ...f })}`),
  builder: (f: { metrics: string[]; groupBy: string; from?: string; to?: string }) => http.get<BuilderReport>(`/reports/builder${qs({ ...f, metrics: f.metrics.join(',') })}`),
};

export const notificationsApi = {
  deliveries: (f: PageParams & { status?: string; event?: string; from?: string; to?: string } = {}) =>
    http.get<Page<NotificationDelivery, { messages: number; sent: number; failed: number; skipped: number; queued: number }>>(`/notifications/deliveries${qs({ ...f })}`),
  resend: (id: string) => http.post<NotificationDelivery>(`/notifications/deliveries/${id}/resend`),
  events: (f: PageParams = {}) => http.get<Page<NotificationEvent>>(`/notifications/events${qs({ ...f })}`),
  allEvents: () => fetchAll((page, size) => notificationsApi.events({ page, size })),
  saveEvent: (key: string, body: { smsEnabled: boolean; fr: string; en: string }) => http.put<Status>(`/notifications/events/${key}`, body),
  mine: (f: PageParams & { topic?: string } = {}) => http.get<Page<InboxMessage, { unread: number; topics: Record<string, number> }>>(`/notifications/mine${qs({ ...f })}`),
  /** Marks one message (or all) as read, then tells the bell to refresh its count. */
  markRead: async (id?: string) => {
    const r = await http.post<Status>('/notifications/mine/read', { id });
    window.dispatchEvent(new Event(INBOX_CHANGED));
    return r;
  },
  settings: () => http.get<NotificationSettings>('/notifications/settings'),
  saveApprovers: (staffIds: string[]) => http.put<NotificationSettings>('/notifications/settings/approvers', { staffIds }),
};

/** Announcements to sellers (shown in the Seller app). */
export const announcementsApi = {
  list: (f: PageParams = {}) => http.get<Page<Announcement>>(`/announcements${qs({ ...f })}`),
  send: (body: AnnouncementInput) => http.post<Announcement>('/announcements', body),
};

/** Help requests from sellers (the Support tab of Notifications). */
export const supportApi = {
  list: (f: PageParams & { status?: SupportRequest['status'] | ''; requester?: string } = {}) =>
    http.get<Page<SupportRequest, { open_count: number; in_progress: number; resolved: number }>>(`/support${qs({ ...f })}`),
  reply: (id: string, body: { status?: SupportRequest['status']; reply?: string }) => http.post<SupportRequest>(`/support/${id}/reply`, body),
  attachment: (id: string) => http.blob(`/support/${id}/attachment`),
};

export const searchApi = {
  /** Verified results, most relevant first. Throws a 503 ApiError when search is unavailable. */
  search: (q: string, types: string[] | undefined, page = 1, size = 10) =>
    http.post<Page<SearchHit, SearchSummary>>(`/search${qs({ page, size })}`, { q, types }),
  suggestions: (q: string) => http.get<SearchHit[]>(`/search/suggestions${qs({ q })}`),
};
