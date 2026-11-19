import { PageHeader } from '../../components/ui';
import { MembersTab } from '../loyalty/MembersTab';

/** Customers: everyone registered with the loyalty programme. Opening one shows their points, purchases and rewards. */
export function CustomersPage() {
  return (
    <>
      <PageHeader title="Customers" subtitle="Registered customers and loyalty members, with their points, tier and purchases." crumbs={[['Customers']]} />
      <MembersTab />
    </>
  );
}
