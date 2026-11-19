import { useNavigate } from 'react-router-dom';
import { Boxes, HandCoins, PlusCircle, ShoppingCart, Truck, Zap } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { Panel } from '../../components/ui';

export function QuickActions() {
  const { can } = useAuth();
  const nav = useNavigate();
  const admin = can('ADMIN');
  return (
    <Panel title="Quick Actions" icon={<Zap size={19} className="gold-ic" />}>
      <div className="quick">
        {admin ? <button className="q gold" onClick={() => nav('/sales/record')}><ShoppingCart /><span>Record Sale</span></button>
          : <button className="q gold" onClick={() => nav('/inventory?action=receipt')}><Boxes /><span>Record Stock</span></button>}
        <button className="q navy" onClick={() => nav('/inventory?action=transfer')}><Truck /><span>Allocate Stock</span></button>
        {admin
          ? <button className="q purple" onClick={() => nav('/payouts')}><HandCoins /><span>Create Payout</span></button>
          : <button className="q purple" onClick={() => nav('/sales')}><ShoppingCart /><span>View Sales</span></button>}
        {admin
          ? <button className="q green" onClick={() => nav('/products/new')}><PlusCircle /><span>Add Product</span></button>
          : <button className="q green" onClick={() => nav('/sales/shifts')}><ShoppingCart /><span>Cash Shifts</span></button>}
      </div>
    </Panel>
  );
}
