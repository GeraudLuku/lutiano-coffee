import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../cart/cart_screen.dart';
import '../menu/menu_screen.dart';
import '../notifications/notifications_screen.dart';
import '../menu/menu_widgets.dart';
import '../menu/product_screen.dart';
import '../scan/sales_point_banner.dart';
import '../scan/sales_point_screen.dart';
import '../scan/sales_points_screen.dart';
import '../scan/scan_flow.dart';
import '../shell/main_shell.dart';

/// Home: a greeting, scan to order, the sales points visited lately, categories and popular products.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final me = app.customer!;
    final recent = app.settings.recentSalesPoints;
    return Scaffold(
      body: SafeArea(
        child: AsyncView<
            ({List<MenuCategory> categories, List<MenuItem> popular})>(
          key: ValueKey('home-${me.id}'),
          load: () async {
            final results = await Future.wait(
                [app.catalogue.categories(), app.catalogue.products(size: 6)]);
            return (
              categories: results[0] as List<MenuCategory>,
              popular: (results[1] as Paged<MenuItem>).items
            );
          },
          builder: (context, data, _) =>
              ListView(padding: const EdgeInsets.all(16), children: [
            Row(children: [
              CircleAvatar(
                  radius: 22,
                  backgroundColor: AppColors.gold,
                  child: Text(
                      me.firstName.isEmpty
                          ? '?'
                          : me.firstName[0].toUpperCase(),
                      style: const TextStyle(
                          color: AppColors.navy,
                          fontWeight: FontWeight.w800,
                          fontSize: 18))),
              const SizedBox(width: 12),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(l.hello(me.firstName),
                        style: const TextStyle(
                            fontSize: 20, fontWeight: FontWeight.w800)),
                    Text(l.coffeeLover,
                        style: const TextStyle(color: AppColors.muted)),
                  ])),
              IconButton(
                  tooltip: l.notifications,
                  onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => const NotificationsScreen())),
                  icon: Badge(
                      isLabelVisible: app.unreadNotifications > 0,
                      label: Text('${app.unreadNotifications}'),
                      child: const Icon(Icons.notifications_none))),
              IconButton(
                  tooltip: l.cart,
                  onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(builder: (_) => const CartScreen())),
                  icon: Badge(
                      isLabelVisible: app.cart.count > 0,
                      label: Text('${app.cart.count}'),
                      child: const Icon(Icons.shopping_bag_outlined))),
            ]),
            const SizedBox(height: 16),
            const SalesPointBanner(),
            const SizedBox(height: 12),
            AppCard(
              color: AppColors.navy,
              onTap: () => scanSalesPoint(context),
              child: Row(children: [
                const Icon(Icons.qr_code_scanner,
                    color: AppColors.gold, size: 34),
                const SizedBox(width: 14),
                Expanded(
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                      Text(l.scanToOrder,
                          style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w800,
                              fontSize: 17)),
                      Text(l.scanToOrderHint,
                          style: const TextStyle(color: AppColors.white70)),
                    ])),
                const Icon(Icons.chevron_right, color: Colors.white),
              ]),
            ),
            const SizedBox(height: 8),
            Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                    onPressed: () => Navigator.of(context).push(
                        MaterialPageRoute(
                            builder: (_) => const SalesPointsScreen())),
                    icon: const Icon(Icons.storefront_outlined),
                    label: Text(l.findASalesPoint))),
            if (recent.isNotEmpty) ...[
              SectionLabel(l.recentSalesPoints),
              for (final p in recent)
                MenuTile(
                    icon: Icons.history,
                    title: p['name'] ?? '',
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) =>
                            SalesPointScreen(salesPointId: p['id']!)))),
            ],
            if (data.categories.isNotEmpty) ...[
              SectionLabel(l.categories),
              SizedBox(
                height: 44,
                child: ListView(scrollDirection: Axis.horizontal, children: [
                  for (final c in data.categories)
                    Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: ActionChip(
                            label: Text(c.name),
                            onPressed: () {
                              Navigator.of(context).push(MaterialPageRoute(
                                  builder: (_) => MenuScreen(
                                      initialCategory: c.name,
                                      standalone: true)));
                            })),
                ]),
              ),
            ],
            if (data.popular.isNotEmpty) ...[
              SectionLabel(l.popularDrinks,
                  trailing: TextButton(
                      onPressed: () => MainShell.goTo(context, MainShell.menu),
                      child: Text(l.seeAll))),
              SizedBox(
                height: 190,
                child: ListView(scrollDirection: Axis.horizontal, children: [
                  for (final item in data.popular)
                    Padding(
                        padding: const EdgeInsets.only(right: 12),
                        child: SizedBox(
                            width: 150,
                            child: MenuCard(
                                item: item,
                                onTap: () => Navigator.of(context).push(
                                    MaterialPageRoute(
                                        builder: (_) =>
                                            ProductScreen(item: item))))))
                ]),
              ),
            ],
          ]),
        ),
      ),
    );
  }
}
