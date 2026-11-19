import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';

/// A product as a card: photo, name and price (from the lowest size when sold by size).
class MenuCard extends StatelessWidget {
  const MenuCard({super.key, required this.item, required this.onTap});
  final MenuItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return AppCard(
      padding: EdgeInsets.zero,
      onTap: onTap,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Expanded(
            child: SizedBox(
                width: double.infinity,
                child: ProductImage(url: item.imageUrl, radius: 12))),
        Padding(
          padding: const EdgeInsets.all(10),
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(item.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontWeight: FontWeight.w800)),
            Text(
                item.hasSizes
                    ? l.fromPrice(xaf(item.priceXaf))
                    : xaf(item.priceXaf),
                style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
          ]),
        ),
      ]),
    );
  }
}

/// A product as a row of the menu list, with its heart.
class MenuRow extends StatelessWidget {
  const MenuRow({super.key, required this.item, required this.onTap});
  final MenuItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final app = AppScope.of(context);
    final fav = app.isFavourite(item.id);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Opacity(
        opacity: item.available ? 1 : 0.55,
        child: AppCard(
          onTap: onTap,
          child: Row(children: [
            ProductImage(url: item.imageUrl, size: 64),
            const SizedBox(width: 12),
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text(item.name,
                      style: const TextStyle(fontWeight: FontWeight.w800)),
                  if (item.description != null && item.description!.isNotEmpty)
                    Text(item.description!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  const SizedBox(height: 2),
                  Text(
                      item.hasSizes
                          ? l.fromPrice(xaf(item.priceXaf))
                          : xaf(item.priceXaf),
                      style: const TextStyle(fontWeight: FontWeight.w700)),
                  if (!item.available)
                    Text(l.unavailableHere,
                        style: const TextStyle(
                            color: AppColors.red, fontSize: 12)),
                ])),
            IconButton(
              tooltip: fav ? l.removeFromFavourites : l.addToFavourites,
              icon: Icon(fav ? Icons.favorite : Icons.favorite_border,
                  color: fav ? AppColors.red : AppColors.muted),
              onPressed: () => app.toggleFavourite(item.id).catchError((e) {
                if (context.mounted) {
                  showMessage(context, errorMessage(e), error: true);
                }
              }),
            ),
          ]),
        ),
      ),
    );
  }
}
