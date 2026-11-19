import 'package:material_ui/material_ui.dart';

import '../l10n/core_l10n.dart';
import '../core/theme.dart';
import 'cards.dart';

/// A shared pulse for every skeleton on screen, so they all breathe together (and stand still when the phone asks for less motion).
class SkeletonScope extends StatefulWidget {
  const SkeletonScope({super.key, required this.child, this.animate = true});
  final Widget child;

  /// False for placeholders that are not waiting on anything right now (they just stand there, without a running animation).
  final bool animate;

  @override
  State<SkeletonScope> createState() => _SkeletonScopeState();
}

class _SkeletonScopeState extends State<SkeletonScope>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(
      vsync: this, duration: const Duration(milliseconds: 1100));
  bool _started = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _sync();
  }

  @override
  void didUpdateWidget(SkeletonScope old) {
    super.didUpdateWidget(old);
    _sync();
  }

  void _sync() {
    final still = MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    final shouldRun = widget.animate && !still;
    if (shouldRun && !_started) {
      _pulse.repeat(reverse: true);
      _started = true;
    } else if (!shouldRun && _started) {
      _pulse.stop();
      _started = false;
    }
  }

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => _SkeletonPulse(
        animation: _pulse,
        child: Semantics(
            label: context.coreL10n.loading,
            excludeSemantics: true,
            child: widget.child),
      );
}

class _SkeletonPulse extends InheritedWidget {
  const _SkeletonPulse({required this.animation, required super.child});
  final Animation<double> animation;

  @override
  bool updateShouldNotify(_SkeletonPulse old) => animation != old.animation;
}

/// A grey rounded block standing in for text, an image or a number that is still loading.
class SkeletonBox extends StatelessWidget {
  const SkeletonBox({super.key, this.width, this.height = 14, this.radius = 6});
  final double? width, height;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final pulse =
        context.dependOnInheritedWidgetOfExactType<_SkeletonPulse>()?.animation;
    Widget box(double t) => Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
            color: Color.lerp(AppColors.line, AppColors.background, t * 0.6),
            borderRadius: BorderRadius.circular(radius),
          ),
        );
    return pulse == null
        ? box(0)
        : AnimatedBuilder(
            animation: pulse, builder: (_, __) => box(pulse.value));
  }
}

/// A list row: round picture, two lines of text and a number on the right.
class SkeletonTile extends StatelessWidget {
  const SkeletonTile({super.key, this.trailing = true});
  final bool trailing;

  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.symmetric(vertical: 8),
        child: Row(children: [
          SkeletonBox(width: 48, height: 48, radius: 24),
          SizedBox(width: 12),
          Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                SkeletonBox(width: 150, height: 14),
                SizedBox(height: 8),
                SkeletonBox(width: 100, height: 11),
              ])),
          SkeletonBox(width: 56, height: 16),
        ]),
      );
}

/// A card-shaped row for lists whose items are cards (sales, deliveries, shifts).
class SkeletonCard extends StatelessWidget {
  const SkeletonCard({super.key});

  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.only(bottom: 8),
        child: AppCard(child: SkeletonTile()),
      );
}

/// A row of summary figures (icon, number, label) as on the dashboard cards.
class SkeletonMetrics extends StatelessWidget {
  const SkeletonMetrics({super.key, this.count = 3});
  final int count;

  @override
  Widget build(BuildContext context) => Row(children: [
        for (var i = 0; i < count; i++)
          const Expanded(
            child: Row(children: [
              SkeletonBox(width: 36, height: 36, radius: 10),
              SizedBox(width: 8),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    SkeletonBox(width: 48, height: 16),
                    SizedBox(height: 6),
                    SkeletonBox(width: 36, height: 10),
                  ])),
            ]),
          ),
      ]);
}

/// A screen of card rows while the first page loads.
class SkeletonList extends StatelessWidget {
  const SkeletonList(
      {super.key,
      this.count = 6,
      this.cards = true,
      this.padding = const EdgeInsets.all(16)});
  final int count;
  final bool cards;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) => SkeletonScope(
        child: ListView(
          physics: const NeverScrollableScrollPhysics(),
          padding: padding,
          children: [
            for (var i = 0; i < count; i++)
              cards ? const SkeletonCard() : const SkeletonTile()
          ],
        ),
      );
}

/// A detail screen while it loads: a header card, then a few labelled rows.
class SkeletonDetail extends StatelessWidget {
  const SkeletonDetail({super.key});

  @override
  Widget build(BuildContext context) => SkeletonScope(
        child: ListView(
          physics: const NeverScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            AppCard(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  const SkeletonBox(width: 180, height: 20),
                  const SizedBox(height: 14),
                  for (var i = 0; i < 4; i++)
                    const Padding(
                        padding: EdgeInsets.symmetric(vertical: 6),
                        child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              SkeletonBox(width: 110, height: 12),
                              SkeletonBox(width: 80, height: 12)
                            ])),
                ])),
            const SizedBox(height: 12),
            const SkeletonCard(),
            const SkeletonCard(),
          ],
        ),
      );
}

/// A product tile (picture, name, price) for grids of products.
class SkeletonProductCard extends StatelessWidget {
  const SkeletonProductCard({super.key});

  @override
  Widget build(BuildContext context) => const AppCard(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
              child: SkeletonBox(
                  width: double.infinity, height: null, radius: 12)),
          SizedBox(height: 10),
          SkeletonBox(width: 110, height: 14),
          SizedBox(height: 8),
          SkeletonBox(width: 70, height: 12),
        ]),
      );
}
