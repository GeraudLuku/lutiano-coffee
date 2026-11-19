import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';
import '../l10n/core_l10n.dart';

class EmptyState extends StatelessWidget {
  const EmptyState(
      {super.key,
      required this.icon,
      required this.title,
      this.message,
      this.action});
  final IconData icon;
  final String title;
  final String? message;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 36, horizontal: 16),
        child: Column(children: [
          Icon(icon, size: 44, color: AppColors.muted),
          const SizedBox(height: 10),
          Text(title,
              textAlign: TextAlign.center,
              style:
                  const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
          if (message != null) ...[
            const SizedBox(height: 4),
            Text(message!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.muted))
          ],
          if (action != null) ...[const SizedBox(height: 14), action!],
        ]),
      );
}

class ErrorBanner extends StatelessWidget {
  const ErrorBanner(this.message, {super.key, this.onRetry});
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) => Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.fromLTRB(14, 10, 6, 10),
        decoration: BoxDecoration(
            color: AppColors.redSoft, borderRadius: BorderRadius.circular(12)),
        child: Row(children: [
          const Icon(Icons.error_outline, color: AppColors.red),
          const SizedBox(width: 10),
          Expanded(
              child:
                  Text(message, style: const TextStyle(color: AppColors.red))),
          if (onRetry != null)
            TextButton(onPressed: onRetry, child: Text(context.coreL10n.retry)),
        ]),
      );
}

class InfoNote extends StatelessWidget {
  const InfoNote(this.message,
      {super.key,
      this.icon = Icons.info_outline,
      this.color = AppColors.blue,
      this.background = AppColors.blueSoft});
  final String message;
  final IconData icon;
  final Color color, background;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
            color: background, borderRadius: BorderRadius.circular(12)),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(width: 10),
          Expanded(
              child:
                  Text(message, style: TextStyle(color: color, height: 1.35))),
        ]),
      );
}

void showMessage(BuildContext context, String message, {bool error = false}) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(
        content: Text(message),
        backgroundColor: error ? AppColors.red : AppColors.navy));
}
