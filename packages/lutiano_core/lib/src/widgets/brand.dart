import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';

/// The LUTIANO mark (gold coffee bean), used on sign-in and About screens.
class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 56});
  final double size;

  @override
  Widget build(BuildContext context) => Image.asset('assets/bean.png',
      package: 'lutiano_core',
      width: size,
      height: size,
      excludeFromSemantics: true);
}

/// The wordmark with the app's line under it (e.g. "Coffee Business OS", "Coffee").
class BrandTitle extends StatelessWidget {
  const BrandTitle({super.key, required this.subtitle, this.light = true});
  final String subtitle;
  final bool light;

  @override
  Widget build(BuildContext context) {
    final c = light ? Colors.white : AppColors.navy;
    return Column(children: [
      Text('LUTIANO',
          style: TextStyle(
              color: c,
              fontSize: 28,
              fontWeight: FontWeight.w800,
              letterSpacing: 3)),
      Text(subtitle,
          style: TextStyle(color: light ? AppColors.white70 : AppColors.muted)),
    ]);
  }
}
