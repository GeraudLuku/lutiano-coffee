import 'package:material_ui/material_ui.dart';

import '../earnings/earnings_screen.dart';
import '../history/sales_history_screen.dart';
import '../home/home_screen.dart';
import '../inventory/inventory_screen.dart';
import '../profile/profile_screen.dart';
import '../../core/l10n.dart';

/// Bottom navigation shared by all three seller roles (design: Home · Sales · Inventory · Earnings · Profile).
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  /// Lets nested screens switch tabs (e.g. "Sales History" quick action).
  static void goTo(BuildContext context, int tab) =>
      context.findAncestorStateOfType<_MainShellState>()?.select(tab);

  static const home = 0, sales = 1, inventory = 2, earnings = 3, profile = 4;

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _tab = MainShell.home;

  void select(int tab) => setState(() => _tab = tab);

  @override
  Widget build(BuildContext context) => Scaffold(
        body: IndexedStack(index: _tab, children: const [
          HomeScreen(),
          SalesHistoryScreen(),
          InventoryScreen(),
          EarningsScreen(),
          ProfileScreen()
        ]),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _tab,
          onDestinationSelected: select,
          destinations: [
            NavigationDestination(
                icon: Icon(Icons.home_outlined),
                selectedIcon: Icon(Icons.home),
                label: context.l10n.home),
            NavigationDestination(
                icon: Icon(Icons.receipt_long_outlined),
                selectedIcon: Icon(Icons.receipt_long),
                label: context.l10n.sales),
            NavigationDestination(
                icon: Icon(Icons.inventory_2_outlined),
                selectedIcon: Icon(Icons.inventory_2),
                label: context.l10n.inventory),
            NavigationDestination(
                icon: Icon(Icons.savings_outlined),
                selectedIcon: Icon(Icons.savings),
                label: context.l10n.earnings),
            NavigationDestination(
                icon: Icon(Icons.person_outline),
                selectedIcon: Icon(Icons.person),
                label: context.l10n.profile),
          ],
        ),
      );
}
