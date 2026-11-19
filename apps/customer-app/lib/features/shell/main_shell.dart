import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../home/home_screen.dart';
import '../menu/menu_screen.dart';
import '../orders/orders_screen.dart';
import '../rewards/rewards_screen.dart';
import '../profile/profile_screen.dart';

/// The signed-in app: Home, Menu, Orders, Rewards and Profile.
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  /// Switches tab from a nested screen.
  static void goTo(BuildContext context, int tab) =>
      context.findAncestorStateOfType<_MainShellState>()?._go(tab);

  static const home = 0, menu = 1, orders = 2, rewards = 3, profile = 4;

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _tab = 0;

  void _go(int tab) => setState(() => _tab = tab);

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final app = AppScope.of(context);
    final cart = app.cart.count;
    final pending = app.takePendingTab();
    if (pending != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _go(pending);
      });
    }
    return Scaffold(
      body: IndexedStack(index: _tab, children: [
        const HomeScreen(),
        const MenuScreen(),
        const OrdersScreen(),
        const RewardsScreen(),
        const ProfileScreen(),
      ]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: _go,
        destinations: [
          NavigationDestination(
              icon: const Icon(Icons.home_outlined),
              selectedIcon: const Icon(Icons.home),
              label: l.home),
          NavigationDestination(
              icon: Badge(
                  isLabelVisible: cart > 0,
                  label: Text('$cart'),
                  child: const Icon(Icons.local_cafe_outlined)),
              selectedIcon: const Icon(Icons.local_cafe),
              label: l.menu),
          NavigationDestination(
              icon: const Icon(Icons.receipt_long_outlined),
              selectedIcon: const Icon(Icons.receipt_long),
              label: l.orders),
          NavigationDestination(
              icon: const Icon(Icons.star_outline),
              selectedIcon: const Icon(Icons.star),
              label: l.rewards),
          NavigationDestination(
              icon: const Icon(Icons.person_outline),
              selectedIcon: const Icon(Icons.person),
              label: l.profile),
        ],
      ),
    );
  }
}
