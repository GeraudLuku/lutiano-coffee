import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';

/// White rounded card with consistent padding.
class AppCard extends StatelessWidget {
  const AppCard(
      {super.key,
      required this.child,
      this.padding = const EdgeInsets.all(16),
      this.onTap,
      this.color});
  final Widget child;
  final EdgeInsets padding;
  final VoidCallback? onTap;
  final Color? color;

  @override
  Widget build(BuildContext context) => Card(
        color: color,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
            onTap: onTap, child: Padding(padding: padding, child: child)),
      );
}

/// Small uppercase heading used between sections ("QUICK ACTIONS").
class SectionLabel extends StatelessWidget {
  const SectionLabel(this.text, {super.key, this.trailing});
  final String text;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 20, bottom: 10),
        child: Row(children: [
          Expanded(
              child: Text(text.toUpperCase(),
                  style: const TextStyle(
                      color: AppColors.muted,
                      fontWeight: FontWeight.w800,
                      fontSize: 11.5,
                      letterSpacing: 1))),
          if (trailing != null) trailing!,
        ]),
      );
}

/// Icon + label + value tile (design: "Today's Performance").
class MetricTile extends StatelessWidget {
  const MetricTile(
      {super.key,
      required this.icon,
      required this.label,
      required this.value,
      this.iconColor = AppColors.green,
      this.iconBg = AppColors.greenSoft});
  final IconData icon;
  final String label, value;
  final Color iconColor, iconBg;

  @override
  Widget build(BuildContext context) => Row(children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
              color: iconBg, borderRadius: BorderRadius.circular(10)),
          child: Icon(icon, color: iconColor, size: 20),
        ),
        const SizedBox(width: 10),
        Expanded(
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Text(value,
                    style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 18,
                        color: AppColors.ink))),
            Text(label,
                style: const TextStyle(color: AppColors.muted, fontSize: 12)),
          ]),
        ),
      ]);
}

/// Label on the left, value on the right; used in detail cards and receipts.
class InfoRow extends StatelessWidget {
  const InfoRow(this.label, this.value,
      {super.key, this.bold = false, this.valueWidget});
  final String label;
  final String value;
  final bool bold;
  final Widget? valueWidget;

  @override
  Widget build(BuildContext context) {
    final style = TextStyle(
        fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
        color: AppColors.ink,
        fontSize: bold ? 16 : 14);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Expanded(
            child: Text(label,
                style: TextStyle(
                    color: bold ? AppColors.ink : AppColors.muted,
                    fontWeight: bold ? FontWeight.w800 : FontWeight.w400))),
        const SizedBox(width: 12),
        Flexible(
            child: valueWidget ??
                Text(value, style: style, textAlign: TextAlign.right)),
      ]),
    );
  }
}

/// Navigation row with icon, title, subtitle and chevron (design: profile and operation menus).
class MenuTile extends StatelessWidget {
  const MenuTile(
      {super.key,
      required this.icon,
      required this.title,
      this.subtitle,
      this.onTap,
      this.trailing,
      this.danger = false});
  final IconData icon;
  final String title;
  final String? subtitle;
  final VoidCallback? onTap;
  final Widget? trailing;
  final bool danger;

  @override
  Widget build(BuildContext context) {
    final color = danger ? AppColors.red : AppColors.navy;
    return ListTile(
      onTap: onTap,
      contentPadding: const EdgeInsets.symmetric(horizontal: 4),
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
            color: danger ? AppColors.redSoft : AppColors.background,
            borderRadius: BorderRadius.circular(10)),
        child: Icon(icon, color: color, size: 20),
      ),
      title: Text(title,
          style: TextStyle(
              fontWeight: FontWeight.w700,
              color: danger ? AppColors.red : AppColors.ink)),
      subtitle: subtitle == null
          ? null
          : Text(subtitle!,
              style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
      trailing: trailing ??
          (onTap == null
              ? null
              : const Icon(Icons.chevron_right, color: AppColors.muted)),
    );
  }
}

/// Gold-tinted summary banner (design: total amount on payment screens).
class HighlightBox extends StatelessWidget {
  const HighlightBox({super.key, required this.label, required this.value});
  final String label, value;

  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 16),
        decoration: BoxDecoration(
            color: AppColors.goldSoft, borderRadius: BorderRadius.circular(12)),
        child: Column(children: [
          Text(label,
              style: const TextStyle(
                  color: AppColors.ink, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(value,
              style: const TextStyle(
                  color: AppColors.ink,
                  fontSize: 28,
                  fontWeight: FontWeight.w800)),
        ]),
      );
}
