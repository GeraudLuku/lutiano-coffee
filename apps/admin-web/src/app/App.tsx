import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { AppLayout } from '../layout/AppLayout';
import { LoginPage } from '../pages/auth/LoginPage';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { UsersListPage } from '../pages/users/UsersListPage';
import { UserFormPage } from '../pages/users/UserFormPage';
import { UserDetailPage } from '../pages/users/UserDetailPage';
import { SellersListPage } from '../pages/users/SellersListPage';
import { LocationsListPage } from '../pages/locations/LocationsListPage';
import { QrCodesPage } from '../pages/locations/QrCodesPage';
import { LocationFormPage } from '../pages/locations/LocationFormPage';
import { LocationDetailPage } from '../pages/locations/LocationDetailPage';
import { ProductsListPage } from '../pages/products/ProductsListPage';
import { ProductFormPage } from '../pages/products/ProductFormPage';
import { ProductDetailPage } from '../pages/products/ProductDetailPage';
import { CategoriesPage } from '../pages/products/CategoriesPage';
import { AddonsPage } from '../pages/products/AddonsPage';
import { InventoryPage } from '../pages/inventory/InventoryPage';
import { StocktakePage } from '../pages/inventory/StocktakePage';
import { SalesListPage } from '../pages/sales/SalesListPage';
import { OrdersPage } from '../pages/sales/OrdersPage';
import { SaleDetailPage } from '../pages/sales/SaleDetailPage';
import { CashManagementPage } from '../pages/cash/CashManagementPage';
import { ShiftDetailPage } from '../pages/cash/ShiftDetailPage';
import { SettingsPage } from '../pages/settings/SettingsPage';
import { PaymentsListPage } from '../pages/payments/PaymentsListPage';
import { PaymentDetailPage } from '../pages/payments/PaymentDetailPage';
import { ReconciliationPage } from '../pages/payments/ReconciliationPage';
import { PaymentMethodsPage } from '../pages/payments/PaymentMethodsPage';
import { RefundsListPage } from '../pages/refunds/RefundsListPage';
import { LoyaltyPage } from '../pages/loyalty/LoyaltyPage';
import { CommissionsPage } from '../pages/commissions/CommissionsPage';
import { TargetsPage } from '../pages/targets/TargetsPage';
import { RecordSalePage } from '../pages/sales/RecordSalePage';
import { ReportsPage } from '../pages/reports/ReportsPage';
import { CustomersPage } from '../pages/customers/CustomersPage';
import { NotificationsPage } from '../pages/notifications/NotificationsPage';
import { PayoutsPage } from '../pages/payouts/PayoutsPage';
import { PayoutDetailPage } from '../pages/payouts/PayoutDetailPage';
import { PeriodPage } from '../pages/commissions/PeriodPage';
import { MemberDetailPage } from '../pages/loyalty/MemberDetailPage';
import { SearchResultsPage } from '../pages/search/SearchResultsPage';
import { RefundDetailPage } from '../pages/refunds/RefundDetailPage';

export function App() {
  const { session, can } = useAuth();
  if (!session) return <LoginPage />;
  const admin = can('ADMIN');
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        {admin && (
          <>
            <Route path="users" element={<UsersListPage />} />
            <Route path="users/new" element={<UserFormPage />} />
            <Route path="users/sellers" element={<SellersListPage />} />
            <Route path="users/:id" element={<UserDetailPage />} />
            <Route path="users/:id/edit" element={<UserFormPage />} />
            <Route path="locations/new" element={<LocationFormPage />} />
            <Route path="locations/qr" element={<QrCodesPage />} />
            <Route path="locations/:id/edit" element={<LocationFormPage />} />
            <Route path="products/new" element={<ProductFormPage />} />
            <Route path="products/:id/edit" element={<ProductFormPage />} />
            <Route path="payments" element={<PaymentsListPage />} />
            <Route path="payments/reconciliation" element={<ReconciliationPage />} />
            <Route path="payments/methods" element={<PaymentMethodsPage />} />
            <Route path="payments/:id" element={<PaymentDetailPage />} />
            <Route path="sales/orders" element={<OrdersPage />} />
            <Route path="sales/refunds" element={<RefundsListPage />} />
            <Route path="sales/record" element={<RecordSalePage />} />
            <Route path="customers" element={<CustomersPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="loyalty" element={<LoyaltyPage />} />
            <Route path="commissions" element={<CommissionsPage />} />
            <Route path="targets" element={<TargetsPage />} />
            <Route path="payouts" element={<PayoutsPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="payouts/:id" element={<PayoutDetailPage />} />
            <Route path="commissions/periods/:id" element={<PeriodPage />} />
            <Route path="loyalty/members/:id" element={<MemberDetailPage />} />
            <Route path="sales/refunds/:id" element={<RefundDetailPage />} />
          </>
        )}
        <Route path="locations" element={<LocationsListPage />} />
        <Route path="locations/:id" element={<LocationDetailPage />} />
        <Route path="products" element={<ProductsListPage />} />
        <Route path="products/categories" element={<CategoriesPage />} />
        <Route path="products/addons" element={<AddonsPage />} />
        <Route path="products/:id" element={<ProductDetailPage />} />
        <Route path="inventory" element={<InventoryPage />} />
        <Route path="inventory/stocktakes/:id" element={<StocktakePage />} />
        <Route path="sales" element={<SalesListPage />} />
        <Route path="sales/shifts" element={<CashManagementPage />} />
        <Route path="sales/shifts/:id" element={<ShiftDetailPage />} />
        <Route path="sales/:id" element={<SaleDetailPage />} />
        <Route path="search" element={<SearchResultsPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
