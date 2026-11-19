import 'package:flutter/widgets.dart';

import 'app_state.dart';

/// Gives every screen the [AppState]. `AppScope.of(context)` rebuilds the screen when it changes, `AppScope.read(context)` does not.
class AppScope extends InheritedNotifier<AppState> {
  const AppScope({super.key, required AppState state, required super.child})
      : super(notifier: state);

  static AppState of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AppScope>()!.notifier!;

  static AppState read(BuildContext context) =>
      (context.getInheritedWidgetOfExactType<AppScope>()!).notifier!;
}
