import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';
import 'skeleton.dart';

/// Turns the media paths the API returns into full addresses; an app puts one above its screens (see [MediaUrls.resolve]).
class MediaUrls extends InheritedWidget {
  const MediaUrls({super.key, required this.toUrl, required super.child});
  final String? Function(String? url) toUrl;

  static String? resolve(BuildContext context, String? url) =>
      context.getInheritedWidgetOfExactType<MediaUrls>()?.toUrl(url);

  @override
  bool updateShouldNotify(MediaUrls old) => toUrl != old.toUrl;
}

/// Product photo served by the API, with a coffee placeholder.
class ProductImage extends StatelessWidget {
  const ProductImage(
      {super.key, required this.url, this.size, this.radius = 10});
  final String? url;
  final double? size;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final resolved = MediaUrls.resolve(context, url);
    final placeholder = Container(
      color: AppColors.goldSoft,
      alignment: Alignment.center,
      child:
          Icon(Icons.coffee, color: AppColors.navy, size: (size ?? 64) * 0.45),
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: SizedBox(
        width: size,
        height: size,
        child: resolved == null
            ? placeholder
            : Image.network(resolved,
                fit: BoxFit.cover,
                // a plain grey block until the photo arrives
                loadingBuilder: (context, child, progress) => progress == null
                    ? child
                    : const SkeletonBox(height: null, radius: 0),
                errorBuilder: (_, __, ___) => placeholder),
      ),
    );
  }
}
