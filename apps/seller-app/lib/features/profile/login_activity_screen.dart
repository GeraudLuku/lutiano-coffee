import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';

String _activityLabel(BuildContext context, String action) {
  final l = context.l10n;
  return switch (action) {
    'SIGNED_IN' => l.activitySignedIn,
    'PASSWORD_CHANGED' => l.activityPasswordChanged,
    'PHONE_CHANGED' => l.phoneChanged,
    'SESSIONS_REVOKED' => l.activitySessionsRevoked,
    'PROFILE_UPDATED' => l.profileSaved,
    _ => action,
  };
}

/// Batch 8 · Screen 5 — Login activity: recent sign-ins and account-security changes, newest first.
class LoginActivityScreen extends StatelessWidget {
  const LoginActivityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.loginActivity)),
      body: PagedListView<AccountActivity>(
        key: ValueKey('activity-${app.dataVersion}'),
        fetch: (page, size) => app.account.activityPage(page: page, size: size),
        skeleton: const SkeletonTile(),
        empty: EmptyState(icon: Icons.history, title: l.noActivity),
        itemBuilder: (context, a, _, __) => Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: AppCard(
            child: Row(children: [
              Icon(
                  a.action == 'SIGNED_IN' ? Icons.login : Icons.shield_outlined,
                  color: AppColors.navy),
              const SizedBox(width: 12),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(_activityLabel(context, a.action),
                        style: const TextStyle(fontWeight: FontWeight.w700)),
                    if (a.client != null && a.action == 'SIGNED_IN')
                      Text(a.client!,
                          style: const TextStyle(
                              color: AppColors.muted, fontSize: 12.5)),
                  ])),
              Text(dateTime(a.at),
                  style: const TextStyle(color: AppColors.muted, fontSize: 12)),
            ]),
          ),
        ),
      ),
    );
  }
}
