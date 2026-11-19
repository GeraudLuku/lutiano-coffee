import type { DocumentType, OperationType, Role, SellerRole } from '../types/models';

export const ROLE_LABEL: Record<Role, string> = { ADMIN: 'Administrator', INVENTORY_MANAGER: 'Inventory Manager', SELLER: 'Seller' };
export const ROLE_DESCRIPTION: Record<Role, string> = {
  ADMIN: 'Full configuration and user management',
  INVENTORY_MANAGER: 'Stock receipts, allocations and reviews',
  SELLER: 'Sells at assigned locations via the Seller app',
};
export const TYPE_LABEL: Record<OperationType, string> = { FIXED_POS: 'Fixed POS', MOBILE: 'Mobile Barista', MARKET_AGENT: 'Market Agent' };
export const TYPE_DESCRIPTION: Record<OperationType, string> = {
  FIXED_POS: 'Physical point of sale (e.g. shop, kiosk)',
  MOBILE: 'Mobile coffee sales (events, offices, campuses)',
  MARKET_AGENT: 'Market-based direct sales',
};
export const CHANNEL_COLOR: Record<OperationType, string> = { FIXED_POS: '#1F4FD1', MOBILE: '#1FA97A', MARKET_AGENT: '#F29A1F' };
/** Prefix of the generated location code, e.g. FP-001. Mirrors the server. */
export const CODE_PREFIX: Record<OperationType, string> = { FIXED_POS: 'FP', MOBILE: 'MB', MARKET_AGENT: 'MA' };

/** Audit entity types -> module names and colours used in activity feeds. */
export const MODULE: Record<string, [string, string]> = {
  sale: ['Sales', 'green'], product: ['Products', 'blue'], allocation: ['Inventory', 'purple'],
  operation: ['Locations', 'gold'], staff_user: ['Users', 'grey'], cash_shift: ['Cash', 'amber'],
};

/** What each role can do, mirroring the server-side checks. */
export const PERMISSIONS: [string, Role[]][] = [
  ['View dashboard and reports', ['ADMIN', 'INVENTORY_MANAGER']],
  ['Manage users, roles and access', ['ADMIN']],
  ['Manage sales locations and QR codes', ['ADMIN']],
  ['Manage products and prices', ['ADMIN']],
  ['Record central stock receipts', ['ADMIN', 'INVENTORY_MANAGER']],
  ['Allocate stock to sales locations', ['ADMIN', 'INVENTORY_MANAGER']],
  ['Review discrepancies and cash variances', ['ADMIN', 'INVENTORY_MANAGER']],
  ['View all sales transactions', ['ADMIN', 'INVENTORY_MANAGER']],
  ['View the audit log', ['ADMIN', 'INVENTORY_MANAGER']],
  ['Record cash sales at assigned locations', ['SELLER']],
  ['Open and close own cash shift', ['SELLER']],
  ['View assigned stock and confirm receipts', ['SELLER']],
  ['Show assigned location QR code', ['SELLER']],
  ['View own sales history', ['SELLER']],
];

/** The seller roles (design: "One app for all three seller roles") and the channel each one works. */
export const SELLER_ROLE_LABEL: Record<SellerRole, string> = { FIXED_POS_BARISTA: 'Fixed POS Barista', MOBILE_BARISTA: 'Mobile Barista', MARKET_AGENT: 'Market Sales Agent' };
export const SELLER_ROLE_DESCRIPTION: Record<SellerRole, string> = {
  FIXED_POS_BARISTA: 'In-store sales at a fixed point of sale',
  MOBILE_BARISTA: 'Mobile coffee sales (events, offices, pop-ups)',
  MARKET_AGENT: 'Direct sales in markets and the field',
};
export const SELLER_ROLE_CHANNEL: Record<SellerRole, OperationType> = { FIXED_POS_BARISTA: 'FIXED_POS', MOBILE_BARISTA: 'MOBILE', MARKET_AGENT: 'MARKET_AGENT' };
export const DOCUMENT_TYPE_LABEL: Record<DocumentType, string> = { NATIONAL_ID: 'National ID', CONTRACT: 'Signed contract', HEALTH_CERTIFICATE: 'Health certificate', OTHER: 'Other document' };
