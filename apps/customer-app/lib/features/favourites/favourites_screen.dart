import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../menu/menu_widgets.dart';
import '../menu/product_screen.dart';

/// The products the customer marked with a heart.
class FavouritesScreen extends StatelessWidget {
  const FavouritesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.favourites)),
      body: PagedListView<MenuItem>(
        key: ValueKey('favourites-${app.favouriteCount}'),
        fetch: (page, size) => app.account.favourites(page: page, size: size),
        empty: EmptyState(
            icon: Icons.favorite_border,
            title: l.noFavourites,
            message: l.noFavouritesMessage),
        itemBuilder: (context, item, _, __) => MenuRow(
            item: item,
            onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => ProductScreen(item: item)))),
      ),
    );
  }
}
