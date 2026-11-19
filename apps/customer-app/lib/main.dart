import 'package:flutter/widgets.dart';
import 'package:lutiano_core/lutiano_core.dart';

import 'app.dart';
import 'state/app_settings.dart';
import 'state/app_state.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await startCrashReporting();
  final state = AppState(
      settings: await AppSettings.load(), tokenStore: const TokenStore());
  await state.restoreSession();
  runApp(CustomerApp(state: state));
}
