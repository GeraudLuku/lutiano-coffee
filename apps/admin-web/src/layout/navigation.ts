import { Award, BadgePercent, BarChart3, Target, HandCoins, UserRound, Boxes, CreditCard, LayoutDashboard, Package, Settings, ShoppingCart, Store, Users, type LucideIcon } from 'lucide-react';
import type { Role } from '../types/models';

export interface NavChild { to: string; label: string; roles?: Role[] }
export interface NavItem { to: string; label: string; icon: LucideIcon; roles: Role[]; children?: NavChild[] }

const STAFF: Role[] = ['ADMIN', 'INVENTORY_MANAGER'];

/** Sidebar entries in design order; only modules that exist are listed. */
export const NAVIGATION: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: STAFF },
  { to: '/users', label: 'Users', icon: Users, roles: ['ADMIN'], children: [{ to: '/users', label: 'All Users' }, { to: '/users/sellers', label: 'Sellers' }, { to: '/users/new', label: 'Add User' }] },
  { to: '/locations', label: 'Sales Locations', icon: Store, roles: STAFF, children: [{ to: '/locations', label: 'All Locations' }, { to: '/locations/new', label: 'Add Location', roles: ['ADMIN'] }, { to: '/locations/qr', label: 'QR Codes', roles: ['ADMIN'] }] },
  { to: '/products', label: 'Products', icon: Package, roles: STAFF, children: [
    { to: '/products', label: 'Product Catalog' }, { to: '/products/new', label: 'Add Product', roles: ['ADMIN'] },
    { to: '/products/categories', label: 'Categories' }, { to: '/products/addons', label: 'Add-ons' },
  ] },
  { to: '/inventory', label: 'Inventory', icon: Boxes, roles: STAFF },
  { to: '/sales', label: 'Sales', icon: ShoppingCart, roles: STAFF, children: [
    { to: '/sales', label: 'Transactions' }, { to: '/sales/shifts', label: 'Cash Management' }, { to: '/sales/orders', label: 'Customer Orders', roles: ['ADMIN'] }, { to: '/sales/refunds', label: 'Refunds', roles: ['ADMIN'] },
  ] },
  { to: '/payments', label: 'Payments', icon: CreditCard, roles: ['ADMIN'], children: [
    { to: '/payments', label: 'All Payments' }, { to: '/payments/reconciliation', label: 'Reconciliation' }, { to: '/payments/methods', label: 'Payment Methods' },
  ] },
  { to: '/customers', label: 'Customers', icon: UserRound, roles: ['ADMIN'] },
  { to: '/loyalty', label: 'Loyalty', icon: Award, roles: ['ADMIN'] },
  { to: '/commissions', label: 'Commissions', icon: BadgePercent, roles: ['ADMIN'] },
  { to: '/targets', label: 'Targets & Bonuses', icon: Target, roles: ['ADMIN'] },
  { to: '/payouts', label: 'Payouts', icon: HandCoins, roles: ['ADMIN'] },
  { to: '/reports', label: 'Reports', icon: BarChart3, roles: ['ADMIN'] },
  { to: '/settings', label: 'Settings', icon: Settings, roles: STAFF },
];
