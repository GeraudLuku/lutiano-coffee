import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';

class BarPoint {
  const BarPoint(this.label, this.value);
  final String label;
  final int value;
}

/// Simple vertical bar chart (design: context.l10n.salesTrend7Days2).
class MiniBarChart extends StatelessWidget {
  const MiniBarChart(
      {super.key,
      required this.points,
      this.height = 120,
      this.highlightLast = true});
  final List<BarPoint> points;
  final double height;
  final bool highlightLast;

  @override
  Widget build(BuildContext context) {
    final max = points.fold<int>(
        0,
        (m, p) => p.value > m
            ? p.value
            : m); // a negative value draws as an empty bar
    return SizedBox(
      height: height + 22,
      child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
        for (var i = 0; i < points.length; i++)
          Expanded(
            child: Semantics(
              label: '${points[i].label}: ${points[i].value}',
              child:
                  Column(mainAxisAlignment: MainAxisAlignment.end, children: [
                Container(
                  margin: const EdgeInsets.symmetric(horizontal: 5),
                  height: max == 0 || points[i].value <= 0
                      ? 3
                      : 3 + (height - 3) * points[i].value / max,
                  decoration: BoxDecoration(
                    color: highlightLast && i == points.length - 1
                        ? AppColors.gold
                        : const Color(0xFFFFD866),
                    borderRadius:
                        const BorderRadius.vertical(top: Radius.circular(5)),
                  ),
                ),
                const SizedBox(height: 6),
                Text(points[i].label,
                    style:
                        const TextStyle(fontSize: 11, color: AppColors.muted)),
              ]),
            ),
          ),
      ]),
    );
  }
}
