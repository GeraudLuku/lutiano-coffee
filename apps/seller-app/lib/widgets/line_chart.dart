import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';

/// One line of a [MiniLineChart]: a name, a colour and a value per label.
class LineSeries {
  const LineSeries(this.name, this.color, this.values);
  final String name;
  final Color color;
  final List<int> values;
}

/// A small line chart over labelled points (one value per label for every series), with a legend.
class MiniLineChart extends StatelessWidget {
  const MiniLineChart(
      {super.key,
      required this.labels,
      required this.series,
      this.height = 150});
  final List<String> labels;
  final List<LineSeries> series;
  final double height;

  @override
  Widget build(BuildContext context) => Semantics(
        label: [for (final s in series) '${s.name}: ${s.values.join(', ')}']
            .join('; '),
        child: Column(children: [
          Wrap(spacing: 16, children: [
            for (final s in series)
              Row(mainAxisSize: MainAxisSize.min, children: [
                Container(
                    width: 10,
                    height: 10,
                    decoration:
                        BoxDecoration(color: s.color, shape: BoxShape.circle)),
                const SizedBox(width: 6),
                Text(s.name,
                    style:
                        const TextStyle(fontSize: 12, color: AppColors.muted)),
              ]),
          ]),
          const SizedBox(height: 8),
          SizedBox(
              height: height,
              width: double.infinity,
              child: CustomPaint(painter: _LinePainter(series))),
          const SizedBox(height: 6),
          Row(children: [
            for (final l in labels)
              Expanded(
                  child: Text(l,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                          fontSize: 11, color: AppColors.muted))),
          ]),
        ]),
      );
}

class _LinePainter extends CustomPainter {
  _LinePainter(this.series);
  final List<LineSeries> series;

  @override
  void paint(Canvas canvas, Size size) {
    final grid = Paint()
      ..color = AppColors.line
      ..strokeWidth = 1;
    for (var i = 0; i <= 3; i++) {
      final y = size.height * i / 3;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), grid);
    }
    final all = series.expand((s) => s.values);
    if (all.isEmpty) return;
    final max = all.reduce((a, b) => a > b ? a : b);
    final min = all.reduce((a, b) => a < b ? a : b);
    final lo = min < 0 ? min : 0;
    final span = (max - lo) == 0 ? 1 : (max - lo);
    for (final s in series) {
      final n = s.values.length;
      if (n == 0) continue;
      Offset at(int i) => Offset(size.width * (i + 0.5) / n,
          size.height - size.height * (s.values[i] - lo) / span);
      final line = Paint()
        ..color = s.color
        ..strokeWidth = 2.5
        ..style = PaintingStyle.stroke;
      final path = Path()..moveTo(at(0).dx, at(0).dy);
      for (var i = 1; i < n; i++) {
        path.lineTo(at(i).dx, at(i).dy);
      }
      canvas.drawPath(path, line);
      for (var i = 0; i < n; i++) {
        canvas.drawCircle(at(i), 3.5, Paint()..color = s.color);
      }
    }
  }

  @override
  bool shouldRepaint(_LinePainter old) => old.series != series;
}
